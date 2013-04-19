package org.powertac.tourney.actions;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.CSV;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ManagedBean
@RequestScoped
public class ActionRound
{
  private Round round;
  private List<String> roundInfo = new ArrayList<String>();
  private List<String> participantInfo = new ArrayList<String>();
  private List<String> csvLinks = new ArrayList<String>();
  private Map<Integer, List> agentsMap = new HashMap<Integer, List>();
  private Map<Broker, Double[]> resultMap = new HashMap<Broker, Double[]>();
  private List<Double> avgsAndSDs = new ArrayList<Double>();

  public ActionRound ()
  {
    loadData();
  }

  private void loadData ()
  {
    int roundId = getRoundId();
    if (roundId < 1) {
      return;
    }

    Session session = HibernateUtil.getSessionFactory().openSession();
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
      addCsvLinks();
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
      if (!FacesContext.getCurrentInstance().isPostback()) {
        Utils.redirect();
      }
      return -1;
    }
  }

  private void loadMaps ()
  {
    resultMap = round.determineWinner();
    avgsAndSDs = round.getAvgsAndSDs(resultMap);

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

  private void addCsvLinks ()
  {
    TournamentProperties properties = TournamentProperties.getProperties();

    String baseUrl = properties.getProperty("actionIndex.logUrl",
        "download?game=%d");
    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("game"));

    String roundCsv = round.getRoundName() + ".csv";
    String gamesCsv = round.getRoundName() + ".games.csv";

    File roundFile = new File(String.format("%s%s",
        properties.getProperty("logLocation"), roundCsv));
    File gamesFile = new File(String.format("%s%s",
        properties.getProperty("logLocation"), gamesCsv));

    if (roundFile.exists()) {
      if (baseUrl.endsWith("?")) {
        roundCsv = "csv=" + round.getRoundName();
      } else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      csvLinks.add(String.format(
          "Round csv : <a href=\"%s\">link</a>", baseUrl + roundCsv));
    }
    if (gamesFile.exists()) {
      if (baseUrl.endsWith("?")) {
        gamesCsv = "csv=" + round.getRoundName() + ".games";
      } else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      csvLinks.add(String.format(
          "Games csv : <a href=\"%s\">link</a>", baseUrl + gamesCsv));
    }
  }

  public void createCsv ()
  {
    CSV.createCsv(round);
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

  public Map<Broker, Double[]> getResultMap ()
  {
    return resultMap;
  }

  public List<Double> getAvgsAndSDs ()
  {
    return avgsAndSDs;
  }
  //</editor-fold>
}
