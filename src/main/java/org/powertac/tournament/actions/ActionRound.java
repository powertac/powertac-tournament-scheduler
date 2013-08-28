package org.powertac.tournament.actions;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.*;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.CSV;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ManagedBean
public class ActionRound implements InitializingBean
{
  private Round round;
  private List<String> roundInfo = new ArrayList<String>();
  private List<String> participantInfo = new ArrayList<String>();
  private List<String> csvLinks = new ArrayList<String>();
  private Map<Integer, List> agentsMap = new HashMap<Integer, List>();
  private Map<Broker, double[]> resultMap = new HashMap<Broker, double[]>();
  private double[] avgsAndSDs;

  public ActionRound ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    int roundId = getRoundId();
    if (roundId < 1) {
      return;
    }

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_ROUND_BY_ID);
      query.setInteger("roundId", roundId);
      round = (Round) query.uniqueResult();

      if (round == null) {
        transaction.rollback();
        Utils.redirect();
        return;
      }

      loadRoundInfo();
      loadParticipantInfo();
      loadCsvLinks();
      loadMaps();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    } finally {
      session.close();
    }
  }

  private int getRoundId ()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("roundId"));
    } catch (NumberFormatException ignored) {
      if (!facesContext.isPostback()) {
        Utils.redirect();
      }
      return -1;
    }
  }

  private void loadMaps ()
  {
    resultMap = round.determineWinner();
    avgsAndSDs = round.getAvgsAndSDsArray(resultMap);

    for (Game game: round.getGameMap().values()) {
      List<Agent> agents = new ArrayList<Agent>();

      for (Agent agent: game.getAgentMap().values()) {
        agents.add(agent);
      }

      agentsMap.put(game.getGameId(), agents);
    }
  }

  private void loadRoundInfo ()
  {
    roundInfo.add("Id : " + round.getRoundId());
    roundInfo.add("Name : " + round.getRoundName());
    roundInfo.add("Status : " + round.getState());

    roundInfo.add("StartTime (UTC) : " + round.startTimeUTC().substring(0, 16));
    roundInfo.add("Date from : " + round.dateFromUTC().substring(0, 10));
    roundInfo.add("Date to : " + round.dateToUTC().substring(0, 10));

    roundInfo.add("MaxBrokers : " + round.getMaxBrokers());
    roundInfo.add("Registered Brokers : " + round.getBrokerMap().size());
    roundInfo.add("MaxAgents : " + round.getMaxAgents());

    roundInfo.add(String.format("Size / multiplier 1 : %s / %s",
        round.getSize1(), round.getMultiplier1()));
    roundInfo.add(String.format("Size / multiplier 2 : %s / %s",
        round.getSize2(), round.getMultiplier2()));
    roundInfo.add(String.format("Size / multiplier 3 : %s / %s",
        round.getSize3(), round.getMultiplier3()));

    roundInfo.add("Pom Id : " + round.getPomId());
    roundInfo.add("Locations : " + round.getLocations());
  }

  private void loadParticipantInfo ()
  {
    for (Broker broker: round.getBrokerMap().values()) {
      User participant = broker.getUser();
      participantInfo.add(String.format("%s, %s, %s",
          broker.getBrokerName(),
          participant.getInstitution(),participant.getContactName()));
    }
    java.util.Collections.sort(participantInfo);
  }

  private void loadCsvLinks ()
  {
    csvLinks = CSV.getRoundCsvLinks(round);
  }

  public void createCsv ()
  {
    CSV.createRoundCsv(round);
  }

  //<editor-fold desc="Setters and Getters">
  public Round getRound ()
  {
    return round;
  }

  public List<String> getRoundInfo ()
  {
    return roundInfo;
  }

  public List<String> getParticipantInfo ()
  {
    return participantInfo;
  }

  public List<String> getCsvLinks ()
  {
    return csvLinks;
  }

  public Map<Integer, List> getAgentsMap ()
  {
    return agentsMap;
  }

  public Map<Broker, double[]> getResultMap ()
  {
    return resultMap;
  }

  public double[] getAvgsAndSDs ()
  {
    return avgsAndSDs;
  }
  //</editor-fold>
}
