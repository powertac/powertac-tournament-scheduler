package org.powertac.tourney.actions;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.Agent;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
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
  private List<Map.Entry<String, Double>> resultMap = new ArrayList<Map.Entry<String, Double>>();

  public ActionTournament()
  {
    loadData();
  }

  private void loadData ()
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

      loadTournamentInfo();
      loadMaps();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  private int getTournamentId ()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("tournamentId"));
    }
    catch (NumberFormatException ignored) {
      Utils.redirect();
      return -1;
    }
  }

  private void loadTournamentInfo ()
  {
    tournamentInfo.add("Status : " + tournament.getStatus());
    tournamentInfo.add("StartTime : " + tournament.startTimeUTC());
    tournamentInfo.add("Date from : " + tournament.dateFromUTC());
    tournamentInfo.add("Date to : " + tournament.dateToUTC());

    tournamentInfo.add("MaxBrokers : " + tournament.getMaxBrokers());
    tournamentInfo.add("Registered Brokers : " + tournament.getBrokerMap().size());
    tournamentInfo.add("MaxAgents : " + tournament.getMaxAgents());

    tournamentInfo.add("Type : " + tournament.getType());
    if (tournament.typeEquals(Tournament.TYPE.MULTI_GAME)) {
      tournamentInfo.add("GameSize 1 : " + tournament.getSize1());
      tournamentInfo.add("GameSize 2 : " + tournament.getSize2());
      tournamentInfo.add("GameSize 3 : " + tournament.getSize3());
    }

    tournamentInfo.add("Pom Id : " + tournament.getPomId());
    tournamentInfo.add("Locations : " + tournament.getLocations());
  }

  private void loadMaps ()
  {
    Map<String, Double> temp = new HashMap<String, Double>();

    for (Game game: tournament.getGameMap().values()) {
      List<Agent> agents = new ArrayList<Agent>();

      for (Agent agent: game.getAgentMap().values()) {
        agents.add(agent);

        String brokerName = agent.getBroker().getBrokerName();
        if (agent.getBalance() == -1) {
          continue;
        }
        if (temp.get(brokerName) != null) {
          temp.put(brokerName, (temp.get(brokerName) + agent.getBalance()));
        } else {
          temp.put(brokerName, agent.getBalance());
        }
      }

      agentsMap.put(game.getGameId(), agents);
    }

    for (Map.Entry<String, Double> entry: temp.entrySet()) {
      resultMap.add(entry);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public Tournament getTournament() {
    return tournament;
  }
  public List<String> getTournamentInfo() {
    return tournamentInfo;
  }
  public Map<Integer, List> getAgentsMap () {
    return agentsMap;
  }
  public List<Map.Entry<String, Double>> getResultMap () {
    return resultMap;
  }
  //</editor-fold>
}
