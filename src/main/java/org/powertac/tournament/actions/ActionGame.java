package org.powertac.tournament.actions;

import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@ManagedBean
public class ActionGame implements InitializingBean
{
  private Game game;
  private List<String> gameInfo = new ArrayList<>();
  private List<Map.Entry<String, Double>> resultMap = new ArrayList<>();

  public ActionGame ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    int gameId = getGameId();
    if (gameId < 1) {
      return;
    }

    game = Game.getGameFromId(gameId);
    if (game == null) {
      Utils.redirect();
      return;
    }

    loadGameInfo();
    loadResultMap();
  }

  private int getGameId ()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("gameId"));
    }
    catch (NumberFormatException ignored) {
      Utils.redirect();
      return -1;
    }
  }

  private void loadGameInfo ()
  {
    gameInfo.add("Id : " + game.getGameId());
    gameInfo.add("Name : " + game.getGameName());
    gameInfo.add("Status : " + game.getState());

    if (game.getState().isComplete()) {
      String link = String.format("<a href=\"%s\">link</a>", game.getLogURL());
      gameInfo.add("Logs : " + String.format(link, game.getGameId()));

      gameInfo.add("Game length : " + game.getGameLength());
      gameInfo.add("Last tick : " + game.getLastTick());
    }

    gameInfo.add("Round : <a href=\"round.xhtml?roundId=" +
        +game.getRound().getRoundId() + "\">"
        + game.getRound().getRoundName() + "</a>");

    if (game.getMachine() != null) {
      gameInfo.add("Running on : " + game.getMachine().getMachineName());
      gameInfo.add("Viz Url : " + game.getMachine().getVizUrl());
    }

    gameInfo.add("StartTime : " + game.startTimeUTC());
    gameInfo.add("Real StartTime : " + game.readyTimeUTC());
    gameInfo.add("Location : " + game.getLocation());
    gameInfo.add("SimStartTime : " + game.getSimStartTime());
  }

  private void loadResultMap ()
  {
    for (Agent agent : game.getAgentMap().values()) {
      Map.Entry<String, Double> entry2 =
          new AbstractMap.SimpleEntry<>(
              agent.getBroker().getBrokerName(), agent.getBalance());
      resultMap.add(entry2);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public Game getGame ()
  {
    return game;
  }

  public void setGame (Game game)
  {
    this.game = game;
  }

  public List<String> getGameInfo ()
  {
    return gameInfo;
  }

  public List<Map.Entry<String, Double>> getResultMap ()
  {
    return resultMap;
  }
  //</editor-fold>
}
