package org.powertac.tourney.services;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class RunBoot
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String logSuffix = "boot-";
  private Game game;
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Session session;

  private static boolean machinesAvailable = true;

  public RunBoot (Session session, Game game)
  {
    this.game = game;
    this.session = session;

    run();
  }

  private void run ()
  {
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
  }

  /***
   * Make sure there is a machine available for the game
   */
  @SuppressWarnings("unchecked")
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

  private boolean startJob () throws Exception
  {
    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-boot-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
            + "&suffix=" + logSuffix
            + "&pomId=" + game.getTournament().getPomId()
            + "&machine=" + game.getMachine().getMachineName()
            + "&gameId=" + game.getGameId();



    log.info("Final url: " + finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();

      String user = properties.getProperty("jenkins.username", "");
      String pass = properties.getProperty("jenkins.password", "");
      if (!user.isEmpty() && !pass.isEmpty()) {
        String userpass = String.format("%s:%s", user, pass);
        String basicAuth = "Basic " +
            new String(new Base64().encode(userpass.getBytes()));
        conn.setRequestProperty("Authorization", basicAuth);
      }

      conn.getInputStream();
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

    log.info(String.format("WatchDogTimer reports %s boots are ready to "
        + "start", games.size()));
    for (Game game: games) {
      log.info(String.format("Boot %s will be started ...", game.getGameId()));
      new RunBoot(session, game);

      if (!machinesAvailable) {
        log.info("WatchDog No free machines, stop looking for Bootable Games");
        break;
      }
    }

    session.close();
  }
}
