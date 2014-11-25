package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.*;

import java.util.ArrayList;
import java.util.List;


public class RunBoot
{
  private static Logger log = Utils.getLogger();

  private TournamentProperties properties = TournamentProperties.getProperties();

  private Game game;
  private Session session;

  private static boolean machinesAvailable;

  public RunBoot (Game game)
  {
    this.game = game;
  }

  private void run ()
  {
    session = HibernateUtil.getSession();
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
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.info("Failed to bootstrap game: " + game.getGameId());
    } finally {
      session.close();
    }
  }

  /**
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
        log.info(String.format(
            "No machine available for scheduled boot %s, retry in %s seconds",
            game.getGameId(), scheduler.getSchedulerInterval() / 1000));
        return false;
      }

      game.setMachine(freeMachine);
      freeMachine.setStateRunning();
      session.update(freeMachine);
      log.info(String.format("Game: %s booting on machine: %s",
          game.getGameId(), game.getMachine().getMachineName()));
      return true;
    } catch (Exception e) {
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
            + "&pomId=" + game.getRound().getPomId()
            + "&gameId=" + game.getGameId()
            + "&machine=" + game.getMachine().getMachineName();

    log.info("Final url: " + finalUrl);

    try {
      JenkinsConnector.sendJob(finalUrl, false);

      log.info("Jenkins request to bootstrap game: " + game.getGameId());
      game.setStateBootInProgress();
      game.setReadyTime(Utils.offsetDate());
      log.debug(String.format("Update game: %s to %s", game.getGameId(),
          Game.getStateBootInProgress()));

      return true;
    } catch (Exception e) {
      log.error("Jenkins failure to bootstrap game: " + game.getGameId());
      game.setStateBootFailed();
      throw e;
    }
  }

  /*
   * Look for bootable games. This means games that are 'boot_pending'.
   * If a round is loaded (runningRound != null) we only look for
   * games in that round. If no round loaded, we look for games in
   * all singleGame rounds.
  **/
  public static void startBootableGames (List <Round> runningRounds)
  {
    if (runningRounds == null || runningRounds.size() == 0) {
      log.info("No rounds available for bootable games");
      return;
    }

    log.info("Looking for Bootstraps To Start..");

    List<Game> games = new ArrayList<Game>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      games = GamesScheduler.getBootableGames(session, runningRounds);
      log.info("CheckForBoots for bootable games");
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    log.info(String.format("Found %s boots ready to start", games.size()));

    machinesAvailable = true;
    for (Game game: games) {
      log.info(String.format("Boot %s will be started ...", game.getGameId()));
      new RunBoot(game).run();

      if (!machinesAvailable) {
        log.info("No free machines, stop looking for Bootable Games");
        break;
      }
    }
  }
}
