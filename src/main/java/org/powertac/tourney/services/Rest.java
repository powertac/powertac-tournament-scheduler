package org.powertac.tourney.services;

import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("rest")
public class Rest
{
  private Scheduler scheduler;
  private static HashMap<String, Integer> skip = new HashMap<String, Integer>();
  private static boolean lock;

  public String parseBrokerLogin (Map<?, ?> params)
  {
    String responseType = ((String[]) params.get(Constants.REQ_PARAM_TYPE))[0];
    String brokerAuthToken =
      ((String[]) params.get(Constants.REQ_PARAM_AUTH_TOKEN))[0];
    String competitionName =
      ((String[]) params.get(Constants.REQ_PARAM_JOIN))[0];

    SimpleDateFormat dateFormatUTC =
      new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

    String retryResponse;
    String loginResponse;
    String doneResponse;

    if (responseType.equalsIgnoreCase("xml")) {
      retryResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><retry>%d</retry></message>";
      loginResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><login><jmsUrl>%s</jmsUrl><gameToken>%s</gameToken></login></message>";
      doneResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><done></done></message>";
    }
    else {
      retryResponse = "{\n \"retry\":%d\n}";
      loginResponse = "{\n \"login\":%d\n \"jmsUrl\":%s\n \"gameToken\":%s\n}";
      doneResponse = "{\n \"done\":\"true\"\n}";
    }
    Database db = new Database();

    try {
      db.startTrans();
      List<Game> allGames = db.getGames();
      List<Tournament> allTournaments = db.getTournaments("pending");
      allTournaments.addAll(db.getTournaments("in-progress"));
      if (competitionName != null && allGames != null) {

        // First find all games that match the competition name and have brokers
        // registered
        List<Game> matches = new ArrayList<Game>();
        for (Game g: allGames) {
          // Only consider games that have started and are ready for
          // brokers to join
          Tournament t = db.getTournamentByGameId(g.getGameId());
          if (g.getStatus().equalsIgnoreCase("game-in-progress")) {

            if (competitionName.equalsIgnoreCase(t.getTournamentName())
                && g.isBrokerRegistered(brokerAuthToken)) {
              synchronized (skip) {
                if (skip.containsKey(g.getGameId() + brokerAuthToken)
                    && skip.get(g.getGameId() + brokerAuthToken) == g
                            .getGameId()) {
                  System.out.println("[INFO] Broker " + brokerAuthToken
                                     + " already recieved login for game "
                                     + g.getGameId());
                  continue;
                }
                System.out.println("[INFO] Sending login to : "
                                   + brokerAuthToken + " jmsUrl : "
                                   + g.getJmsUrl());
                skip.put(g.getGameId() + brokerAuthToken, g.getGameId());
              }
              return String.format(loginResponse, g.getJmsUrl(), "1234");
            }
          }
        }
        db.commitTrans();

        boolean competitionExists = false;

        for (Tournament t: allTournaments) {
          if (competitionName.equals(t.getTournamentName())) {
            competitionExists = true;
            break;
          }
        }

        if (competitionExists) {
          System.out.println("[INFO] Broker: " + brokerAuthToken
                             + " attempted to log into existing tournament: "
                             + competitionName + " --sending retry");
          return String.format(retryResponse, 20);
        }
        else {
          System.out
                  .println("[INFO] Broker: " + brokerAuthToken
                           + " attempted to log into non-existing tournament: "
                           + competitionName + " --sending done");
          return doneResponse;
        }

      }
      Thread.sleep(30000);
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
    
    return doneResponse;
  }

  // TODO This should be moved to a Game StateMachine
  public String parseServerInterface (Map<?, ?> params)
  {
    // TODO Check IP of sending client

    String actionString = ((String[]) params.get(Constants.REQ_PARAM_ACTION))[0];
    if (actionString.equalsIgnoreCase("status")) {
      scheduler = (Scheduler) SpringApplicationContext.getBean("scheduler");
      String statusString = "";
      String gameIdString = "";
      int gameId = -1;
      try {
        statusString = ((String[]) params.get(Constants.REQ_PARAM_STATUS))[0];
        gameIdString = ((String[]) params.get(Constants.REQ_PARAM_GAME_ID))[0];
        gameId = Integer.parseInt(gameIdString);
      }
      catch (Exception e) {
        return "ERROR";
      }

      if (statusString.equalsIgnoreCase("bootstrap-running")) {
        System.out.println(
            "[INFO] Recieved bootstrap running message from game: " + gameId);
        Database db = new Database();
        try {
          db.startTrans();
          db.updateGameStatusById(gameId, "boot-in-progress");
          db.commitTrans();
          System.out.println(
              "[INFO] Setting game: " + gameId + " to boot-in-progress");
        }
        catch (SQLException e) {
          db.abortTrans();
          e.printStackTrace();
        }
      }
      else if (statusString.equalsIgnoreCase("bootstrap-done")) {
        System.out.println(
            "[INFO] Recieved bootstrap done message from game: " + gameId);

        Database db = new Database();
        try {
          db.startTrans();
          TournamentProperties props = new TournamentProperties();
          String fileUploadLocation = props.getProperty("fileUploadLocation");
          if (!fileUploadLocation.endsWith("/")) {
            fileUploadLocation += "/";
          }
          String updateUrl = Utils.getTourneyUrl() + "faces/pom.jsp?location="
                             + fileUploadLocation + gameId + "-boot.xml";
          db.updateGameBootstrapById(gameId, updateUrl);
          db.updateGameStatusById(gameId, "boot-complete");

          scheduler.bootrunning = false;
          Game g = db.getGame(gameId);
          db.setMachineStatus(g.getMachineId(), "idle");
          db.commitTrans();

          System.out.println(
              "[INFO] Setting game: " + gameId + " to boot-complete");
        }
        catch (Exception e) {
          db.abortTrans();
          e.printStackTrace();
        }
      }
      else if (statusString.equalsIgnoreCase("game-ready")) {
        System.out.println(
                "[INFO] Recieved game ready message from game: " + gameId);
        Database db = new Database();
        try {
            db.startTrans();
            db.updateGameStatusById(gameId, "game-in-progress");
            db.commitTrans();
            System.out.println(
                    "[INFO] Setting game: " + gameId + " to game-in-progress");
        }
        catch (SQLException e) {
            db.abortTrans();
            e.printStackTrace();
        }
      }
      else if (statusString.equalsIgnoreCase("game-running")) {
        // TODO Implement a message from the server to the ts
      }
      else if (statusString.equalsIgnoreCase("game-done")) {
        System.out.println(
                "[INFO] Recieved game done message from game: " + gameId);

        Database db = new Database();
        Game g = null;
        try {
            db.startTrans();
            db.updateGameStatusById(gameId, "game-complete");
            System.out.println("[INFO] Setting game: " + gameId
                    + " to game-complete");
            g = db.getGame(gameId);
            // Do some cleanup
            db.updateGameFreeBrokers(gameId);
            System.out.println("[INFO] Freeing Brokers for game: " + gameId);
            db.updateGameFreeMachine(gameId);
            System.out.println("[INFO] Freeing Machines for game: " + gameId);

            db.setMachineStatus(g.getMachineId(), "idle");
            db.commitTrans();
        }
        catch (Exception e) {
            db.abortTrans();
            e.printStackTrace();
        }
        scheduler.resetServer(g.getMachineId());
      }
      else if (statusString.equalsIgnoreCase("game-failed")) {
        System.out.println("[WARN] GAME " + gameId + " FAILED!");
        Database db = new Database();
        try {
            db.startTrans();
            db.updateGameStatusById(gameId, "game-failed");
            Game g = db.getGame(gameId);

            db.updateGameFreeBrokers(gameId);
            db.updateGameFreeMachine(gameId);

            scheduler.resetServer(g.getMachineId());
            db.setMachineStatus(g.getMachineId(), "idle");

            db.commitTrans();
        }
        catch (SQLException e) {
            db.abortTrans();
            e.printStackTrace();
        }
      }
      else if (statusString.equalsIgnoreCase("boot-failed")) {
        System.out.println("[WARN] GAME " + gameId + " FAILED!");
        Database db = new Database();
        try {
            db.startTrans();
            db.updateGameStatusById(gameId, "boot-failed");
            Game g = db.getGame(gameId);
            db.setMachineStatus(g.getMachineId(), "idle");
            db.commitTrans();
        }
        catch (SQLException e) {
            db.abortTrans();
            e.printStackTrace();
        }
      }
      else {
        return "ERROR";
      }
      return "success";
    }
    return "ERROR";
  }

  /***
   * Returns a properties file string
   * 
   * @param params
   * @return String representing a properties file
   */
  public String parseProperties (Map<?, ?> params)
  {
    String gameId = "0";
    if (params != null) {
      try {
        gameId = ((String[]) params.get(Constants.REQ_PARAM_GAME_ID))[0];
      }
      catch (Exception e) {}
    }

    List<String> props = new ArrayList<String>();

    props = CreateProperties.getPropertiesForGameId(Integer.parseInt(gameId));
    
    Game g = new Game();
    Database db = new Database();
    try{
    	db.startTrans();
    	g = db.getGame(Integer.parseInt(gameId));
    	db.commitTrans();
    }catch(Exception e){
    	db.abortTrans();
    	e.printStackTrace();
    }

    String result = "";

    // TODO Get these from Constant
    // Location of weather data
    String weatherLocation = "server.weatherService.weatherLocation = ";
    // Simulation base time
    String startTime = "common.competition.simulationBaseTime = ";
    // Simulation jmsUrl
    String jms = "server.jmsManagementService.jmsBrokerUrl = ";
    // Visualizer Settings
    String remote = "server.visualizerProxyService.remoteVisualizer = ";// true";
    String queueName = "server.visualizerProxyService.visualizerQueueName = ";
    // Test Settings
    //String minTimeslot = "common.competition.minimumTimeslotCount = 220";
    //String expectedTimeslot = "common.competition.expectedTimeslotCount = 240";
    String minTimeslot = "common.competition.minimumTimeslotCount = 1320";
    String expectedTimeslot = "common.competition.expectedTimeslotCount = 1440";
    String serverFirstTimeout =
      "server.competitionControlService.firstLoginTimeout = 600000";
    // Timeout Settings
    String serverTimeout =
      "server.competitionControlService.loginTimeout = 120000";
    
    if (g.getGameName().contains("Test") || g.getGameName().contains("test")) {
    	minTimeslot = "common.competition.minimumTimeslotCount = 200";
    	expectedTimeslot = "common.competition.expectedTimeslotCount = 220";
    }

    if (props.size() == 4) {
      result += weatherLocation + props.get(0) + "\n";
      result += startTime + props.get(1) + "\n";
      if (props.get(2).isEmpty()) {
    	  result += jms + "tcp://localhost:61616" + "\n";
      }
      else {
    	  result += jms + props.get(2) + "\n";
      }
      result += serverFirstTimeout + "\n";
      result += serverTimeout + "\n";
      if (props.get(2).length() > 2) {
        result += remote + "true\n";
      }
      else {
        result += remote + "\n";
      }
      result += minTimeslot + "\n";
      result += expectedTimeslot + "\n";
      result += queueName + props.get(3) + "\n";
    }

    return result;
  }

  /***
   * Returns a pom file string
   * 
   * @param params
   * @return String representing a pom file
   */
  public String parsePom (Map<?, ?> params)
  {
    String location = "";
    if (params != null) {
      try {
        location = ((String[]) params.get(Constants.REQ_PARAM_POM))[0];
      }
      catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        return "";
      }
    }

    String result = "";
    try {
      // Open the file that is the first command line parameter
      List<String> path = new ArrayList<String>();
      String[] pathArray = (location.split("/"));
      for (String s: pathArray) {
        path.add(s.replace("..", ""));
      }
      TournamentProperties properties = new TournamentProperties();
      FileInputStream fstream = new FileInputStream(
          properties.getProperty("fileUploadLocation", "/export/scratch")
          + path.get(path.size() - 1));
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      // Read File Line By Line
      while ((strLine = br.readLine()) != null) {
        // Print the content on the console
        // System.out.println (strLine);
        result += strLine + "\n";
      }
      // Close the input stream
      fstream.close();
      in.close();
      br.close();
    }
    catch (Exception e) {// Catch exception if any
      System.err.println("Error: " + e.getMessage());
    }

    return result;
  }
}
