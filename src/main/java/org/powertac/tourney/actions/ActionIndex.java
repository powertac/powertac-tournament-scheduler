/**
 * @author constantine
 *
 */

package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Game;
import org.powertac.tourney.services.MemStore;
import org.powertac.tourney.services.TournamentProperties;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionIndex {
  private List<Game> notCompleteGamesList = new ArrayList<Game>();
  private List<Game> completeGamesList = new ArrayList<Game>();

  private static boolean editing;
  private String content;

  public ActionIndex() {
    loadData();
  }

  private void loadData() {
    notCompleteGamesList = Game.getNotCompleteGamesList();
    completeGamesList = Game.getCompleteGamesList();
  }

  public List<Game> getNotCompleteGamesList() {
    return notCompleteGamesList;
  }

  public List<Game> getCompleteGamesList() {
    return completeGamesList;
  }

  public String getLogUrl(Game g) {
    TournamentProperties properties = TournamentProperties.getProperties();
    String baseUrl = properties.getProperty("actionIndex.logUrl",
        "download?game=%d");

    return String.format(baseUrl, g.getGameId());
  }

  public void edit() {
    if (editing) {
      if (!MemStore.setIndexContent(content)) {
        String msg = "Error saving to DB";
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
        FacesContext.getCurrentInstance().addMessage("contentForm", fm);
        return;
      }
    }
    editing = !editing;
  }

  public void cancel() {
    editing = false;
  }

  public boolean isEditing() {
    return editing;
  }

  public String getContent() {
    return MemStore.getIndexContent();
  }

  public void setContent(String content) {
    this.content = content;
  }
}
