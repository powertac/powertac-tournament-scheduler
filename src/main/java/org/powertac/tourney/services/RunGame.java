package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;

import java.util.ArrayList;
import java.util.List;


public class RunGame
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Game game;
  private String brokers = "";
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Session session;

  private static boolean machinesAvailable;

  public RunGame (Game game)
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
    finally {
      session.close();
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
   * Also check if participating brokers have an agent available (we don't check
   * if agents are checking in, brokers are responsible for availability).
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
      if (! MemStore.getBrokerState(agent.getBroker().getBrokerId())) {
        log.info(String.format("Not starting game %s : broker %s is disabled",
            game.getGameId(), agent.getBroker().getBrokerId()));
        return false;
      }

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

  /*
   * If all conditions are met (we have a slave available, game is booted and
   * agents should be avalable) send job to Jenkins.
   */
  private boolean startJob () throws Exception
  {
    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-sim-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
            + "&pomId=" + game.getTournament().getPomId()
            + "&gameId=" + game.getGameId()
            + "&machine=" + game.getMachine().getMachineName()
            + "&brokers=" + brokers
            + "&serverQueue=" + game.getServerQueue();

    log.info("Final url: " + finalUrl);

    try {
      JenkinsConnector.sendJob(finalUrl);

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

  /*
   * Look for runnable games. This means games that are 'game_pending'.
   * If a tournament is loaded (runningTournament != null) we only look for
   * games in that tournament. If no tournament loaded, we look for games in
   * all singleGame tournaments.
  **/
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
    session.close();

    log.info(String.format("WatchDogTimer reports %s game(s) are ready to "
        + "start", games.size()));

    machinesAvailable = true;
    for (Game game: games) {
      log.info(String.format("Game %s will be started ...", game.getGameId()));
      new RunGame(game);

      if (!machinesAvailable) {
        log.info("WatchDog No free machines, stop looking for Startable Games");
        break;
      }
    }
  }
}
