/**
 * @author constantine
 *
 */

package org.powertac.tournament.actions;

import org.powertac.tournament.beans.Game;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.TournamentProperties;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionIndex
{
  private List<Game> notCompleteGamesList;
  private List<Game> completeGamesList;

  private static boolean editing;
  private String content;

  public ActionIndex ()
  {
    loadData();
  }

  private void loadData ()
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

  public void edit ()
  {
    if (editing) {
      if (!MemStore.setIndexContent(content)) {
        message(0, "Error saving to DB");
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

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("contentForm", fm);
    }
  }
}
