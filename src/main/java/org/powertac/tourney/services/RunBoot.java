package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;

import java.util.ArrayList;
import java.util.List;


/*
 *
 */
public class RunBoot
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Game game;
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Session session;

  private static boolean machinesAvailable;

  public RunBoot (Game game)
  {
    this.game = game;

    run();
  }

  private void run ()
  {
    session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (!checkMachineAvailable()) {
        transaction.rollback();
        machinesAvailable = false;
        return;
      }

      if (!startJob()) {
        transaction.rollback();
        return;
      }

      session.update(game);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.info("Failed to bootstrap game: " + game.getGameId());
    }
    finally {
      session.close();
    }
  }

  /***
   * Make sure there is a machine available for the game
   */
  private boolean checkMachineAvailable ()
      throws Exception
  {
    try {
      log.info("Claiming free machine");

      Machine freeMachine = Machine.getFreeMachine(session);
      if (freeMachine == null) {
        Scheduler scheduler = Scheduler.getScheduler();
        log.info(String.format("No machines available to run scheduled boot %s"
            + "... will retry in %s seconds",
            game.getGameId(), scheduler.getWatchDogInterval()/1000));
        return false;
      }

      game.setMachine(freeMachine);
      freeMachine.setStatus(Machine.STATE.running.toString());
      session.update(freeMachine);
      log.info(String.format("Game: %s booting on machine: %s",
          game.getGameId(), game.getMachine().getMachineName()));
      return true;
    }
    catch (Exception e) {
      log.warn("Error claiming free machine for boot " + game.getGameId());
      throw e;
    }
  }

  /*
   * If all conditions are met (we have a slave available) send job to Jenkins.
   */
  private boolean startJob () throws Exception
  {
    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-boot-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
            + "&pomId=" + game.getTournament().getPomId()
            + "&gameId=" + game.getGameId()
            + "&machine=" + game.getMachine().getMachineName();

    log.info("Final url: " + finalUrl);

    try {
      JenkinsConnector.sendJob(finalUrl);

      log.info("Jenkins request to bootstrap game: " + game.getGameId());
      game.setStatus(Game.STATE.boot_in_progress.toString());
      game.setReadyTime(Utils.offsetDate());
      log.debug(String.format("Update game: %s to %s", game.getGameId(),
          Game.STATE.boot_in_progress.toString()));

      return true;
    }
    catch (Exception e) {
      log.error("Jenkins failure to bootstrap game: " + game.getGameId());
      game.setStatus(Game.STATE.boot_failed.toString());
      throw e;
    }
  }

  /*
   * Look for bootable games. This means games that are 'boot_pending'.
   * If a tournament is loaded (runningTournament != null) we only look for
   * games in that tournament. If no tournament loaded, we look for games in
   * all singleGame tournaments.
  **/
  public static void startBootableGames (Tournament runningTournament)
  {
    log.info("WatchDogTimer Looking for Bootstraps To Start..");

    List<Game> games = new ArrayList<Game>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (runningTournament == null) {
        games = Game.getBootableSingleGames(session);
        log.info("WatchDog CheckForBoots for SINGLE_GAME tournament boots");
      }
      else {
        games = Game.getBootableMultiGames(session, runningTournament);
        log.info("WatchDog CheckForBoots for MULTI_GAME tournament boots");
      }
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    log.info(String.format("WatchDogTimer reports %s boots are ready to "
        + "start", games.size()));
    for (Game game: games) {
      machinesAvailable = true;

      log.info(String.format("Boot %s will be started ...", game.getGameId()));
      new RunBoot(game);

      if (!machinesAvailable) {
        log.info("WatchDog No free machines, stop looking for Bootable Games");
        break;
      }
    }
  }
}
