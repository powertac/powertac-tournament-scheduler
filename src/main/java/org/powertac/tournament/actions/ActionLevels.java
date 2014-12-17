package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.services.Forecaster;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.powertac.tournament.services.Forecaster.Forecast;


@ManagedBean
public class ActionLevels implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  private List<Level> levelList;

  public ActionLevels ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    levelList = Level.getNotCompleteLevelList();
  }

  public String getStatus (Level level)
  {
    String result = "";
    for (Round round : level.getRoundMap().values()) {
      if (round.isPending()) {
        result = "pending";
      }
      else if (round.isStarted() &&
          (result.equals("completed") || result.equals(""))) {
        result = "in_progress";
      }
      else if (round.isComplete() && result.equals("")) {
        result = "completed";
      }
    }
    return result;
  }

  public String getRounds (Level level)
  {
    StringBuilder result = new StringBuilder();

    for (Round round : level.getRoundMap().values()) {
      result.append(String.format("<a href=\"round.xhtml?roundId=%d\">%d</a> ",
          round.getRoundId(), round.getRoundId()));
      result.append("<br/>");
    }

    return result.toString();
  }

  public String getStartTimes (Level level)
  {
    StringBuilder result = new StringBuilder();

    for (Round round : level.getRoundMap().values()) {
      result.append(Utils.dateToStringMedium(round.getStartTime()));
      result.append("<br/>");
    }

    return result.toString();
  }

  public String getEndTimes (Level level)
  {
    StringBuilder result = new StringBuilder();

    for (Round round : level.getRoundMap().values()) {
      Forecast forecast = MemStore.getForecast(round.getRoundId());

      if (forecast != null) {
        Date endDate = Utils.dateFromLong(forecast.getScheduleEnd());
        result.append(Utils.dateToStringMedium(endDate));
      }
      result.append("<br/>");
    }

    return result.toString();
  }

  public void forecast (Level level)
  {
    Map<Integer, Forecast> forecastMap = Forecaster.createForLevel(level);
    if (forecastMap == null) {
      Utils.growlMessage("Can't forecast for more than 500 games");
      return;
    }

    for (Round round : level.getRoundMap().values()) {
      Forecast forecast = forecastMap.get(round.getRoundId());
      forecast.writeSchedule(round.getRoundName());
      MemStore.setForecast(round.getRoundId(), forecast);
    }
  }

  public List<Level> getLevelList ()
  {
    return levelList;
  }
}
