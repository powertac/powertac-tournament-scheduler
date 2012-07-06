package org.powertac.tourney.services;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class RunGame extends TimerTask
{
  private Machine machine;

  private boolean running = false;
  private boolean tourney = false;

  private String logSuffix = "sim-";
  private String pomId = "";
  private String gameId = "";
  private String brokers = "";
  private String machineName = "";

  public RunGame (int gameId, int pomId)
  {
    this.gameId = String.valueOf(gameId);
    this.pomId = String.valueOf(pomId);
	  running = false;
  }

  public RunGame (int gameId, int pomId, Machine machine, String brokers)
  {
    this.gameId = String.valueOf(gameId);
    this.pomId = String.valueOf(pomId);
    this.brokers = brokers;
    this.machine = machine;

    running = false;
    tourney = true;
  }

  /***
   * Make sure a bootstrap has been run for the sim
   */
  private void checkBootstrap ()
  {
    if (!tourney) {
      if (!running) {
        Database db = new Database();

        try {
          db.startTrans();
          if (db.isGameReady(Integer.parseInt(gameId))) {
            try {
              db.updateGameStatusById(Integer.parseInt(gameId), "game-pending");
              db.commitTrans();
            }
            catch (SQLException e) {
              db.abortTrans();
              e.printStackTrace();
            }
          }
          else {
            System.out.println("Game: " + gameId
                               + " reports that bootstrap is not ready!");
          }
        }
        catch (NumberFormatException e) {
          e.printStackTrace();
        }
        catch (SQLException e) {
          System.out.println("Bootstrap Database error while scheduling sim!!");
          this.cancel();
          e.printStackTrace();
        }
      }
    }

  }

  /***
   * Make sure brokers are registered for the tournament
   */
  private boolean checkBrokers ()
  {
    if (!tourney) {
      if (!running) {
        Database db = new Database();

        int gId = Integer.parseInt(gameId);

        try {
          db.startTrans();
          Game g = db.getGame(gId);

          int numRegistered = db.getNumberBrokersRegistered(g.getTourneyId());
          if (numRegistered < 1) {
            System.out.println("TourneyId: " + g.getTourneyId());
            System.out.println("No brokers registered, waiting to start game " + gameId);
            db.updateGameStatusById(gId, "boot-complete");
            db.commitTrans();
            cancel();
            return false;
          }
          else {
            System.out.println("There are "+ numRegistered
                + " brokers registered for tournament... starting sim");
            brokers = "";

            List<Broker> brokerList =
              db.getBrokersInGame(Integer.parseInt(gameId));
            for (Broker b: brokerList) {
              brokers += b.getBrokerName() + ",";
            }
            brokers = brokers.substring(0, brokers.length() - 1);

            if (brokerList.size() < 1) {
              System.out.println(
                  "Error no brokers listed in database for gameId: " + gameId);
              cancel();
              db.abortTrans();
              return false;
            }
          }

          db.commitTrans();
        }
        catch (SQLException e) {
          db.abortTrans();
          System.out.println("Broker Database error while scheduling sim!!");
          e.printStackTrace();
        }
      }
    }
    return true;
  }

  // TODO Combine with check in RunBootstrap? Make a queue?
  private void checkMachineAvailable ()
  {
    // TODO

    if (!tourney) {
      Database db = new Database();
      if (!running) {
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
            Machine firstAvailable = available.get(0);
            int gId = Integer.parseInt(gameId);
            String jmsUrl = "tcp://" + firstAvailable.getUrl() + ":61616";

            db.updateGameJmsUrlById(gId, jmsUrl);
            db.updateProperties(gId, jmsUrl, firstAvailable.getVizQueue());
            db.updateGameMachine(gId, firstAvailable.getMachineId());
            db.updateGameViz(gId, firstAvailable.getVizUrl());
            db.setMachineStatus(firstAvailable.getMachineId(), "running");
            machineName = firstAvailable.getName();
            System.out.println("Game: " + gameId + " running on machine: "
                               + machineName);
            db.commitTrans();
          }
          else {
            db.abortTrans();

            System.out.println("No machines available to run scheduled game: "
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
    }
    else {
      Database db = new Database();
      try {
        db.startTrans();
        int gId = Integer.parseInt(gameId);
        String jmsUrl = "tcp://" + machine.getUrl() + ":61616";

        db.updateGameJmsUrlById(gId, jmsUrl);
        db.updateProperties(gId, jmsUrl, machine.getVizQueue());
        db.updateGameMachine(gId, machine.getMachineId());
        db.updateGameViz(gId, machine.getVizUrl());
        db.setMachineStatus(machine.getMachineId(), "running");
        machineName = machine.getName();
        System.out.println("Game: " + gameId + " running on machine: "
                           + machineName);
        db.commitTrans();
      }
      catch (Exception e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
  }

  @Override
  public void run ()
  {
    // Check if a boot exists
    checkBootstrap();

    // Check if brokers are registered
    if (!checkBrokers()) {
      return;
    }
    // Check if there is a machine available to run the sim and set it
    // TODO
    checkMachineAvailable();

    String finalUrl =
        "http://localhost:8080/jenkins/job/"
        + "start-server-instance/buildWithParameters?"
        + "token=start-instance"
        + "&tourneyUrl=" + Utils.getTourneyUrl()
        + "&suffix=" + logSuffix
        + "&pomId=" + pomId
        + "&machine=" + machineName
        + "&gameId=" + gameId
        + "&brokers=" + brokers;

      /*
        http://localhost:8080/jenkins/job/
          start-server-instance/buildWithParameters?
          token=start-instance&
          tourneyUrl=http://127.0.1.1:8080/TournamentScheduler/
          &suffix=sim-
          &propUrl=http://127.0.1.1:8080/TournamentScheduler/faces/properties.jsp?gameId=7
          &pomUrl=http://127.0.1.1:8080/TournamentScheduler/faces/pom.jsp?pomId=1
          &bootUrl=http://127.0.1.1:8080/TournamentScheduler/faces/serverInterface.jsp?action=boot&gameId=7
          &machine=localhost
          &gameId=7
          &brokers=LARGEpower1,LARGEpower2
          */

    System.out.println("{INFO] Final url: " + finalUrl);

    try {
      if (!running) {
        // TODO Check if we need getinputstream
        URL url = new URL(finalUrl);
        URLConnection conn = url.openConnection();
        conn.getInputStream();
        System.out.println("Jenkins request to start sim game: " + gameId);
        running = true;
      }
      else {
        // Should not get here
        System.out.println("Request already sent, what?");
      }

    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println("Jenkins failure to start simulation game: " + gameId);
    }
  }
}
