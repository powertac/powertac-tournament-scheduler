package org.powertac.tournament.servlets;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Properties;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.powertac.tournament.constants.Constants.Prop;
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

  private String parseProperties (HttpServletRequest request)
  {
    // Allow slaves and admin users
    User user = User.getCurrentUser();
    if (!MemStore.checkMachineAllowed(request.getRemoteAddr()) &&
        !user.isAdmin()) {
      return "error";
    }

    int gameId;
    try {
      String niceName = request.getParameter(Rest.REQ_PARAM_GAMEID);
      gameId = MemStore.getGameId(niceName);
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
    Properties properties = Properties.getProperties();

    StringBuilder result = new StringBuilder();
    result.append(Prop.weatherServerURL)
        .append(properties.getProperty("weatherServerLocation")).append("\n");
    result.append(Prop.weatherLocation).append(game.getLocation()).append("\n");
    Location location = Location.getLocationByName(game.getLocation());
    result.append(Prop.timezoneOffset).append(location.getTimezone()).append("\n");
    result.append(Prop.startTime).append(game.getSimStartTime()).append("\n");
    result.append(Prop.jms).append("tcp://0.0.0.0:61616\n");
    result.append(Prop.serverFirstTimeout).append(600000).append("\n");
    result.append(Prop.serverTimeout).append(120000).append("\n");
    result.append(Prop.remote).append(true).append("\n");
    result.append(Prop.vizQ).append(game.getVisualizerQueue()).append("\n");

    if (game.getGameLength() > 0) {
      result.append(Prop.minTimeslot).append(game.getGameLength()).append("\n");
      result.append(Prop.expectedTimeslot).append(game.getGameLength()).append("\n");
    }
    else {
      boolean test = game.getGameName().toLowerCase().contains("test");

      result.append(Prop.minTimeslot);
      if (test)
        result.append(properties.getPropertyInt("test.minimumTimeslotCount"));
      else
        result.append(properties.getPropertyInt("competition.minimumTimeslotCount"));
      result.append("\n");

      result.append(Prop.expectedTimeslot);
      if (test)
        result.append(properties.getPropertyInt("test.expectedTimeslotCount"));
      else
        result.append(properties.getPropertyInt("competition.expectedTimeslotCount"));
      result.append("\n");
    }

    return result.toString();
  }
}
