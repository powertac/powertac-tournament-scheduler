package org.powertac.tournament.servlets;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.SimLogParser;
import org.powertac.tournament.services.TournamentProperties;
import org.powertac.tournament.services.Utils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static org.powertac.tournament.constants.Constants.Rest;


@WebServlet(description = "REST API for game servers",
    urlPatterns = {"/serverInterface.jsp"})
public class RestServer extends HttpServlet
{
  private static Logger log = Utils.getLogger();

  private static String responseType = "text/plain; charset=UTF-8";

  private TournamentProperties properties = TournamentProperties.getProperties();

  public RestServer ()
  {
    super();
  }

  synchronized protected void doGet (HttpServletRequest request,
                                     HttpServletResponse response)
      throws IOException
  {
    String result = handleGET(request);

    writeResult(response, result);
  }

  synchronized protected void doPut (HttpServletRequest request,
                                     HttpServletResponse response)
      throws IOException
  {
    String result = handlePUT(request);

    writeResult(response, result);
  }

  synchronized protected void doPost (HttpServletRequest request,
                                      HttpServletResponse response)
      throws IOException
  {
    String result = handlePOST(request);

    writeResult(response, result);
  }

  private void writeResult (HttpServletResponse response, String result)
      throws IOException
  {
    response.setContentType(responseType);
    response.setContentLength(result.length());

    PrintWriter out = response.getWriter();
    out.print(result);
    out.flush();
    out.close();
  }

  public String handleGET (HttpServletRequest request)
  {
    try {
      String actionString = request.getParameter(Rest.REQ_PARAM_ACTION);
      if (actionString.equalsIgnoreCase(Rest.REQ_PARAM_STATUS)) {
        if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
          return "error";
        }

        return handleStatus(request);
      }
      else if (actionString.equalsIgnoreCase(Rest.REQ_PARAM_BOOT)) {
        String gameId = request.getParameter(Rest.REQ_PARAM_GAMEID);
        return serveBoot(gameId);
      }
      else if (actionString.equalsIgnoreCase(Rest.REQ_PARAM_HEARTBEAT)) {
        if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
          return "error";
        }

        return handleHeartBeat(request);
      }
    }
    catch (Exception ignored) {
    }
    return "error";
  }

  /**
   * Handle 'PUT' to serverInterface.jsp, either boot.xml or (Boot|Sim) log
   */
  public String handlePUT (HttpServletRequest request)
  {
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String fileName = request.getParameter(Rest.REQ_PARAM_FILENAME);

      log.info("Received a file " + fileName);

      String path;
      if (fileName.endsWith("boot.xml")) {
        path = properties.getProperty("bootLocation") + fileName;
      }
      else {
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
        }
        catch (Exception e) {
          log.error("Error creating LogParser for " + fileName);
        }
      }
    }
    catch (Exception e) {
      return "error";
    }
    return "success";
  }

  /**
   * Handle 'POST' to serverInterface.jsp, this is an end-of-game message
   */
  public String handlePOST (HttpServletRequest request)
  {
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String actionString = request.getParameter(Rest.REQ_PARAM_ACTION);
      if (!actionString.equalsIgnoreCase(Rest.REQ_PARAM_GAMERESULTS)) {
        log.debug("The message didn't have the right action-string!");
        return "error";
      }

      int gameId = Integer.parseInt(
          request.getParameter(Constants.Rest.REQ_PARAM_GAMEID));
      if (!(gameId > 0)) {
        log.debug("The message didn't have a gameId!");
        return "error";
      }

      Session session = HibernateUtil.getSession();
      Transaction transaction = session.beginTransaction();
      try {
        Game game = (Game) session.get(Game.class, gameId);
        String standings = request.getParameter(Rest.REQ_PARAM_MESSAGE);
        return game.handleStandings(session, standings, true);
      }
      catch (Exception e) {
        transaction.rollback();
        e.printStackTrace();
        return "error";
      }
      finally {
        session.close();
      }
    }
    catch (Exception e) {
      log.error("Something went wrong with receiving the POST message!");
      log.error(e.getMessage());
      return "error";
    }
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
    }
    catch (Exception e) {
      log.error(e.getMessage());
      result = "error";
    }

    return result;
  }

  private String handleStatus (HttpServletRequest request)
  {
    String statusString = request.getParameter(Rest.REQ_PARAM_STATUS);
    int gameId = Integer.parseInt(
        request.getParameter(Rest.REQ_PARAM_GAMEID));

    log.info(String.format("Received %s message from game: %s",
        statusString, gameId));

    Session session = HibernateUtil.getSession();
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
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "error";
    }
    finally {
      session.close();
    }

    String gameLength = request.getParameter(Rest.REQ_PARAM_GAMELENGTH);
    if (gameLength != null && transaction.wasCommitted()) {
      log.info(String.format("Received gamelength %s for game %s",
          gameLength, gameId));
      MemStore.addGameLength(gameId, gameLength);
    }
    return "success";
  }

  private String handleHeartBeat (HttpServletRequest request)
  {
    int gameId;

    // Write heartbeat + elapsed time to the MemStore
    try {
      String message = request.getParameter(Rest.REQ_PARAM_MESSAGE);
      gameId = Integer.parseInt(
          request.getParameter(Rest.REQ_PARAM_GAMEID));

      if (!(gameId > 0)) {
        log.debug("The message didn't have a gameId!");
        return "error";
      }
      MemStore.addGameHeartbeat(gameId, message);

      long elapsedTime = Long.parseLong(
          request.getParameter(Rest.REQ_PARAM_ELAPSED_TIME));
      if (elapsedTime > 0) {
        MemStore.addElapsedTime(gameId, elapsedTime);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return "error";
    }

    // Write heartbeat to the DB
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Game game = (Game) session.get(Game.class, gameId);
      String standings = request.getParameter(Rest.REQ_PARAM_STANDINGS);
      return game.handleStandings(session, standings, false);
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "error";
    }
    finally {
      session.close();
    }
  }
}
