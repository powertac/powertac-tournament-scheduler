package org.powertac.tourney.services;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.powertac.tourney.services.Utils.log;

@Service("rest")
public class Rest
{
  public String parseBrokerLogin (Map<String, String[]> params)
  {
    log("Broker login request");
    String responseType = params.get(Constants.Rest.REQ_PARAM_TYPE)[0];
    String brokerAuthToken = params.get(Constants.Rest.REQ_PARAM_AUTH_TOKEN)[0];
    String tournamentName = params.get(Constants.Rest.REQ_PARAM_JOIN)[0];

    String retryResponse = "{\n \"retry\":%d\n}";
    String loginResponse = "{\n \"login\":%d\n \"jmsUrl\":%s\n \"queueName\":%s\n \"serverQueue\":%s\n}";
    String doneResponse = "{\n \"done\":\"true\"\n}";
    if (responseType.equalsIgnoreCase("xml")) {
      String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
      String tail = "</message>";
      retryResponse = head + "<retry>%d</retry>" + tail;
      loginResponse = head + "<login><jmsUrl>%s</jmsUrl><queueName>%s</queueName><serverQueue>%s</serverQueue></login>" + tail;
      doneResponse = head + "<done></done>" + tail;
    }

    List<Game> allGames = Game.getGameList();
    // JEC - this could be a REALLY long list after a while.
    List<Tournament> allTournaments = Tournament.getTournamentList();

    if (tournamentName == null || allGames == null) {
      return doneResponse;
    }

    Database db = new Database();
    try {
      db.startTrans();

      // Find games matching the competition name and have brokers registered
      for (Game g: allGames) {
        // Only consider games that are ready (started, but waiting for logins)
        if (!g.stateEquals(Game.STATE.game_ready)) {
          continue;
        }

        // TODO This could be more efficient, refactor with Hibernate
        Tournament t = db.getTournamentByGameId(g.getGameId());
        if (!tournamentName.equalsIgnoreCase(t.getTournamentName())) {
          continue;
        }

        Broker broker = g.getBrokerRegistration(brokerAuthToken);

        if (broker != null && !broker.getBrokerInGame()) {
          broker.setBrokerInGame(true);
          db.updateBrokerInGame(g.getGameId(), broker);
          db.commitTrans();
          return String.format(loginResponse, g.getJmsUrl(),
                               broker.getQueueName(), g.getServerQueue());
        }
      }

      db.commitTrans();

      boolean competitionExists = false;
      for (Tournament t: allTournaments) {
        if (tournamentName.equals(t.getTournamentName())) {
          competitionExists = true;
          break;
        }
      }

      if (competitionExists) {
        log("[INFO] Broker: {0} attempted to log into existing tournament"
            + ": {1} --sending retry", brokerAuthToken, tournamentName);
        return String.format(retryResponse, 60);
      }
      else {
        log("[INFO] Broker: {0} attempted to log into non-existing tournament"
            + ": {1} --sending done", brokerAuthToken, tournamentName);
        return doneResponse;
      }
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return doneResponse;
  }

  /**
   * Handles a login GET request from a visualizer of the form<br/>
   * &nbsp;../visualizerLogin.jsp?machineName<br/>
   * Response is either retry(n) to tell the viz to wait n seconds and try again,
   * or queueName(qn) to tell the visualizer to connect to its machine and
   * listen on the queue named qn.
   */
  public String parseVisualizerLogin (HttpServletRequest request,
                                      Map<String, String[]> params)
  {
    log("Visualizer login request");
    String machineName = params.get("machineName")[0];
    String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
    String tail = "</message>";
    String retryResponse = head + "<retry>%d</retry>" + tail;
    String loginResponse = head + "<login><queueName>%s</queueName></login>" + tail;
    String errorResponse = head + "<error>%s</error>" + tail; 

    // Validate source of request
    if (!validateVizRequest(request)) {
      return String.format(errorResponse, "invalid login request");
    }

    // If there is a game in game_ready state on the machine for this viz,
    // then return a login with the correct queue name; otherwise, return
    // a retry.
    List<Game> readyGames;
    Database db = new Database();
    try {
      db.startTrans();
      readyGames = db.findGamesByStatusAndMachine(Game.STATE.game_ready,
                                                      machineName);
      readyGames.addAll(db.findGamesByStatusAndMachine(Game.STATE.game_pending,
                                                       machineName));
      if (readyGames.isEmpty()) {
        db.commitTrans();
        return String.format(retryResponse, 60);
      }
      else {
        // We'll use the first Game in the list
        Game candidate = readyGames.get(0);
        String queue = candidate.getVisualizerQueue();
        db.commitTrans();
        return String.format(loginResponse, queue);
      }
    }
    catch (SQLException e) {
      db.abortTrans();
      log("ERROR: " + e.toString());
      e.printStackTrace();
      return String.format(errorResponse, "database error");
    }
  }
  
  private boolean validateVizRequest (HttpServletRequest rq)
  {
    String host = rq.getRemoteHost();
    System.out.println("Viz request from " + host);
    return true;
  }

  public String parseServerInterface (Map<String, String[]> params, String clientAddress)
  {
    if (!Utils.checkClientAllowed(clientAddress)) {
      return "error";
    }

    try {
      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (actionString.equalsIgnoreCase("status")) {
        String statusString = params.get(Constants.Rest.REQ_PARAM_STATUS)[0];
        int gameId = Integer.parseInt(
            params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0]);
        return Game.handleStatus(statusString, gameId);
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
    StringBuffer sb = new StringBuffer();
    for (String key : params.keySet()) {
      sb.append(key).append(":").append(params.get(key)).append(",");
    }
    log("parseProperties [{0}]", sb.toString());
    String gameId = "0";
    try {
      gameId = params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0];
    }
    catch (Exception ignored) {}

    List<String> props;
    props = CreateProperties.getPropertiesForGameId(Integer.parseInt(gameId));

    Game g;
    Database db = new Database();
    try {
    	db.startTrans();
    	g = db.getGame(Integer.parseInt(gameId));
    	db.commitTrans();
    }
    catch(Exception e) {
    	db.abortTrans();
    	e.printStackTrace();
      return "";
    }

    String weatherLocation = "server.weatherService.weatherLocation = ";
    String startTime = "common.competition.simulationBaseTime = ";
    String jms = "server.jmsManagementService.jmsBrokerUrl = ";
    String remote = "server.visualizerProxyService.remoteVisualizer = true";
    String vizQ = "server.visualizerProxyService.visualizerQueue = ";
    String minTimeslot = "common.competition.minimumTimeslotCount = 1380";
    String expectedTimeslot = "common.competition.expectedTimeslotCount = 1440";
    String serverFirstTimeout =
      "server.competitionControlService.firstLoginTimeout = 600000";
    String serverTimeout =
      "server.competitionControlService.loginTimeout = 120000";

    // Test settings
    if (g.getGameName().toLowerCase().contains("test")) {
    	minTimeslot = "common.competition.minimumTimeslotCount = 200";
    	expectedTimeslot = "common.competition.expectedTimeslotCount = 220";
    }

    String result = "";
    // JEC - replaced a HORRIBLE magic number.
    if (props.size() > 0) {
      result += weatherLocation + props.get(0) + "\n";
    }
    if (props.size() > 1) {
      result += startTime + props.get(1) + "\n";
    }
    if (props.size() > 2) {
      if (props.get(2).isEmpty()) {
    	  result += jms + "tcp://localhost:61616" + "\n";
      }
      else {
    	  result += jms + props.get(2) + "\n";
      }
    }
    result += serverFirstTimeout + "\n";
    result += serverTimeout + "\n";
    result += remote + "\n";
    result += vizQ + g.getVisualizerQueue() + "\n";
    result += minTimeslot + "\n";
    result += expectedTimeslot + "\n";
    log("Props for game {0}:\n{1}", g.getGameId(), result);

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
      log("Error: " + e.getMessage());
      return "error";
    }
  }

