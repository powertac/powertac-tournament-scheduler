package org.powertac.tourney.services;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.List;


public class RunGame
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String logSuffix = "sim-";
  private Game game;
  private int gameId;
  private String brokers = "";
  private String machineName = "";
  private int watchDogInterval;
  private TournamentProperties properties = TournamentProperties.getProperties();

  public RunGame (Game game)
  {
    this.game = game;
    gameId = game.getGameId();

    watchDogInterval = Integer.parseInt(
        properties.getProperty("scheduler.watchDogInterval")) / 1000;

    run();
  }

  private void run ()
  {
    Database db = new Database();
    try {
      db.startTrans();

      if (!checkBootstrap(db)) {
        db.abortTrans();
        return;
      }

      if (!checkBrokers(db)) {
        db.abortTrans();
        return;
      }

      if (!checkMachineAvailable(db)) {
        db.abortTrans();
        return;
      }

      if (!startJob(db)) {
        db.abortTrans();
        return;
      }

      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      log.error("Failed to start simulation game: " + gameId);
    }
  }

  /***
   * Make sure a bootstrap has been run for the sim
   */
  private boolean checkBootstrap (Database db) throws SQLException
  {
    try {
      if (game.hasBootstrap()) {
        return true;
      }
      else {
        log.info("Game: " + gameId + " reports that boot is not ready!");
        db.updateGameStatusById(gameId, Game.STATE.boot_pending);
        return false;
      }
    }
    catch (SQLException sqle) {
      log.warn("Bootstrap DB error while scheduling game " + gameId);
      throw sqle;
    }
  }

  /***
   * Make sure brokers are registered for the tournament
   */
  private boolean checkBrokers (Database db) throws SQLException
  {
    try {
      List<Broker> brokerList = db.getBrokersInGame(gameId);
      if (brokerList.size() < 1) {
        log.info(String.format("Game: %s (tournament %s) reports no brokers "
            + "registered", gameId, game.getTourneyId()));
        return false;
      }
      else {
        for (Broker broker: brokerList) {
          if (!broker.agentsAvailable(db)) {
            log.info(String.format("Not starting game %s : broker %s doesn't "
                + "have enough available agents", gameId, broker.getBrokerId()));
            return false;
          }

          brokers += broker.getBrokerName() + "/";
          brokers += db.getBrokerQueueName(gameId, broker.getBrokerId()) +",";
        }
        brokers = brokers.substring(0, brokers.length()-1);

        log.info(String.format("There are %s brokers registered for tournament"
            + "... starting sim", brokerList.size()));
        log.info("Broker Ids : " + brokers);

        return true;
      }
    }
    catch (SQLException sqle) {
      log.warn("Broker DB error while scheduling game " + gameId);
      throw sqle;
    }
  }

  /***
   * Make sure there is a machine available for the game
   */
  private boolean checkMachineAvailable (Database db) throws SQLException
  {
    try {
      Machine machine = db.claimFreeMachine();
      if (machine == null) {
        log.info(String.format("No machines available to run scheduled game: %s"
            + "... will retry in %s seconds", gameId, watchDogInterval));
        return false;
      }

      String jmsUrl = "tcp://" + machine.getUrl() + ":61616";
      // JEC - most of this should be done through a Game instance
      db.updateGameJmsUrlById(gameId, jmsUrl);
      db.updateProperties(gameId, jmsUrl);
      db.updateGameMachine(gameId, machine.getMachineId());
      db.updateGameViz(gameId, machine.getVizUrl());
      db.setMachineStatus(machine.getMachineId(), Machine.STATE.running);
      machineName = machine.getName();
      log.info(String.format("Game: %s running on machine: %s",
          gameId, machine.getName()));
      return true;
    }
    catch (SQLException sqle) {
      log.warn("Error claiming free machines for game " + gameId);
      throw sqle;
    }
  }

  private boolean startJob (Database db) throws Exception
  {
    // We have to reload the game, as it is changed by checkMachineAvailable
    game = db.getGameById(gameId);
    int pomId = db.getTournamentByGameId(gameId).getPomId();

    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-sim-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
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

      String user = properties.getProperty("jenkins.username", "");
      String pass = properties.getProperty("jenkins.password", "");
      if (!user.isEmpty() && !pass.isEmpty()) {
        String userpass = String.format("%s:%s", user, pass);
        String basicAuth = "Basic " +
            new String(new Base64().encode(userpass.getBytes()));
        conn.setRequestProperty("Authorization", basicAuth);
      }

      conn.getInputStream();
      log.info("Jenkins request to start sim game: " + gameId);
      db.updateGameStatusById(gameId, Game.STATE.game_pending);
      log.debug(String.format("Update game: %s to %s", gameId,
          Game.STATE.game_pending.toString()));

      return true;
    }
    catch (Exception e) {
      log.error("Jenkins failure to start simulation game: " + gameId);
      db.updateGameStatusById(gameId, Game.STATE.game_failed);
      throw e;
    }
  }
}
