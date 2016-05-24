package org.powertac.tournament.actions;

import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Round.Result;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.CSV;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ManagedBean
public class ActionRound implements InitializingBean
{
  private Round round;
  private List<String> roundInfo = new ArrayList<>();
  private List<String> participantInfo = new ArrayList<>();
  private List<String> csvLinks = new ArrayList<>();
  private Map<Integer, List> agentsMap = new HashMap<>();
  private Map<Broker, Result> resultMap = new HashMap<>();
  private Result roundResults;

  public ActionRound ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    int roundId = getRoundId();
    if (roundId < 1) {
      return;
    }

    round = Round.getRoundFromId(roundId, true);
    loadRoundInfo();
    loadParticipantInfo();
    loadCsvLinks();
    loadMaps();
  }

  private int getRoundId ()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("roundId"));
    }
    catch (NumberFormatException ignored) {
      if (!facesContext.isPostback()) {
        Utils.redirect();
      }
      return -1;
    }
  }

  private void loadMaps ()
  {
    resultMap = round.getResultMap();
    roundResults = resultMap.remove(null);

    for (Game game : round.getGameMap().values()) {
      List<Agent> agents = new ArrayList<>();

      for (Agent agent : game.getAgentMap().values()) {
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

    roundInfo.add("Locations : " + round.getLocations());
  }

  private void loadParticipantInfo ()
  {
    for (Broker broker : round.getBrokerMap().values()) {
      User participant = broker.getUser();
      participantInfo.add(String.format("%s, %s, %s",
          broker.getBrokerName(),
          participant.getInstitution(), participant.getContactName()));
    }
    Collections.sort(participantInfo);
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

  public Map<Broker, Result> getResultMap ()
  {
    return resultMap;
  }

  public Result getRoundResults ()
  {
    return roundResults;
  }
  //</editor-fold>
}