  public String servePom(String pomId)
  {
    String result = "";
    try {
      // Determine pom-file location
      TournamentProperties properties = TournamentProperties.getProperties();
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
      log("Error : " + e.getMessage());
      result = "error";
    }

    return result;
  }

  public String serveBoot(String gameId)
  {
    String result = "";

    try {
      // Determine boot-file location
      TournamentProperties properties = TournamentProperties.getProperties();
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
      log("Error : " + e.getMessage());
      result = "error";
    }

    return result;
  }

  /***
   * Handle 'PUT' to serverInterface.jsp, either boot.xml or (Boot|Sim) log
   */
  public String handleServerInterfacePUT (Map<String, String[]> params, HttpServletRequest request)
  {
    if (!Utils.checkClientAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String fileName = params.get(Constants.Rest.REQ_PARAM_FILENAME)[0];
      TournamentProperties properties = TournamentProperties.getProperties();

      String path;
      if (fileName.endsWith("boot.xml")) {
        path = properties.getProperty("bootLocation") + fileName;
      } else {
        path = properties.getProperty("logLocation") + fileName;
      }

      // Write to file
      InputStream is = request.getInputStream();
      FileOutputStream fos = new FileOutputStream(path);
      byte buf[] = new byte[1024];
      int letti;
      while ((letti = is.read(buf)) > 0) {
        fos.write(buf, 0, letti);
      }
      fos.close();
    } catch (Exception e) {
      return "error";
    }
    return "success";
  }
}
