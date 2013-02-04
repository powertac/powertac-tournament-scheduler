/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 1/28/13
 * Time: 2:26 PM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.constants.Constants;

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

      // Check if the broker registered for a running competition
      Tournament tournament;
      int runningTourneyId = isRunningCompetition(session, joinName, broker);

      // Broker not registered to competition with joinName, check tourneys
      if (runningTourneyId == -2) {
        tournament = getRunningTournament(session, joinName, broker);
      }
      // Broker not registered to competition with joinName, check tourneys
      else if (runningTourneyId == -1) {
        transaction.commit();
        return doneResponse;
      }
      // Broker registered to competition, but no tourneys available
      else if (runningTourneyId == 0) {
        // TODO Should we check tourney with same name?
        log.debug("No tournament is ready for competition : " + joinName);
        MemStore.addBrokerCheckin(broker.getBrokerId());
        return String.format(retryResponse, 60);
      }
      // We found running tournament in competition + broker is registered
      else {
        query = session.createQuery(Constants.HQL.GET_TOURNAMENT_BY_ID);
        query.setInteger("tournamentId", runningTourneyId);
        tournament = (Tournament) query.uniqueResult();
      }

      // No competition-tournament or tournament found
      if (tournament == null) {
        transaction.commit();
        return doneResponse;
      }

      // Check if any ready games that are more than X minutes ready
      // This allows the Viz to Login first
      Game game = getReadyGame(session, tournament, broker);
      if (game != null) {
        Agent agent = game.getAgentMap().get(broker.getBrokerId());
        transaction.commit();
        return String.format(loginResponse, game.getMachine().getJmsUrl(),
            agent.getBrokerQueue(), game.getServerQueue());
      }

      log.debug("No games ready to start for tournament : " + joinName);
      MemStore.addBrokerCheckin(broker.getBrokerId());
      transaction.commit();
      return String.format(retryResponse, 60);
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error, sending done response");
      return doneResponse;
    } finally {
      session.close();
    }
  }

  /*
   * Ugly hack : returns
   *  tourneyId : broker is registered to running tourney (always > 0)
   *  0 : broker is registered to competition, but no tourneys available
   *  -1 : broker isn't registered to competition
   *  -2 : there's no competition with joinName
   */
  private int isRunningCompetition(Session session,
                                   String joinName, Broker broker)
  {
    Query query = session.createQuery(Constants.HQL.GET_COMPETITION_BY_NAME);
    query.setString("competitionName", joinName);
    Competition competition = (Competition) query.uniqueResult();
    int result = -2;

    if (competition == null) {
      log.debug("Competition doesn't exists : " + joinName);
      return -2;
    }

    if (competition.isComplete()) {
      log.debug("Competition is finished, we're done : " + joinName);
      return -1;
    }

    for (CompetitionRound round: competition.getRoundMap().values()) {
      for (Tournament tournament: round.getTournamentMap().values()) {
        if (broker.getTournamentMap().get(tournament.getTournamentId())==null) {
          continue;
        }

        // We now know broker is registered for competition
        if (tournament.isComplete()) {
          result = 0;
          continue;
        }

        // Found a running tourney in competition that broker is registered for
        return tournament.getTournamentId();
      }
    }

    return result;
  }

  private Tournament getRunningTournament (Session session, String joinName,
                                           Broker broker)
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

    if (broker.getTournamentMap().get(tournament.getTournamentId()) == null) {
      log.debug(String.format("Broker not registered for tournament " +
          tournament.getTournamentName()));
      return null;
    }

    return tournament;
  }

  private Game getReadyGame (Session session, Tournament tournament,
                             Broker broker)
  {
    long readyDeadline = 2 * 60 * 1000;
    long nowStamp = Utils.offsetDate().getTime();

    for (Game game: tournament.getGameMap().values()) {
      if (!game.isReady()) {
        continue;
      }
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
      return game;
    }

    return null;
  }
}