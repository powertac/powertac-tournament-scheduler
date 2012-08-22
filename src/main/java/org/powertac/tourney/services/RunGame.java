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


public class RunGame implements Runnable
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String logSuffix = "sim-";
  private int pomId;
  private int gameId;
  private String brokers = "";
  private Machine machine = null;
  private int watchDogInterval;
  private Database db = new Database();
  private TournamentProperties properties = TournamentProperties.getProperties();

  public RunGame (int gameId, int pomId)
  {
    this.gameId = gameId;
    this.pomId = pomId;

    watchDogInterval = Integer.parseInt(
        properties.getProperty("scheduler.watchDogInterval")) / 1000;
  }

  /***
   * Make sure a bootstrap has been run for the sim
   */
  // TODO Should be a Game method
  private boolean checkBootstrap ()
  {
    try {
      db.startTrans();
      if (db.isGameReady(gameId)) {
        db.commitTrans();
        return true;
      }
      else {
        log.info("Game: " + gameId + " reports that boot is not ready!");
        db.updateGameStatusById(gameId, Game.STATE.boot_pending);
      }
    }
    catch (NumberFormatException e) {
      e.printStackTrace();
    }
    catch (SQLException e) {
      log.warn("Bootstrap DB error while scheduling game " + gameId);
      e.printStackTrace();
    }

    db.abortTrans();
    return false;
  }

  /***
   * Make sure brokers are registered for the tournament
   */
  private boolean checkBrokers ()
  {
    try {
      db.startTrans();
      Game g = db.getGame(gameId);

      List<Broker> brokerList = db.getBrokersInGame(gameId);
      if (brokerList.size() < 1) {
        log.info(String.format("Game: %s (tournament %s) reports no brokers "
            + "registered", gameId, g.getTourneyId()));
        return false;
      }
      else {
        for (Broker b: brokerList) {
          if (!b.agentsAvailable(db)) {
            log.info(String.format("Not starting game %s : broker %s doesn't "
                + "have enough available agents", gameId, b.getBrokerId()));
            return false;
          }

          brokers += b.getBrokerName() + "/";
          brokers += db.getBrokerQueueName(gameId, b.getBrokerId()) +",";
        }
        brokers = brokers.substring(0, brokers.length()-1);

        log.info(String.format("There are %s brokers registered for tournament"
            + "... starting sim", brokerList.size()));
        log.info("Broker Ids : " + brokers);

        return true;
      }
    }
    catch (SQLException e) {
      log.warn("Broker DB error while scheduling game " + gameId);
      e.printStackTrace();
      return false;
    }
    finally {
      db.abortTrans();
    }
  }

  /***
   * Make sure there is a machine available for the game
   */
  private boolean checkMachineAvailable ()
  {
    try {
      db.startTrans();

      machine = db.claimFreeMachine();
      if (machine == null) {
        db.abortTrans();
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
      db.commitTrans();
      log.info(String.format("Game: %s running on machine: %s",
          gameId, machine.getName()));

      return true;
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      log.warn("Error claiming free machines for game " + gameId);
      return false;
    }
  }

  @Override
  public synchronized void run ()
  {
    if (!checkBootstrap()) {
      return;
    }

    if (!checkBrokers()) {
      return;
    }

    if (!checkMachineAvailable()) {
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
        properties.getProperty("jenkins.location")
        + "job/start-server-instance/buildWithParameters?"
        + "tourneyUrl=" + properties.getProperty("tourneyUrl")
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machine.getName()
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
      game.setState(Game.STATE.game_pending);
      log.info(String.format("Update game: %s to %s", gameId,
          Game.STATE.game_pending.toString()));
    }
    catch (Exception e) {
      e.printStackTrace();
      log.error("Jenkins failure to start simulation game: " + gameId);
      game.setState(Game.STATE.game_failed);
    }
  }
}
