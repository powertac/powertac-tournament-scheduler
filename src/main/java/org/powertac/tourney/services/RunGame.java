package org.powertac.tourney.services;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class RunGame
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String logSuffix = "sim-";
  private Game game;
  private String brokers = "";
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Session session;

  private static boolean machinesAvailable = true;

  public RunGame (Session session, Game game)
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

      if (!checkBootstrap()) {
        transaction.rollback();
        return;
      }

      if (!checkBrokers()) {
        transaction.rollback();
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
      log.info("Failed to start simulation game: " + game.getGameId());
    }
  }

  /***
   * Make sure a bootstrap has been run for the sim
   */
  private boolean checkBootstrap ()
  {
    if (game.hasBootstrap()) {
      return true;
    }
    else {
      log.info("Game: " + game.getGameId() + " reports that boot is not ready!");
      game.setStatus(Game.STATE.boot_pending.toString());
      return false;
    }
  }

  /***
   * Make sure brokers are registered for the tournament
   * Also check if participating brokers have an agent available
   */
  private boolean checkBrokers ()
  {
    if (game.getAgentMap().size() < 1) {
      log.info(String.format("Game: %s (tournament %s) reports no brokers "
          + "registered",
          game.getGameId(), game.getTournament().getTournamentId()));
      return false;
    }

    for (Agent agent: game.getAgentMap().values()) {
      if (!agent.getBroker().agentsAvailable()) {
        log.info(String.format("Not starting game %s : broker %s doesn't have "
            + "enough available agents",
            game.getGameId(), agent.getBroker().getBrokerId()));
        return false;
      }

      brokers += agent.getBroker().getBrokerName() + "/";
      brokers += agent.getBrokerQueue() +",";
    }
    brokers = brokers.substring(0, brokers.length()-1);
    return true;
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
        log.info(String.format("No machines available to run scheduled sim %s"
            + "... will retry in %s seconds",
            game.getGameId(), scheduler.getWatchDogInterval()/1000));
        return false;
      }

      game.setMachine(freeMachine);
      freeMachine.setStatus(Machine.STATE.running.toString());
      session.update(freeMachine);
      log.info(String.format("Game: %s running on machine: %s",
          game.getGameId(), game.getMachine().getMachineName()));
      return true;
    }
    catch (Exception e) {
      log.warn("Error claiming free machine for game " + game.getGameId());
      throw e;
    }
  }

  private boolean startJob () throws Exception
  {
    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-sim-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
            + "&suffix=" + logSuffix
            + "&pomId=" + game.getTournament().getPomId()
            + "&machine=" + game.getMachine().getMachineName()
            + "&gameId=" + game.getGameId()
            + "&brokers=" + brokers
            + "&serverQueue=" + game.getServerQueue();

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
      log.info("Jenkins request to start sim game: " + game.getGameId());
      game.setStatus(Game.STATE.game_pending.toString());
      game.setReadyTime(Utils.offsetDate());
      log.debug(String.format("Update game: %s to %s", game.getGameId(),
          Game.STATE.game_pending.toString()));

      return true;
    }
    catch (Exception e) {
      log.error("Jenkins failure to start simulation game: " + game.getGameId());
      game.setStatus(Game.STATE.game_failed.toString());
      throw e;
    }
  }

  public static void startRunnableGames(Tournament runningTournament)
  {
    log.info("WatchDogTimer Looking for Runnable Games");

    List<Game> games = new ArrayList<Game>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (runningTournament == null) {
        games = Game.getStartableSingleGames(session);
        log.info("WatchDog CheckForSims for SINGLE_GAME tournament games");
      }
      else {
        games = Game.getStartableMultiGames(session, runningTournament);
        log.info("WatchDog CheckForSims for MULTI_GAME tournament games");
      }
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }

    log.info(String.format("WatchDogTimer reports %s game(s) are ready to "
        + "start", games.size()));
    for (Game game: games) {
      log.info(String.format("Game %s will be started ...", game.getGameId()));
      new RunGame(session, game);

      if (!machinesAvailable) {
        log.info("WatchDog No free machines, stop looking for Startable Games");
        break;
      }
    }

    session.close();
  }
}
