package org.powertac.tourney.services;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Tournament;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;


public class RunBootstrap
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String logSuffix = "boot-";
  private int gameId;
  private String machineName = "";
  private int watchDogInterval;
  private TournamentProperties properties = TournamentProperties.getProperties();

  public RunBootstrap (int gameId)
  {
    this.gameId = gameId;

    watchDogInterval = Integer.parseInt(
        properties.getProperty("scheduler.watchDogInterval")) / 1000;

    run();
  }

  private void run ()
  {
    Database db = new Database();
    try {
      db.startTrans();

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
      log.info("Failed to bootstrap game: " + gameId);
    }
  }

  private boolean checkMachineAvailable (Database db) throws SQLException
  {
    try {
      log.info("Claiming free machine");

      Machine machine = db.claimFreeMachine();
      if (machine != null) {
        String jmsUrl = "tcp://" + machine.getUrl() + ":61616";
        db.updateGameJmsUrlById(gameId, jmsUrl);
        db.updateGameMachine(gameId, machine.getMachineId());
        machineName = machine.getName();
        log.info(String.format("Running boot %s on machine %s",
            gameId, machineName));
        return true;
      }

      log.info(String.format("No machines available to run scheduled boot: %s"
          + "... will retry in %s seconds", gameId, watchDogInterval));
      return false;
    }
    catch (SQLException sqle) {
      log.warn("Error claiming free machines for boot " + gameId);
      throw sqle;
    }
  }

  private boolean startJob (Database db) throws Exception
  {
    Tournament t = db.getTournamentByGameId(gameId);
    int pomId = t.getPomId();

    String finalUrl =
        properties.getProperty("jenkins.location")
            + "job/start-boot-server/buildWithParameters?"
            + "tourneyUrl=" + properties.getProperty("tourneyUrl")
            + "&suffix=" + logSuffix
            + "&pomId=" + pomId
            + "&machine=" + machineName
            + "&gameId=" + gameId;

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
      log.info("Jenkins request to bootstrap game: " + gameId);
      db.updateGameStatusById(gameId, Game.STATE.boot_in_progress);
      db.setGameReadyTime(gameId);
      log.debug(String.format("Update game: %s to %s", gameId,
          Game.STATE.boot_in_progress.toString()));

      return true;
    }
    catch (Exception e) {
      log.info("Jenkins failure to bootstrap game: " + gameId);
      db.updateGameStatusById(gameId, Game.STATE.boot_failed);
      throw e;
    }
  }
}
