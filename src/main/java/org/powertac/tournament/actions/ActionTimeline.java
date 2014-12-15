package org.powertac.tournament.actions;

import org.powertac.tournament.beans.Game;
import org.powertac.tournament.services.Forecaster;
import org.powertac.tournament.services.Utils;
import org.primefaces.extensions.event.timeline.TimelineSelectEvent;
import org.primefaces.extensions.model.timeline.TimelineEvent;
import org.primefaces.extensions.model.timeline.TimelineModel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

// http://www.primefaces.org/showcase-ext/sections/timeline/basic.jsf

@ManagedBean
@ViewScoped
public class ActionTimeline implements Serializable
{
  private static Forecaster forecaster;
  private static TimelineModel model;
  private static String origin;

  public ActionTimeline ()
  {
    if (forecaster == null) {
      forecaster = Forecaster.createFromRunning();
      origin = "All running rounds";
    }

    fillTimeline();
  }

  private void fillTimeline ()
  {
    if (forecaster == null) {
      return;
    }

    Map<Integer, Game> gamesMap = forecaster.getGamesMap();
    Map<Integer, Long> startTimes = forecaster.getStartTimes();
    Map<Integer, Long> endTimes = forecaster.getEndTimes();

    model = new TimelineModel();
    for (Game game : gamesMap.values()) {
      int gameId = game.getGameId();

      // Should never happen
      if (startTimes.get(gameId) == -1 || endTimes.get(gameId) == -1) {
        continue;
      }

      Date start = Utils.dateFromLong(startTimes.get(gameId));
      Date end = Utils.dateFromLong(endTimes.get(gameId));
      model.add(new TimelineEvent(game, start, end));
    }
  }

  public void onSelect (TimelineSelectEvent e)
  {
    TimelineEvent event = e.getTimelineEvent();
    Game game = (Game) event.getData();
    String message = game.getBrokerIdsInGameString() + "<br/>" +
        Utils.dateToStringFull(event.getStartDate()) + "<br/>" +
        Utils.dateToStringFull(event.getEndDate());

    Utils.growlMessage("Game : " + game.getGameId(), message);
  }

  public TimelineModel getModel ()
  {
    if (model == null) {
      model = new TimelineModel();
    }
    return model;
  }

  public String getOrigin ()
  {
    return origin;
  }

  public void clear ()
  {
    forecaster = null;
    model = null;
    origin = null;
  }

  public static void setForecaster (Forecaster forecaster, String origin)
  {
    ActionTimeline.forecaster = forecaster;
    ActionTimeline.origin = origin;
  }
}
