package org.powertac.tourney.services;

import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Scheduler;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;

import static org.powertac.tourney.services.Utils.log;

public class RunBootstrap implements Runnable
{
  private String logSuffix = "boot-";
  private int pomId;
  private int gameId;
  private String machineName = "";
  private int watchDogInterval;
  private TournamentProperties properties = TournamentProperties.getProperties();
  private Database db;

  public RunBootstrap (int gameId, int pomId)
  {
    this.gameId = gameId;
    this.pomId = pomId;

    db = new Database();

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
        log("Claiming free machine");
        freeMachine = db.claimFreeMachine();
      }
      else {
        log("Claiming machine {0}", machineName);
        freeMachine = db.claimFreeMachine(machineName);
      }

      if (freeMachine != null) {
        String jmsUrl = "tcp://" + freeMachine.getUrl() + ":61616";

        db.updateGameJmsUrlById(gameId, jmsUrl);
        db.updateGameMachine(gameId, freeMachine.getMachineId());
        db.commitTrans();

        machineName = freeMachine.getName();

        log("[INFO] Running boot {0} on machine {1}", gameId, machineName);
        return true;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    db.abortTrans();
    return false;
 }

  public void run ()
  {
    if (!checkMachineAvailable()) {
      log("[INFO] No machines available to run scheduled boot: {0}... will retry"
          + " in {1} seconds", gameId, watchDogInterval);
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
        "http://localhost:8080/jenkins/job/"
        + "start-server-instance/buildWithParameters?"
        + "token=start-instance"
        + "&tourneyUrl=" + properties.getProperty("tourneyUrl")
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machineName
        + "&gameId=" + gameId;

    log("[INFO] Final url: {0}", finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
      log("[INFO] Jenkins request to bootstrap game: {0}", gameId);
      Scheduler.bootRunning = true;
      game.setState(Game.STATE.boot_in_progress);
    }
    catch (Exception e) {
      e.printStackTrace();
      log("[INFO] Jenkins failure to bootstrap game: {0}", gameId);

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
