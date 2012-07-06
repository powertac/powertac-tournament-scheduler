package org.powertac.tourney.services;

import org.powertac.tourney.beans.Machine;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class RunBootstrap extends TimerTask
{
  private String logSuffix = "boot-";
  private String pomId = "";
  private String gameId = "";
  private String machineName = "";

  public RunBootstrap (int gameId, int pomId)
  {
    this.gameId = String.valueOf(gameId);
    this.pomId = String.valueOf(pomId);
  }

  public RunBootstrap (int gameId, int pomId, String machineName)
  {
    this.gameId = String.valueOf(gameId);
    this.pomId = String.valueOf(pomId);
    this.machineName = machineName;
  }

  private void checkMachineAvailable ()
  {
    // TODO This doesn't seem to work properly

    Database db = new Database();
    try {
      db.startTrans();
      List<Machine> machines = db.getMachines();
      List<Machine> available = new ArrayList<Machine>();
      for (Machine m: machines) {
        if (m.getStatus().equalsIgnoreCase("idle") && m.isAvailable()) {
          available.add(m);
        }
      }
      if (available.size() > 0) {
        if (machineName.isEmpty()) {
          int gId = Integer.parseInt(gameId);
          db.updateGameJmsUrlById(gId, "tcp://" + available.get(0).getName() + ":61616");
          db.updateGameMachine(gId, available.get(0).getMachineId());
          db.setMachineStatus(available.get(0).getMachineId(), "running");
          machineName = available.get(0).getName();
        }
        System.out.println("[INFO] Running boot " + gameId + " on machine "
                          + machineName);
        db.commitTrans();
      }
      else {
        db.abortTrans();
        System.out.println(
           "[INFO] No machines available to run scheduled boot: "
           + gameId + " ... will retry in 5 minutes");
        // Thread.sleep(300000);
        // this.run();
     }
   }
   catch (NumberFormatException e) {
     e.printStackTrace();
   }
   catch (SQLException e) {
     e.printStackTrace();
   }
 }

  public void run ()
  {
    // TODO
    checkMachineAvailable();

    // TODO Check if we still need machineName
    String finalUrl =
        "http://localhost:8080/jenkins/job/"
        + "start-server-instance/buildWithParameters?"
        + "token=start-instance"
        + "&tourneyUrl=" + Utils.getTourneyUrl()
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machineName
        + "&gameId=" + gameId;

    System.out.println("[INFO] Final url: " + finalUrl);

    try {
      // TODO Check if we need getinputstream
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
      System.out.println("[INFO] Jenkins request to bootstrap game: " + gameId);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println("[INFO] Jenkins failure to bootstrap game: " + gameId);
      Database db = new Database();
      try {
        db.updateGameStatusById(Integer.parseInt(gameId), "boot-failed");
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
