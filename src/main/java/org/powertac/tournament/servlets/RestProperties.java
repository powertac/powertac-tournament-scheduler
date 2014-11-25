package org.powertac.tournament.servlets;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.TournamentProperties;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.powertac.tournament.constants.Constants.Props;
import static org.powertac.tournament.constants.Constants.Rest;


@WebServlet(description = "REST API to retrieve the properties",
    urlPatterns = {"/properties.jsp"})
public class RestProperties extends HttpServlet
{
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
      gameId = Integer.parseInt(request.getParameter(Rest.REQ_PARAM_GAMEID));
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
    result += String.format(Props.weatherServerURL,
        properties.getProperty("weatherServerLocation"));
    result += String.format(Props.weatherLocation, game.getLocation());
    result += String.format(Props.startTime, game.getSimStartTime());
    if (game.getMachine() != null) {
      result += String.format(Props.jms, game.getMachine().getJmsUrl());
    }
    else {
      result += String.format(Props.jms, "tcp://localhost:61616");
    }
    result += String.format(Props.serverFirstTimeout, 600000);
    result += String.format(Props.serverTimeout, 120000);
    result += String.format(Props.remote, true);
    result += String.format(Props.vizQ, game.getVisualizerQueue());

    if (game.getGameLength() > 0) {
      result += String.format(Props.minTimeslot, game.getGameLength());
      result += String.format(Props.expectedTimeslot, game.getGameLength());
    }
    else {
      boolean test = game.getGameName().toLowerCase().contains("test");
      result += String.format(Props.minTimeslot, test
          ? properties.getPropertyInt("test.minimumTimeslotCount")
          : properties.getPropertyInt("competition.minimumTimeslotCount"));
      result += String.format(Props.expectedTimeslot, test
          ? properties.getPropertyInt("test.expectedTimeslotCount")
          : properties.getPropertyInt("competition.expectedTimeslotCount"));
    }

    Location location = Location.getLocationByName(game.getLocation());
    result += String.format(Props.timezoneOffset, location.getTimezone());

    return result;
  }
}
