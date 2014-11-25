package org.powertac.tournament.servlets;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Tournament;
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


@WebServlet(description = "REST API for brokers",
    urlPatterns = {"/brokerLogin.jsp"})
public class RestBroker extends HttpServlet
{
  private static Logger log = Utils.getLogger();

  private static String responseType = "text/plain; charset=UTF-8";

  public RestBroker ()
  {
    super();
  }

  synchronized protected void doGet (HttpServletRequest request,
                                     HttpServletResponse response)
      throws IOException
  {
    String result = parseBrokerLogin(request);

    response.setContentType(responseType);
    response.setContentLength(result.length());

    PrintWriter out = response.getWriter();
    out.print(result);
    out.flush();
    out.close();
  }

  public String parseBrokerLogin (HttpServletRequest request)
  {
    String responseType = request.getParameter(Rest.REQ_PARAM_TYPE);
    String brokerAuth = request.getParameter(Rest.REQ_PARAM_AUTH_TOKEN);
    String joinName = request.getParameter(Rest.REQ_PARAM_JOIN);

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

    log.info(String.format("Broker %s login request : %s",
        brokerAuth, joinName));

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_BROKER_BY_BROKERAUTH);
      query.setString("brokerAuth", brokerAuth);
      Broker broker = (Broker) query.uniqueResult();

      if (broker == null) {
        log.info("Broker doesn't exists : " + brokerAuth);
        transaction.commit();
        return doneResponse;
      }
      log.debug("Broker id is : " + broker.getBrokerId());

      // Check if the broker registered for a running tournament
      Tournament tournament = getTournament(session, broker, joinName);

      // Tournament doesn't exist, is finished or broker not registered
      if (tournament == null) {
        transaction.commit();
        return doneResponse;
      }

      // Only games more than X minutes ready, allowing the Viz to Login first
      String readyString = getReadyString (session, broker, loginResponse,
          tournament.getTournamentId());
      if (readyString != null) {
        transaction.commit();
        return readyString;
      }

      log.debug("No games ready to start for tournament : " + joinName);
      MemStore.addBrokerCheckin(broker.getBrokerId());
      transaction.commit();
      return String.format(retryResponse, 60);
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error, sending retry response");
      return String.format(retryResponse, 60);
    }
    finally {
      session.close();
    }
  }

  private Tournament getTournament (Session session, Broker broker,
                                    String joinName)
  {
    Query query = session.createQuery(Constants.HQL.GET_TOURNAMENT_BY_NAME);
    query.setString("tournamentName", joinName);
    Tournament tournament = (Tournament) query.uniqueResult();

    if (tournament == null) {
      log.debug("Tournament doesn't exists : " + joinName);
      return null;
    }

    if (tournament.isComplete()) {
      log.debug("Tournament is finished, we're done : " + joinName);
      return null;
    }

    if (tournament.getBrokerMap().get(broker.getBrokerId()) == null) {
      log.debug("Tournament exists, but broker isn't registered : " + joinName);
      return null;
    }

    return tournament;
  }

  @SuppressWarnings("unchecked")
  private String getReadyString (Session session, Broker broker,
                                 String loginResponse, int tournamentId)
  {
    // Wait 10 seconds, game is set ready before it actually starts
    long readyDeadline = 10 * 1000;
    long nowStamp = Utils.offsetDate().getTime();

    Query query = session.createQuery(Constants.HQL.GET_GAMES_READY);
    List<Game> games = (List<Game>) query.
        setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

    for (Game game: games) {
      if (game.getRound().getTournamentId() != tournamentId) {
        continue;
      }

      if ((nowStamp - game.getReadyTime().getTime()) < readyDeadline) {
        continue;
      }

      // Check if an other agent already checked in
      Agent agent = game.getAgentMap().get(broker.getBrokerId());
      if (agent == null || !agent.isPending()) {
        continue;
      }

      log.debug("Game " + game.getGameId() + " is ready");

      agent.setStateInProgress();
      session.update(agent);

      log.info(String.format("Sending login to broker %s : %s, %s, %s",
          broker.getBrokerName(), game.getMachine().getJmsUrl(),
          agent.getBrokerQueue(), game.getServerQueue()));
      return String.format(loginResponse, game.getMachine().getJmsUrl(),
          agent.getBrokerQueue(), game.getServerQueue());
    }

    return null;
  }
}
