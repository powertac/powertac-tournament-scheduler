package org.powertac.tournament.servlets;

import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Scheduler;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;


@WebServlet(description = "Access to the REST API", urlPatterns = {"/Rest"})
public class Rest extends HttpServlet
{
  public Rest ()
  {
    super();
  }

  protected void doGet (HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    String type = request.getParameter("type");
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    String result = "{}";
    if (type.equals("brokers")) {
      result = parseBrokers();
    }
    else if (type.equals("games")) {
      result = parseGames();
    }
    else if (type.equals("visualizers")) {
      result = parseVisualizers();
    }
    else if (type.equals("watchdog")) {
      result = parseWatchdog();
    }

    response.setContentLength(result.length());
    out.print(result);
    out.flush();
    out.close();
  }

  private String parseBrokers ()
  {
    String result = "{ ";

    for (Integer i : MemStore.getBrokerCheckins().keySet()) {
      if (MemStore.getBrokerCheckins().get(i) == null) {
        continue;
      }

      result += "\"" + i.toString() + "\": \"";

      Iterator<Long> iter = MemStore.getBrokerCheckins().get(i).iterator();
      while (iter.hasNext()) {
        Long checkin = iter.next();
        int stamp = (int) (System.currentTimeMillis() - checkin) / 1000;
        if (stamp > 900) {
          iter.remove();
          MemStore.removeBrokerCheckin(i, checkin);
        }
        else if (stamp < 60) {
          result += "<b>" + stamp + "</b> ";
        }
        else {
          result += stamp + " ";
        }
      }
      result += "\" , ";
    }

    if (result.length() > 2) {
      result = result.substring(0, result.length() - 2);
    }
    result += " }";

    return result;
  }

  private String parseGames ()
  {
    String result = "{ ";

    for (Integer i : MemStore.getGameHeartbeats().keySet()) {
      String[] messages = MemStore.getGameHeartbeats().get(i);
      if (messages == null) {
        continue;
      }

      result += "\"" + i.toString() + "\": \"";

      try {
        int stamp = (int)
            (System.currentTimeMillis() - Long.parseLong(messages[1])) / 1000;
        if (stamp > 900) {
          MemStore.removeGameHeartbeat(i);
          MemStore.removeGameLength(i);
        }
        else {
          Integer gameLength = MemStore.getGameLengths().get(i);
          if (gameLength == null) {
            result += messages[0] + " (" + stamp + ")";
          }
          else {
            result += messages[0] + " / " + gameLength + " (" + stamp + ")";
          }
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      result += "\" , ";
    }

    if (result.length() > 2) {
      result = result.substring(0, result.length() - 2);
    }
    result += " }";

    return result;
  }

  private String parseVisualizers ()
  {
    String result = "{ ";

    for (String s : MemStore.getVizCheckins().keySet()) {
      if (MemStore.getVizCheckins().get(s) == null) {
        continue;
      }

      result += "\"" + s + "\": \"";

      try {
        long stamp =
            (System.currentTimeMillis() - MemStore.getVizCheckins().get(s)) / 1000;
        if (stamp > 900) {
          MemStore.removeVizCheckin(s);
        }
        else {
          result += String.valueOf(stamp);
        }
      }
      catch (Exception ignored) {
      }

      result += "\" , ";
    }

    if (result.length() > 2) {
      result = result.substring(0, result.length() - 2);
    }
    result += " }";

    return result;
  }

  private String parseWatchdog ()
  {
    Scheduler scheduler = Scheduler.getScheduler();
    return "{ \"text\": \"WatchDog running "
        + scheduler.getLastSchedulerRun() + "\" }";
  }
}
