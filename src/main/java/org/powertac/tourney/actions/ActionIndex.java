/**
 * @author constantine
 *
 */

package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Game;
import org.powertac.tourney.services.TournamentProperties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionIndex
{
  private List<Game> notCompleteGamesList = new ArrayList<Game>();
  private List<Game> completeGamesList = new ArrayList<Game>();

  public ActionIndex ()
  {
    loadData();
  }

  private void loadData ()
  {
    notCompleteGamesList = Game.getNotCompleteGamesList();
    completeGamesList = Game.getCompleteGamesList();
  }

  public List<Game> getNotCompleteGamesList()
  {
    return notCompleteGamesList;
  }
  
  public List<Game> getCompleteGamesList()
  {
    return completeGamesList;
  }

  public String getLogUrl (Game g)
  {
    TournamentProperties properties = TournamentProperties.getProperties();
    String baseUrl = properties.getProperty("actionIndex.logUrl",
                                            "download?game=%d");
    return String.format(baseUrl, g.getGameId());
  }
}
