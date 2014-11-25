package org.powertac.tournament.servlets;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.TournamentProperties;
import org.powertac.tournament.services.Utils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@WebServlet(description = "REST API to retrieve the properties",
    urlPatterns = {"/properties.jsp"})
public class RestProperties extends HttpServlet
{
  private static Logger log = Utils.getLogger();

  private static String responseType = "text/plain; charset=UTF-8";

  public RestProperties ()
  {
    super();
  }

  protected void doGet (HttpServletRequest request, HttpServletResponse response)
      throws IOException
  {
    String result = parseProperties(request);

    response.setContentType(responseType);
    response.setContentLength(result.length());

    PrintWriter out = response.getWriter();
    out.print(result);
    out.flush();
    out.close();
  }

  public String parseProperties (HttpServletRequest request)
  {
    // Allow slaves and admin users
    User user = User.getCurrentUser();
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr()) &&
        !user.isAdmin()) {
      return "error";
    }

    int gameId;
    try {
      gameId = Integer.parseInt(
          request.getParameter(Constants.Rest.REQ_PARAM_GAMEID));
    }
    catch (Exception ignored) {
      return "";
    }

    Game game;
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      game = (Game) session.get(Game.class, gameId);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "";
    }
    finally {
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
    }
    else {
      result += String.format(Constants.Props.jms, "tcp://localhost:61616");
    }
    result += String.format(Constants.Props.serverFirstTimeout, 600000);
    result += String.format(Constants.Props.serverTimeout, 120000);
    result += String.format(Constants.Props.remote, true);
    result += String.format(Constants.Props.vizQ, game.getVisualizerQueue());

    int minTimeslotCount =
        properties.getPropertyInt("competition.minimumTimeslotCount");
    int expTimeslotCount =
        properties.getPropertyInt("competition.expectedTimeslotCount");
    if (game.getGameName().toLowerCase().contains("test")) {
      minTimeslotCount =
          properties.getPropertyInt("test.minimumTimeslotCount");
      expTimeslotCount =
          properties.getPropertyInt("test.expectedTimeslotCount");
    }
    result += String.format(Constants.Props.minTimeslot, minTimeslotCount);
    result += String.format(Constants.Props.expectedTimeslot, expTimeslotCount);

    Location location = Location.getLocationByName(game.getLocation());
    result += String.format(Constants.Props.timezoneOffset,
        location.getTimezone());

    return result;
  }
}
