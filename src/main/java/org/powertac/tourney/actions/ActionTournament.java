package org.powertac.tourney.actions;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.Agent;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
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
public class ActionTournament
{
  private Tournament tournament;
  private List<String> tournamentInfo = new ArrayList<String>();
  private Map<Integer, List> agentsMap = new HashMap<Integer, List>();
  private Map<String, Double[]> resultMap = new HashMap<String, Double[]>();
  private List<Double> avgsAndSDs = new ArrayList<Double>();

  public ActionTournament()
  {
    loadData();
  }

  private void loadData()
  {
    int tournamentId = getTournamentId();
    if (tournamentId < 1) {
      return;
    }

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_TOURNAMENT_BY_ID);
      query.setInteger("tournamentId", tournamentId);
      tournament = (Tournament) query.uniqueResult();

      if (tournament == null) {
        transaction.rollback();
        Utils.redirect();
        return;
      }

      loadTournamentInfo();
      loadMaps();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    } finally {
      session.close();
    }
  }

  private int getTournamentId()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("tournamentId"));
    } catch (NumberFormatException ignored) {
      if (!FacesContext.getCurrentInstance().isPostback()) {
        Utils.redirect();
      }
      return -1;
    }
  }

  private void loadMaps()
  {
    resultMap = tournament.determineWinner();
    avgsAndSDs = tournament.getAvgsAndSDs(resultMap);

    for (Game game : tournament.getGameMap().values()) {
      List<Agent> agents = new ArrayList<Agent>();

      for (Agent agent : game.getAgentMap().values()) {
        agents.add(agent);
      }

      agentsMap.put(game.getGameId(), agents);
    }
  }

  private void loadTournamentInfo()
  {
    tournamentInfo.add("Id : " + tournament.getTournamentId());
    tournamentInfo.add("Name : " + tournament.getTournamentName());
    tournamentInfo.add("Status : " + tournament.getStatus());

    tournamentInfo.add("StartTime : " + tournament.startTimeUTC());
    tournamentInfo.add("Date from : " + tournament.dateFromUTC());
    tournamentInfo.add("Date to : " + tournament.dateToUTC());

    tournamentInfo.add("MaxBrokers : " + tournament.getMaxBrokers());
    tournamentInfo.add("Registered Brokers : " + tournament.getBrokerMap().size());
    tournamentInfo.add("MaxAgents : " + tournament.getMaxAgents());

    tournamentInfo.add("Type : " + tournament.getType());
    if (tournament.isMulti()) {
      tournamentInfo.add(String.format("Size / multiplier 1 : %s / %s",
          tournament.getSize1(), tournament.getMultiplier1()));
      tournamentInfo.add(String.format("Size / multiplier 2 : %s / %s",
          tournament.getSize2(), tournament.getMultiplier2()));
      tournamentInfo.add(String.format("Size / multiplier 3 : %s / %s",
          tournament.getSize3(), tournament.getMultiplier3()));
    }

    tournamentInfo.add("Pom Id : " + tournament.getPomId());
    tournamentInfo.add("Locations : " + tournament.getLocations());

    addCsvLinks();
  }

  private void addCsvLinks()
  {
    TournamentProperties properties = TournamentProperties.getProperties();

    String baseUrl = properties.getProperty("actionIndex.logUrl",
        "download?game=%d");
    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("game"));

    String tournamentCsv = tournament.getTournamentName() + ".csv";
    String gamesCsv = tournament.getTournamentName() + ".games.csv";

    File tournamentFile = new File(String.format("%s%s",
        properties.getProperty("logLocation"), tournamentCsv));
    File gamesFile = new File(String.format("%s%s",
        properties.getProperty("logLocation"), gamesCsv));

    if (tournamentFile.isFile()) {
      if (baseUrl.endsWith("?")) {
        tournamentCsv = "csv=" + tournament.getTournamentName();
      } else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      tournamentInfo.add(String.format(
          "Tournament csv : <a href=\"%s\">link</a>", baseUrl + tournamentCsv));
    }
    if (gamesFile.exists()) {
      if (baseUrl.endsWith("?")) {
        gamesCsv = "csv=" + tournament.getTournamentName() + ".games";
      } else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      tournamentInfo.add(String.format(
          "Games csv : <a href=\"%s\">link</a>", baseUrl + gamesCsv));
    }
  }

  public void createCsv()
  {
    tournament.createCsv();
  }

  //<editor-fold desc="Setters and Getters">
  public Tournament getTournament()
  {
    return tournament;
  }

  public List<String> getTournamentInfo()
  {
    return tournamentInfo;
  }

  public Map<Integer, List> getAgentsMap()
  {
    return agentsMap;
  }

  public Map<String, Double[]> getResultMap()
  {
    return resultMap;
  }

  public List<Double> getAvgsAndSDs()
  {
    return avgsAndSDs;
  }
  //</editor-fold>
}
