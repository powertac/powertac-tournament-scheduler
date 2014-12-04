package org.powertac.tournament.actions;

import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.services.Forecaster;
import org.powertac.tournament.services.Scheduler;
import org.powertac.tournament.services.Utils;
import org.primefaces.extensions.event.timeline.TimelineSelectEvent;
import org.primefaces.extensions.model.timeline.TimelineEvent;
import org.primefaces.extensions.model.timeline.TimelineModel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// http://www.primefaces.org/showcase-ext/sections/timeline/basic.jsf

@ManagedBean
@ViewScoped
public class ActionTimeline implements Serializable
{
  private static TimelineModel model;

  public ActionTimeline ()
  {
    // Get the games of all running rounds
    Map<Integer, Game> gamesMap = new HashMap<Integer, Game>();
    Scheduler scheduler = Scheduler.getScheduler();
    for (Round round : scheduler.getRunningRounds()) {
      gamesMap.putAll(round.getGameMap());
    }

    Forecaster forecaster = new Forecaster(gamesMap);
    forecaster.createSchedule();
    fillTimeline(forecaster.getGamesMap(),
        forecaster.getStartTimes(), forecaster.getEndTimes());
  }

  private void fillTimeline (Map<Integer, Game> gamesMap,
                             Map<Integer, Long> startTimes,
                             Map<Integer, Long> endTimes)
  {
    // TODO Also write to file for debug purposes
    System.out.println();
    System.out.println("Schedule : ");

    model = new TimelineModel();
    for (Game game : gamesMap.values()) {
      int gameId = game.getGameId();

      // Should never happen
      if (startTimes.get(gameId) == -1 || endTimes.get(gameId) == -1) {
        continue;
      }

      Date start = dateFromLong(startTimes.get(gameId));
      Date end = dateFromLong(endTimes.get(gameId));
      model.add(new TimelineEvent(game, start, end));

      // Also print to sysout for debug purposes
      System.out.println(gameId + " " + Utils.dateToStringFull(start)
          + " " + Utils.dateToStringFull(end));
    }
  }

  // Converts a UTS timestamp to a server-local time
  private Date dateFromLong (long time)
  {
    Calendar cal = Calendar.getInstance();
    int diff = cal.get(Calendar.DST_OFFSET) + cal.get(Calendar.ZONE_OFFSET);
    cal.setTimeInMillis(time * 1000 + diff);
    return cal.getTime();
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
}
