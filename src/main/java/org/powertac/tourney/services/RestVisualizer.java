/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 1/28/13
 * Time: 2:26 PM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.Game;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class RestVisualizer
{
  private static Logger log = Logger.getLogger("TMLogger");

  /**
   * Handles a login GET request from a visualizer of the form<br/>
   * &nbsp;../visualizerLogin.jsp?machineName<br/>
   * Response is either retry(n) to tell the viz to wait n seconds and try again,
   * or queueName(qn) to tell the visualizer to connect to its machine and
   * listen on the queue named qn.
   */
  public String parseVisualizerLogin (Map<String, String[]> params,
                                      HttpServletRequest request)
  {
    String machineName = params.get("machineName")[0];
    String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
    String tail = "</message>";
    String retryResponse = head + "<retry>%d</retry>" + tail;
    String loginResponse = head + "<login><queueName>%s</queueName><serverQueue>%s</serverQueue></login>" + tail;
    String errorResponse = head + "<error>%s</error>" + tail;

    log.info("Visualizer login request : " + machineName);

    // Validate source of request
    if (!MemStore.checkVizAllowed(request.getRemoteHost())) {
      return String.format(errorResponse, "invalid login request");
    }

    // Wait 30 seconds, game is set ready before it actually starts
    long readyDeadline = 30 * 1000;
    long nowStamp = Utils.offsetDate().getTime();
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      for (Game game: Game.getNotCompleteGamesList()) {
        if (game.getMachine() == null) {
          continue;
        }
        if (!game.getMachine().getMachineName().equals(machineName)) {
          continue;
        }
        if (!game.isReady()) {
          continue;
        }

        if ((nowStamp - game.getReadyTime().getTime()) < readyDeadline) {
          continue;
        }

        String queue = game.getVisualizerQueue();
        String svrQueue = game.getServerQueue();
        log.info("Game available, login visualizer, " + queue + ", " + svrQueue);
        transaction.commit();
        return String.format(loginResponse, queue, svrQueue);
      }

      log.debug("No games available, retry visualizer");
      MemStore.addVizCheckin(machineName);
      transaction.commit();
      return String.format(retryResponse, 60);
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error(e.toString());
      return String.format(errorResponse, "database error");
    } finally {
      session.close();
    }
  }
}