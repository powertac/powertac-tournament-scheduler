/**
 * @author constantine
 *
 */

package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.TournamentProperties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@RequestScoped
public class ActionIndex
{
  private String sortColumn = null;
  private boolean sortAscending = true;

  public List<Game> getGameList ()
  {
    return Game.getGameList();
  }
  
  public List<Game> getGameCompleteList ()
  {
    List<Game> games = new ArrayList<Game>();

    Database db = new Database();
    try {
      db.startTrans();
      games = db.getCompleteGames();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return games;
  }

  public String getBrokersInGame (Game g)
  {
    String result = "";

    if (g == null) {
        return result;
    }

    List<Broker> brokersRegistered = new ArrayList<Broker>();

    Database db = new Database();

    try {
      db.startTrans();
      brokersRegistered = db.getBrokersInGame(g.getGameId());
      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }

    for (Broker b: brokersRegistered) {
      result += b.getBrokerName() + "\n";
    }

    return result;
  }

  public String getTournamentNameByGame (Game g)
  {
    String result = "";

    if (g == null) {
      return result;
    }

    Database db = new Database();

    try {
      db.startTrans();
      result = db.getTournamentByGameId(g.getGameId()).getTournamentName();
      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return result;
  }

  public String getLogUrl (Game g)
  {
    TournamentProperties properties = TournamentProperties.getProperties();
    String baseUrl = properties.getProperty("actionIndex.logUrl",
                                            "download?game=%d");
    return String.format(baseUrl, g.getGameId());
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
