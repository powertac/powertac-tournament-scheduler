package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Scheduler;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;


public class RunBootstrap implements Runnable
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String logSuffix = "boot-";
  private int pomId;
  private int gameId;
  private String machineName = "";
  private int watchDogInterval;
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Database db = new Database();

  public RunBootstrap (int gameId, int pomId)
  {
    this.gameId = gameId;
    this.pomId = pomId;

    watchDogInterval = Integer.parseInt(
        properties.getProperty("scheduler.watchDogInterval")) / 1000;
    machineName = properties.getProperty("bootserverName", "");
  }

  private boolean checkMachineAvailable ()
  {
    try {
      db.startTrans();

      Machine freeMachine;
      if (machineName.isEmpty()) {
        log.info("Claiming free machine");
        freeMachine = db.claimFreeMachine();
      }
      else {
        log.info("Claiming machine " + machineName);
        freeMachine = db.claimFreeMachine(machineName);
      }

      if (freeMachine != null) {
        String jmsUrl = "tcp://" + freeMachine.getUrl() + ":61616";

        db.updateGameJmsUrlById(gameId, jmsUrl);
        db.updateGameMachine(gameId, freeMachine.getMachineId());
        db.commitTrans();

        machineName = freeMachine.getName();

        log.info(String.format("Running boot %s on machine %s",
            gameId, machineName));

        return true;
      }

      log.info(String.format("No machines available to run scheduled boot: %s"
          + "... will retry in %s seconds", gameId, watchDogInterval));
    }
    catch (Exception e) {
      log.warn("Error claiming free machines for boot " + gameId);
      e.printStackTrace();
    }

    db.abortTrans();
    return false;
 }

  public void run ()
  {
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
        properties.getProperty("jenkinsLocation")
        + "job/start-server-instance/buildWithParameters?"
        + "token=start-instance"
        + "&tourneyUrl=" + properties.getProperty("tourneyUrl")
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machineName
        + "&gameId=" + gameId;

    log.info("Final url: " + finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
      log.info("Jenkins request to bootstrap game: " + gameId);
      Scheduler.bootRunning = true;
      game.setState(Game.STATE.boot_in_progress);
    }
    catch (Exception e) {
      e.printStackTrace();
      log.info("Jenkins failure to bootstrap game: " + gameId);

      Database db = new Database();
      try {
        db.updateGameStatusById(gameId, Game.STATE.boot_failed);
      }
      catch (NumberFormatException e1) {
        e1.printStackTrace();
      }
      catch (SQLException e1) {
        e1.printStackTrace();
      }
      game.setState(Game.STATE.boot_failed);
    }
  }
}
