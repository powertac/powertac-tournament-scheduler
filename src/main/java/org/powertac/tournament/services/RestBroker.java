/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 1/28/13
 * Time: 2:26 PM
 */

package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.*;
import org.powertac.tournament.constants.Constants;

import java.util.List;
import java.util.Map;

public class RestBroker
{
  private static Logger log = Logger.getLogger("TMLogger");

  public String parseBrokerLogin (Map<String, String[]> params)
  {
    String responseType = params.get(Constants.Rest.REQ_PARAM_TYPE)[0];
    String brokerAuth = params.get(Constants.Rest.REQ_PARAM_AUTH_TOKEN)[0];
    String joinName = params.get(Constants.Rest.REQ_PARAM_JOIN)[0];
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

    Session session = HibernateUtil.getSessionFactory().openSession();
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
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error, sending retry response");
      return String.format(retryResponse, 60);
    } finally {
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
    long readyDeadline = 2 * 60 * 1000;
    long nowStamp = Utils.offsetDate().getTime();

    Query query = session.createQuery(Constants.HQL.GET_GAMES_READY);
    List<Game> games = (List<Game>) query.
        setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

    for (Game game: games) {
      if (game.getRound().getLevel().getTournamentId() != tournamentId) {
        continue;
      }

      // Check if an other agent already checked in
      Agent agent = game.getAgentMap().get(broker.getBrokerId());
      if (agent == null || !agent.isPending()) {
        continue;
      }

      log.debug("Game " + game.getGameId() + " is ready");

      long diff = nowStamp - game.getReadyTime().getTime();
      if (diff < readyDeadline) {
        log.debug("Broker needs to wait for the viz timeout : " +
            (readyDeadline - diff) / 1000);
        continue;
      }

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