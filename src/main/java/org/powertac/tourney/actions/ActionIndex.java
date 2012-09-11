/**
 * @author constantine
 *
 */

package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Game;
import org.powertac.tourney.services.TournamentProperties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionIndex
{
  private String sortColumn = null;
  private boolean sortAscending = true;

  public List<Game> getNotCompleteGamesList()
  {
    return Game.getNotCompleteGamesList();
  }
  
  public List<Game> getCompleteGamesList()
  {
    return Game.getCompleteGamesList();
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
