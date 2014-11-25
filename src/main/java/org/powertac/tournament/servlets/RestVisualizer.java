package org.powertac.tournament.servlets;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Utils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.powertac.tournament.constants.Constants.Rest;


@WebServlet(description = "REST API for visualizers",
    urlPatterns = {"/visualizerLogin.jsp"})
public class RestVisualizer extends HttpServlet
{
  private static Logger log = Utils.getLogger();

  private static String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
  private static String tail = "</message>";
  private static String retryResponse = head + "<retry>%d</retry>" + tail;
  private static String loginResponse = head + "<login><queueName>%s</queueName><serverQueue>%s</serverQueue></login>" + tail;
  private static String errorResponse = head + "<error>%s</error>" + tail;

  private static String responseType = "text/plain; charset=UTF-8";

  public RestVisualizer ()
  {
    super();
  }

  synchronized protected void doGet (HttpServletRequest request,
                                     HttpServletResponse response)
      throws IOException
  {
    String result = parseVisualizerLogin(request);

    response.setContentType(responseType);
    response.setContentLength(result.length());

    PrintWriter out = response.getWriter();
    out.print(result);
    out.flush();
    out.close();
  }

  /**
   * Handles a login GET request from a visualizer of the form<br/>
   * &nbsp;../visualizerLogin.jsp?machineName<br/>
   * Response is either retry(n) to tell the viz to wait n seconds and try again,
   * or queueName(qn) to tell the visualizer to connect to its machine and
   * listen on the queue named qn.
   */
  @SuppressWarnings("unchecked")
  public String parseVisualizerLogin (HttpServletRequest request)
  {
    String machineName = request.getParameter(Rest.REQ_PARAM_MACHINE_NAME);

    String load = request.getParameter(Rest.REQ_PARAM_MACHINE_LOAD);
    if (load == null) {
      load = "";
    }
    MemStore.addMachineLoad(machineName, load);

    log.info("Visualizer login request : " + machineName);

    // Validate source of request
    if (!MemStore.checkVizAllowed(request.getRemoteHost())) {
      return String.format(errorResponse, "invalid login request");
    }

    // Wait 10 seconds, game is set ready before it actually starts
    long readyDeadline1 = 10 * 1000;
    // In the first 60 secs, check if all brokers are logged in
    long readyDeadline2 = (10 + 60) * 1000;
    long nowStamp = Utils.offsetDate().getTime();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();

    try {
      Query query = session.createQuery(Constants.HQL.GET_GAMES_READY);
      List<Game> games = (List<Game>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

      gamesLoop:
      for (Game game : games) {
        if (game.getMachine() == null) {
          continue;
        }
        if (!game.getMachine().getMachineName().equals(machineName)) {
          continue;
        }
        if (!game.isReady()) {
          continue;
        }

        if ((nowStamp - game.getReadyTime().getTime()) < readyDeadline1) {
          continue;
        }

        if ((nowStamp - game.getReadyTime().getTime()) < readyDeadline2) {
          for (Agent agent : game.getAgentMap().values()) {
            if (!agent.isInProgress()) {
              continue gamesLoop;
            }
          }
        }

        String queue = game.getVisualizerQueue();
        String svrQueue = game.getServerQueue();
        log.info("Game available, login visualizer, " + queue + ", " + svrQueue);
        transaction.commit();
        return String.format(loginResponse, queue, svrQueue);
      }

      log.debug("No games available, retry " + machineName);
      MemStore.addVizCheckin(machineName);
      transaction.commit();
      return String.format(retryResponse, 60);
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error(e.toString());
      return String.format(errorResponse, "database error");
    }
    finally {
      session.close();
    }
  }
}
