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

  private String logSuffix = "sim";
  private String pomId = "";
  private int gameId;
  private String brokers = "";
  private String machineName = "";
  private Database db;

  private TournamentProperties properties = new TournamentProperties();

  public RunGame (int gameId, int pomId)
  {
    this.gameId = gameId;
    this.pomId = String.valueOf(pomId);
    running = false;
    db = new Database();
  }

  public RunGame (int gameId, int pomId, Machine machine, String brokers)
  {
    this.gameId = gameId;
    this.pomId = String.valueOf(pomId);
    this.brokers = brokers;
    this.machine = machine;
    db = new Database();

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

        try {
          db.startTrans();
          if (db.isGameReady(gameId)) {
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

    try {
      db.startTrans();
      Game g = db.getGame(gameId);

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

        List<Broker> brokerList = db.getBrokersInGame(gameId);

        if (brokerList.size() < 1) {
          db.commitTrans();
          log("Game: {0} reports no brokers listed in database, ", gameId);
          return false;
        }
        for (Broker b: brokerList) {
          brokers += b.getBrokerName() + "/" + b.getQueueName() +",";
        }
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
      db.updateGameJmsUrlById(gameId, jmsUrl);
      db.updateProperties(gameId, jmsUrl);
      db.updateGameMachine(gameId, machine.getMachineId());
      db.updateGameViz(gameId, machine.getVizUrl());
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
    try {
      db.startTrans();
      db.updateGameStatusById(gameId, state);
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
    
    Game game;
    try {
      db.startTrans();
      game = db.getGame(gameId);
      db.commitTrans();
    }
    catch (SQLException e1) {
      db.abortTrans();
      e1.printStackTrace();
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
