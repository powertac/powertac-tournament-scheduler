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
}
