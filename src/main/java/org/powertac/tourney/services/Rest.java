package org.powertac.tourney.services;

import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("rest")
public class Rest
{
  private static HashMap<String, Integer> skip = new HashMap<String, Integer>();

  public String parseBrokerLogin (Map<String, String[]> params)
  {
    String responseType = params.get(Constants.Rest.REQ_PARAM_TYPE)[0];
    String brokerAuthToken = params.get(Constants.Rest.REQ_PARAM_AUTH_TOKEN)[0];
    String competitionName = params.get(Constants.Rest.REQ_PARAM_JOIN)[0];

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

  public String parseServerInterface (Map<String, String[]> params)
  {
    // TODO Check IP of sending client
    //if (!Utils.checkClientAllowed(request.getRemoteAddr())) {
    //  return "error";
    //}

    try {
      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (actionString.equalsIgnoreCase("status")) {
        String statusString = params.get(Constants.Rest.REQ_PARAM_STATUS)[0];
        int gameId = Integer.parseInt(
            params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0]);
        return handleStatus(statusString, gameId);
      }
      else if (actionString.equalsIgnoreCase("boot")) {
        String gameId = params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0];
        return serveBoot(gameId);
      }
    }
    catch (Exception ignored) {}
    return "error";
  }

  /***
   * Returns a properties file string
   *
   * @param params
   * @return String representing a properties file
   */
  public String parseProperties (Map<String, String[]> params)
  {
    String gameId = "0";
    try {
      gameId = params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0];
    }
    catch (Exception ignored) {}

    List<String> props;
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

    String weatherLocation = "server.weatherService.weatherLocation = ";
    String startTime = "common.competition.simulationBaseTime = ";
    String jms = "server.jmsManagementService.jmsBrokerUrl = ";
    String remote = "server.visualizerProxyService.remoteVisualizer = ";
    String queueName = "server.visualizerProxyService.visualizerQueueName = ";
    String minTimeslot = "common.competition.minimumTimeslotCount = 1320";
    String expectedTimeslot = "common.competition.expectedTimeslotCount = 1440";
    String serverFirstTimeout =
      "server.competitionControlService.firstLoginTimeout = 600000";
    String serverTimeout =
      "server.competitionControlService.loginTimeout = 120000";

    // Test settings
    if (g.getGameName().contains("Test") || g.getGameName().contains("test")) {
    	minTimeslot = "common.competition.minimumTimeslotCount = 200";
    	expectedTimeslot = "common.competition.expectedTimeslotCount = 220";
    }

    String result = "";
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
  public String parsePom (Map<String, String[]> params)
  {
    try {
      String pomId = params.get(Constants.Rest.REQ_PARAM_POM_ID)[0];
      return servePom(pomId);
    }
    catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      return "error";
    }
  }

  public String servePom(String pomId)
  {
    String result = "";
    try {
      // Determine pom-file location
      TournamentProperties properties = new TournamentProperties();
      String pomLocation = properties.getProperty("pomLocation") +
                            "pom."+ pomId +".xml";

      // Read the file
      FileInputStream fstream = new FileInputStream(pomLocation);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        result += strLine + "\n";
      }

      // Close the input stream
      fstream.close();
      in.close();
      br.close();
    }
    catch (Exception e) {
      System.err.println("Error : " + e.getMessage());
      result = "error";
    }

    return result;
  }

  public String serveBoot(String gameId)
  {
    String result = "";

    try {
      // Determine boot-file location
      TournamentProperties properties = new TournamentProperties();
      String bootLocation = properties.getProperty("bootLocation") +
                            "game-" + gameId + "-boot.xml";

      // Read the file
      FileInputStream fstream = new FileInputStream(bootLocation);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        result += strLine + "\n";
      }

      // Close the input stream
      fstream.close();
      in.close();
      br.close();
    }
    catch (Exception e) {
      System.err.println("Error : " + e.getMessage());
      result = "error";
    }

    return result;
  }

  // TODO This should be moved to a Game StateMachine
  private String handleStatus(String status, int gameId)
  {
    Scheduler scheduler = (Scheduler) SpringApplicationContext.getBean("scheduler");

    if (status.equalsIgnoreCase("boot-running")) {
      System.out.println(
          "[INFO] Recieved bootstrap running message from game: " + gameId);

      // Remove bootfile, but it shouldn't exist
      removeBootFile(gameId);

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
    else if (status.equalsIgnoreCase("boot-done")) {
      System.out.println(
          "[INFO] Recieved bootstrap done message from game: " + gameId);

      Database db = new Database();
      try {
        db.startTrans();

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
    else if (status.equalsIgnoreCase("boot-failed")) {
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
    else if (status.equalsIgnoreCase("game-ready")) {
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
    else if (status.equalsIgnoreCase("game-running")) {
      // TODO Implement a message from the server to the ts
    }
    else if (status.equalsIgnoreCase("game-done")) {
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
    else if (status.equalsIgnoreCase("game-failed")) {
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
    else {
      return "error";
    }
    return "success";
  }

  private void removeBootFile(int gameId)
  {
    TournamentProperties properties = new TournamentProperties();
    String bootLocation = properties.getProperty("bootLocation") +
        gameId + "-boot.xml";
    File f = new File(bootLocation);

    if (!f.exists()) {
      return;
    }

    if (!f.canWrite()) {
      System.out.println("[Error] Write protected: " + bootLocation);
    }

    if (!f.delete()) {
      System.out.println("[Error] Failed to delete : " + bootLocation);
    }
  }
}
