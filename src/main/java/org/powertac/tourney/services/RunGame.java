package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.List;


public class RunGame implements Runnable
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Machine machine = null;

  private boolean running = false;
  private boolean tourney = false;
  private int watchDogInterval;

  private String logSuffix = "sim-";
  private int pomId;
  private int gameId;
  private String brokers = "";
  private String machineName = "";
  private Database db;

  private TournamentProperties properties = TournamentProperties.getProperties();

  public RunGame (int gameId, int pomId)
  {
    this(gameId, pomId, null, "");
    tourney = false;
  }

  public RunGame (int gameId, int pomId, Machine machine, String brokers)
  {
    this.gameId = gameId;
    this.pomId = pomId;
    this.brokers = brokers;
    this.machine = machine;
    db = new Database();

    tourney = true;

    watchDogInterval = Integer.parseInt(
        properties.getProperty("scheduler.watchDogInterval")) / 1000;
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
            log.info("Game: " + gameId + " reports that boot is not ready!");
          }
        }
        catch (NumberFormatException e) {
          e.printStackTrace();
        }
        catch (SQLException e) {
          log.info("Bootstrap Database error while scheduling sim!!");
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
        log.info(String.format("Game: %s reports no brokers registered, waiting"
            + " to start, tourneyId: %s", gameId, g.getTourneyId()));
        return false;
      }
      else {
        log.info(String.format("There are %s brokers registered for tournament"
            + "... starting sim", numRegistered));

        List<Broker> brokerList = db.getBrokersInGame(gameId);

        if (brokerList.size() < 1) {
          db.commitTrans();
          log.info("Game: " + gameId+ " reports no brokers listed in database");
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
      log.info("Broker Database error while scheduling sim!!");
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
      log.info(String.format("Game: %s running on machine: %s",
          gameId, machineName));

      return true;
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public synchronized void run ()
  {
    if (running) {
      // Should not get here
      log.error("Game " + gameId + " is already running!");
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
      log.info(String.format("No machines available to run scheduled game: %s"
          + "... will retry in %s seconds", gameId, watchDogInterval));
      return;
    }

    // TODO Refactor with Hibernate
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
        properties.getProperty("jenkinsLocation")
        + "job/start-server-instance/buildWithParameters?"
        + "token=start-instance"
        + "&tourneyUrl=" + properties.getProperty("tourneyUrl")
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machineName
        + "&gameId=" + gameId
        + "&brokers=" + brokers
        + "&serverQueue=" + game.getServerQueue();

    log.info("Final url: " + finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
      log.info("Jenkins request to start sim game: " + gameId);
      running = true;
      game.setState(Game.STATE.game_pending);
    }
    catch (Exception e) {
      e.printStackTrace();
      log.error("Jenkins failure to start simulation game: " + gameId);
      game.setState(Game.STATE.game_failed);
    }
  }
}
