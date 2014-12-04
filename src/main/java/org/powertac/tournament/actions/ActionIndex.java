/**
 * @author constantine
 *
 */

package org.powertac.tournament.actions;

import org.powertac.tournament.beans.Game;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.TournamentProperties;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.List;


@ManagedBean
public class ActionIndex implements InitializingBean
{
  private List<Game> notCompleteGamesList;
  private List<Game> completeGamesList;

  private static boolean editing;
  private String content;

  public ActionIndex ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    notCompleteGamesList = Game.getNotCompleteGamesList();
    completeGamesList = Game.getCompleteGamesList();
  }

  public List<Game> getNotCompleteGamesList ()
  {
    return notCompleteGamesList;
  }

  public List<Game> getCompleteGamesList ()
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

  public String getBootUrl (Game g)
  {
    TournamentProperties properties = TournamentProperties.getProperties();
    String baseUrl = properties.getProperty("actionIndex.bootUrl",
        "download?boot=%d");

    return String.format(baseUrl, g.getGameId());
  }

  public void edit ()
  {
    if (editing) {
      if (!MemStore.setIndexContent(content)) {
        Utils.growlMessage("Failed to save to DB");
        return;
      }
    }
    editing = !editing;
  }

  public void cancel ()
  {
    editing = false;
  }

  public boolean isEditing ()
  {
    return editing;
  }

  public String getContent ()
  {
    return MemStore.getIndexContent();
  }

  public void setContent (String content)
  {
    this.content = content;
  }
}
