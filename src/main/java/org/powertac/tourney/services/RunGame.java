package org.powertac.tourney.services;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.List;

import static org.powertac.tourney.services.Utils.log;


public class RunGame implements Runnable
{
  private Machine machine = null;

  private boolean running = false;
  private boolean tourney = false;

  private Game game;
  private String logSuffix = "sim";
  private String pomId = "";
  private String gameId = "";
  private String brokers = "";
  private String machineName = "";

  private TournamentProperties properties = new TournamentProperties();

  public RunGame (Game game, int pomId)
  {
    this.game = game;
    this.gameId = String.valueOf(game.getGameId());
    this.pomId = String.valueOf(pomId);
    running = false;
  }

  public RunGame (Game game, int pomId, Machine machine, String brokers)
  {
    this.game = game;
    this.gameId = String.valueOf(game.getGameId());
    this.pomId = String.valueOf(pomId);
    this.brokers = brokers;
    this.machine = machine;

    running = false;
    tourney = true;
  }

  /***
   * Make sure a bootstrap has been run for the sim
   */
  private boolean checkBootstrap ()
  {
    if (!tourney) {
      if (!running) {
        Database db = new Database();

        try {
          db.startTrans();
          if (db.isGameReady(Integer.parseInt(gameId))) {
            db.commitTrans();
            return true;
          }
          else {
            log("Game: {0} reports that bootstrap is not ready!", gameId);
          }
        }
        catch (NumberFormatException e) {
          e.printStackTrace();
        }
        catch (SQLException e) {
          log("Bootstrap Database error while scheduling sim!!");
          e.printStackTrace();
        }

				db.abortTrans();
        return false;
      }
    }
    return true;
  }

  /***
   * Make sure brokers are registered for the tournament
   */
  private boolean checkBrokers ()
  {
    // We already got the brokers
    if (!brokers.isEmpty()) {
      return true;
    }

    // TODO Why is this??
    if ((!tourney) && (running)) {
      return true;
    }

    int gId = Integer.parseInt(gameId);
    Database db = new Database();

    try {
      db.startTrans();
      Game g = db.getGame(gId);

      int numRegistered = db.getNumberBrokersRegistered(g.getTourneyId());
      if (numRegistered < 1) {
        db.commitTrans();
        log("Game: {0} reports no brokers registered, waiting to start, "
            + "tourneyId: {1}", gameId, g.getTourneyId());
        return false;
      }
      else {
        log("There are {0} brokers registered for tournament... starting sim",
            numRegistered);

        List<Broker> brokerList = db.getBrokersInGame(gId);
        for (Broker b: brokerList) {
          brokers += b.getBrokerName() + "/" + b.getQueueName() +",";
        }
        if (brokerList.size() < 1) {
          db.commitTrans();
          log("Game: {0} reports no brokers listed in database, ", gId);
          return false;
        }

//        brokers = "";
//        for (Broker b: brokerList) {
//          brokers += b.getBrokerName() + ",";
//        }
        brokers = brokers.substring(0, brokers.length()-1);
      }

      db.commitTrans();
      return true;
    }
    catch (SQLException e) {
      db.abortTrans();
      log("Broker Database error while scheduling sim!!");
      e.printStackTrace();
      return false;
    }
  }

  /***
   * Make sure there is a machine available for the game
   */
  private boolean checkMachineAvailable ()
  {
    int gId = Integer.parseInt(gameId);
    Database db = new Database();

    if ((!tourney) && (running)) {
      return false;
    }

    try {
      db.startTrans();

      if (machine == null) {
        if (machineName.isEmpty()) {
          machine = db.claimFreeMachine();
        }
        else {
          machine = db.claimFreeMachine(machineName);
        }
        if (machine == null) {
          db.abortTrans();
          return false;
        } else {
          machineName = machine.getName();
        }
      }

      String jmsUrl = "tcp://" + machine.getUrl() + ":61616";
      // JEC - most of this should be done through a Game instance
      db.updateGameJmsUrlById(gId, jmsUrl);
      db.updateProperties(gId, jmsUrl);
      db.updateGameMachine(gId, machine.getMachineId());
      db.updateGameViz(gId, machine.getVizUrl());
      db.setMachineStatus(machine.getMachineId(), Machine.STATE.running);
      db.commitTrans();
      log("Game: {0} running on machine: {1}", gameId, machineName);

      return true;
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      return false;
    }
  }

  private void setGameState(Game.STATE state) {
    Database db = new Database();
    try {
      int gId = Integer.parseInt(gameId);
      db.startTrans();
      db.updateGameStatusById(gId, state);
      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  @Override
  public synchronized void run ()
  {
    if (running) {
      // Should not get here
      log("[ERROR] Game {0} is already running!", gameId);
    }

    // Check if a boot exists
    if (!checkBootstrap()) {
      return;
    }

    // Check if brokers are registered
    if (!checkBrokers()) {
      return;
    }
    // Check if there is a machine available to run the sim and set it
    if (!checkMachineAvailable()) {
      log("[INFO] No machines available to run scheduled game: {0}... will retry"
          + " in {1} seconds", gameId, Integer.parseInt(
          properties.getProperty("scheduler.watchDogInterval")) / 1000);
      return;
    }

    String finalUrl =
        "http://localhost:8080/jenkins/job/"
        + "start-server-instance/buildWithParameters?"
        + "token=start-instance"
        + "&tourneyUrl=" + Utils.getTourneyUrl()
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machineName
        + "&gameId=" + gameId
        + "&brokers=" + brokers
        + "&serverQueue=" + game.getServerQueue();

    log("[INFO] Final url: {0}", finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
      log("Jenkins request to start sim game: {0}", gameId);
      running = true;
      setGameState(Game.STATE.game_pending);
    }
    catch (Exception e) {
      e.printStackTrace();
      log("Jenkins failure to start simulation game: {0}", gameId);
      setGameState(Game.STATE.game_failed);
    }
  }
}
