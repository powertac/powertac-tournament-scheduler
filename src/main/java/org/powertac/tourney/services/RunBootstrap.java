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
  private String pomId = "";
  private String gameId = "";
  private String machineName = "";

  private TournamentProperties properties = new TournamentProperties();

  public RunBootstrap (int gameId, int pomId)
  {
    this.gameId = String.valueOf(gameId);
    this.pomId = String.valueOf(pomId);

    machineName = properties.getProperty("bootserverName", "");
  }

  private boolean checkMachineAvailable ()
  {
    Database db = new Database();
    try {
      db.startTrans();

      Machine freeMachine;
      if (machineName.isEmpty()) {
        System.out.println("Claiming free machine");
        freeMachine = db.claimFreeMachine();
      }
      else {
        System.out.println("Claiming machine " + machineName);
        freeMachine = db.claimFreeMachine(machineName);
      }

      if (freeMachine != null) {
        int gId = Integer.parseInt(gameId);
        String jmsUrl = "tcp://" + freeMachine.getUrl() + ":61616";

        db.updateGameJmsUrlById(gId, jmsUrl);
        db.updateGameMachine(gId, freeMachine.getMachineId());
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
        + "&gameId=" + gameId;

    log("[INFO] Final url: {0}", finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
      log("[INFO] Jenkins request to bootstrap game: {0}", gameId);
      Scheduler.bootRunning = true;
    }
    catch (Exception e) {
      e.printStackTrace();
      log("[INFO] Jenkins failure to bootstrap game: {0}", gameId);

      Database db = new Database();
      try {
        db.updateGameStatusById(Integer.parseInt(gameId), Game.STATE.boot_failed);
      }
      catch (NumberFormatException e1) {
        e1.printStackTrace();
      }
      catch (SQLException e1) {
        e1.printStackTrace();
      }
    }
  }
}
