package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Machine;

import java.util.List;


public class RunGame
{
  private static Logger log = Utils.getLogger();

  private Game game;
  private List<Machine> freeMachines;
  private String brokers = "";
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Session session;

  public RunGame (Game game, List<Machine> freeMachines)
  {
    this.game = game;
    this.freeMachines = freeMachines;
  }

  private void run ()
  {
    session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (!checkBootstrap()) {
        transaction.rollback();
        return;
      }

      if (!checkBrokers()) {
        transaction.rollback();
        return;
      }

      setMachineToGame();
      startJob();
      session.update(game);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.info("Failed to start sim game: " + game.getGameId());
    }
    finally {
      session.close();
    }
  }

  /**
   * Make sure a bootstrap has been run for the sim
   */
  private boolean checkBootstrap ()
  {
    if (game.hasBootstrap()) {
      return true;
    }
    else {
      log.info("Game: " + game.getGameId() + " reports that boot is not ready!");
      game.setStateBootPending();
      return false;
    }
  }

  /**
   * Make sure brokers are registered for the round
   * Also check if participating brokers have an agent available (we don't check
   * for agents checking in, brokers are responsible for availability).
   */
  private boolean checkBrokers ()
  {
    try {
      session.refresh(game);

      if (game.getAgentMap().size() < 1) {
        log.info(String.format("Game: %s (round %s) reports no brokers "
                + "registered",
            game.getGameId(), game.getRound().getRoundId()));
        return false;
      }

      for (Agent agent : game.getAgentMap().values()) {
        Broker broker = agent.getBroker();
        // Check if any broker is disabled in the interface
        if (!MemStore.getBrokerState(broker.getBrokerId())) {
          log.info(String.format("Not starting game %s : broker %s is disabled",
              game.getGameId(), broker.getBrokerId()));
          return false;
        }

        // Check if any broker is already running the maxAgent nof agents
        if (!broker.hasAgentsAvailable(game.getRound())) {
          log.info(String.format("Not starting game %s : broker %s doesn't have "
                  + "enough available agents",
              game.getGameId(), broker.getBrokerId()));
          return false;
        }

        brokers += broker.getBrokerName() + "/" + agent.getBrokerQueue() + ",";
      }
      brokers = brokers.substring(0, brokers.length() - 1);
      return true;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Link machine to the game
   */
  private void setMachineToGame ()
  {
    log.info("Claiming free machine");

    Machine freeMachine = freeMachines.remove(0);
    game.setMachine(freeMachine);
    freeMachine.setStateRunning();
    session.update(freeMachine);
    log.info(String.format("Game: %s running on machine: %s",
        game.getGameId(), game.getMachine().getMachineName()));
  }

  /*
   * If all conditions are met (we have a slave available, game is booted and
   * agents should be available) send job to Jenkins.
   */
  private void startJob () throws Exception
  {
    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-sim-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
            + "&pomId=" + game.getRound().getPomId()
            + "&gameId=" + game.getGameId()
            + "&machine=" + game.getMachine().getMachineName()
            + "&brokers=" + brokers
            + "&serverQueue=" + game.getServerQueue();

    log.info("Final url: " + finalUrl);

    try {
      JenkinsConnector.sendJob(finalUrl);

      log.info("Jenkins request to start sim game: " + game.getGameId());
      game.setStateGamePending();
      game.setReadyTime(Utils.offsetDate());
      log.debug(String.format("Update game: %s to %s", game.getGameId(),
          Game.getStateGamePending()));
    }
    catch (Exception e) {
      log.error("Jenkins failure to start sim game: " + game.getGameId());
      game.setStateGameFailed();
      throw e;
    }
  }

  /*
   * Look for runnable games. This means games that are 'game_pending'.
   * If a round is loaded (runningRound != null) we only look for
   * games in that round. If no round loaded, we look for games in
   * all singleGame rounds.
   */
  public static void startRunnableGames (List<Integer> runningRoundIds,
                                         List<Game> notCompleteGames,
                                         List<Machine> freeMachines)
  {
    if (runningRoundIds == null || runningRoundIds.size() == 0) {
      log.info("No rounds available for runnable games");
      return;
    }

    log.info("Looking for Runnable Games");

    List<Game> games =
        GamesScheduler.getStartableGames(runningRoundIds, notCompleteGames);

    log.info(String.format("Found %s game(s) ready to start", games.size()));

    for (Game game : games) {
      if (freeMachines.size() == 0) {
        log.info("No free machines, stop looking for Startable Games");
        return;
      }

      log.info(String.format("Game %s will be started ...", game.getGameId()));
      new RunGame(game, freeMachines).run();
    }
  }
}
