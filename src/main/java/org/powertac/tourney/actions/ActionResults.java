package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Agent;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.services.Database;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@RequestScoped
public class ActionResults
{
  private String sortColumn = null;
  private boolean sortAscending = true;

  public ActionResults()
  {
  }

  public List<Game> getGameList (int tourneyId)
  {
    List<Game> games = new ArrayList<Game>();

    Database db = new Database();
    try {
      db.startTrans();

      games = db.getGamesInTourney(tourneyId);

      db.commitTrans();
    }
    catch (Exception e) {
      e.printStackTrace();
      db.abortTrans();
    }

    return games;
  }

  public List<Map.Entry<String, Double>> getResultMap(int tourneyId)
  {
    Map<String, Double> temp = new HashMap<String, Double>();

    for (Game g: getGameList(tourneyId)) {
      for (Broker b: g.getBrokersInGame()) {
        Agent a = b.getAgent(g.getGameId());

        if (temp.get(b.getBrokerName()) != null) {
          temp.put(b.getBrokerName(), temp.get(b.getBrokerName() + a.getBalance()));
        } else {
          temp.put(b.getBrokerName(), a.getBalance());
        }
      }
    }

    List<Map.Entry<String, Double>> results =
        new ArrayList<Map.Entry<String, Double>>();

    for (Map.Entry<String, Double> entry: temp.entrySet()) {
        results.add(entry);
    }

    return results;
  }

  //<editor-fold desc="Setters and Getters">
  public String getSortColumn ()
  {
    return sortColumn;
  }
  public void setSortColumn (String sortColumn)
  {
    this.sortColumn = sortColumn;
  }

  public boolean isSortAscending ()
  {
    return sortAscending;
  }
  public void setSortAscending (boolean sortAscending)
  {
    this.sortAscending = sortAscending;
  }
  //</editor-fold>
}
