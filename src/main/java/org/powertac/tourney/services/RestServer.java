/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 1/28/13
 * Time: 2:19 PM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.constants.Constants;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Map;

public class RestServer
{
  private static Logger log = Logger.getLogger("TMLogger");

  private TournamentProperties properties = TournamentProperties.getProperties();

  public String handleGet (Map<String, String[]> params,
                           HttpServletRequest request)
  {
    try {
      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (actionString.equalsIgnoreCase(Constants.Rest.REQ_PARAM_STATUS)) {
        if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
          return "error";
        }

        return handleStatus(params);
      } else if (actionString.equalsIgnoreCase(Constants.Rest.REQ_PARAM_BOOT)) {
        String gameId = params.get(Constants.Rest.REQ_PARAM_GAMEID)[0];
        return serveBoot(gameId);
      } else if (actionString.equalsIgnoreCase(Constants.Rest.REQ_PARAM_HEARTBEAT)) {
        if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
          return "error";
        }

        return handleHeartBeat(params);
      }
    } catch (Exception ignored) {
    }
    return "error";
  }

  /**
   * Handle 'PUT' to serverInterface.jsp, either boot.xml or (Boot|Sim) log
   */
  public String handlePUT (Map<String, String[]> params,
                           HttpServletRequest request)
  {
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String fileName = params.get(Constants.Rest.REQ_PARAM_FILENAME)[0];

      log.info("Received a file " + fileName);

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
      is.close();
      fos.close();

      // TODO Check if still needed : receiving standings from the game directly
      // If sim-logs received, extract end-of-game standings
      if (fileName.contains("sim-logs")) {
        try {
          Runnable r = new SimLogParser(
              properties.getProperty("logLocation"), fileName);
          new Thread(r).start();
        } catch (Exception e) {
          log.error("Error creating LogParser for " + fileName);
        }
      }
    } catch (Exception e) {
      return "error";
    }
    return "success";
  }

  /**
   * Handle 'POST' to serverInterface.jsp, this is an end-of-game message
   */
  public String handlePOST (Map<String, String[]> params,
                            HttpServletRequest request)
  {
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (!actionString.equalsIgnoreCase(Constants.Rest.REQ_PARAM_GAMERESULTS)) {
        log.debug("The message didn't have the right action-string!");
        return "error";
      }

      int gameId = Integer.parseInt(
          params.get(Constants.Rest.REQ_PARAM_GAMEID)[0]);
      if (!(gameId > 0)) {
        log.debug("The message didn't have a gameId!");
        return "error";
      }

      Session session = HibernateUtil.getSessionFactory().openSession();
      Transaction transaction = session.beginTransaction();
      try {
        Game game = (Game) session.get(Game.class, gameId);
        String standings = params.get(Constants.Rest.REQ_PARAM_MESSAGE)[0];
        return game.handleStandings(session, standings, true);
      } catch (Exception e) {
        transaction.rollback();
        e.printStackTrace();
        return "error";
      } finally {
        session.close();
      }
    } catch (Exception e) {
      log.error("Something went wrong with receiving the POST message!");
      log.error(e.getMessage());
      return "error";
    }
  }

  /**
   * Returns a properties file string
   *
   * @param params :
   * @return String representing a properties file
   */
  public String parseProperties (Map<String, String[]> params,
                                 HttpServletRequest request)
  {
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    int gameId;
    try {
      gameId = Integer.parseInt(params.get(Constants.Rest.REQ_PARAM_GAMEID)[0]);
    } catch (Exception ignored) {
      return "";
    }

    Game game;
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      game = (Game) session.get(Game.class, gameId);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "";
    } finally {
      session.close();
    }

    return getPropertiesString(game);
  }

  private String getPropertiesString (Game game)
  {
    TournamentProperties properties = TournamentProperties.getProperties();

    String result = "";
    result += String.format(Constants.Props.weatherServerURL,
        properties.getProperty("weatherServerLocation"));
    result += String.format(Constants.Props.weatherLocation, game.getLocation());
    result += String.format(Constants.Props.startTime, game.getSimStartTime());
    if (game.getMachine() != null) {
      result += String.format(Constants.Props.jms, game.getMachine().getJmsUrl());
    } else {
      result += String.format(Constants.Props.jms, "tcp://localhost:61616");
    }
    result += String.format(Constants.Props.serverFirstTimeout, 600000);
    result += String.format(Constants.Props.serverTimeout, 120000);
    result += String.format(Constants.Props.remote, true);
    result += String.format(Constants.Props.vizQ, game.getVisualizerQueue());

    int minTimeslotCount =
        properties.getPropertyInt("competition.minimumTimeslotCount", "1400");
    int expTimeslotCount =
        properties.getPropertyInt("competition.expectedTimeslotCount", "1440");
    if (game.getGameName().toLowerCase().contains("test")) {
      minTimeslotCount =
          properties.getPropertyInt("test.minimumTimeslotCount", "200");
      expTimeslotCount =
          properties.getPropertyInt("test.expectedTimeslotCount", "220");
    }
    result += String.format(Constants.Props.minTimeslot, minTimeslotCount);
    result += String.format(Constants.Props.expectedTimeslot, expTimeslotCount);

    Location location = Location.getLocationByName(game.getLocation());
    result += String.format(Constants.Props.timezoneOffset,
        location.getTimezone());

    return result;
  }

  /**
   * Returns a pom file string
   *
   * @param params :
   * @return String representing a pom file
   */
  public String parsePom (Map<String, String[]> params)
  {
    try {
      String pomId = params.get(Constants.Rest.REQ_PARAM_POM_ID)[0];
      return servePom(pomId);
    } catch (Exception e) {
      log.error(e.getMessage());
      return "error";
    }
  }

  private String servePom (String pomId)
  {
    String result = "";
    try {
      // Determine pom-file location
      String pomLocation = properties.getProperty("pomLocation") +
          "pom." + pomId + ".xml";

      // Read the file
      FileInputStream fstream = new FileInputStream(pomLocation);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        result += strLine + "\n";
      }

      // Close the streams
      fstream.close();
      in.close();
      br.close();
    } catch (Exception e) {
      log.error(e.getMessage());
      result = "error";
    }

    return result;
  }

  private String serveBoot (String gameId)
  {
    String result = "";

    try {
      // Determine boot-file location
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

      // Close the streams
      fstream.close();
      in.close();
      br.close();
    } catch (Exception e) {
      log.error(e.getMessage());
      result = "error";
    }

    return result;
  }

  private String handleStatus (Map<String, String[]> params)
  {
    String statusString = params.get(Constants.Rest.REQ_PARAM_STATUS)[0];
    int gameId = Integer.parseInt(
        params.get(Constants.Rest.REQ_PARAM_GAMEID)[0]);

    log.info(String.format("Received %s message from game: %s",
        statusString, gameId));

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_GAME_BY_ID);
      query.setInteger("gameId", gameId);
      Game game = (Game) query.uniqueResult();

      if (game == null) {
        log.warn(String.format("Trying to set status %s on non-existing "
            + "game : %s", statusString, gameId));
        return "error";
      }

      game.handleStatus(session, statusString);
      transaction.commit();
      return "success";
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "error";
    } finally {
      session.close();

      String[] gameLengths = params.get(Constants.Rest.REQ_PARAM_GAMELENGTH);
      if (gameLengths != null && transaction.wasCommitted()) {
        log.info(String.format("Received gamelength %s for game %s",
            gameLengths[0], gameId));
        MemStore.addGameLength(gameId, gameLengths[0]);
      }
    }
  }

  private String handleHeartBeat (Map<String, String[]> params)
  {
    int gameId;

    try {
      String message = params.get(Constants.Rest.REQ_PARAM_MESSAGE)[0];
      gameId = Integer.parseInt(params.get(Constants.Rest.REQ_PARAM_GAMEID)[0]);
      if (!(gameId > 0)) {
        log.debug("The message didn't have a gameId!");
        return "error";
      }

      MemStore.addGameHeartbeat(gameId, message);
    } catch (Exception e) {
      e.printStackTrace();
      return "error";
    }

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Game game = (Game) session.get(Game.class, gameId);
      String standings = params.get(Constants.Rest.REQ_PARAM_STANDINGS)[0];
      return game.handleStandings(session, standings, false);
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "error";
    } finally {
      session.close();
    }
  }
}