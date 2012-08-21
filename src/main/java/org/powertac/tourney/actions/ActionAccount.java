package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Database;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

@ManagedBean
@RequestScoped
public class ActionAccount
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String newBrokerName;
  private String newBrokerShortDescription;

  public ActionAccount ()
  {
  }

  public void addBroker ()
  {
    // Check if name and description not empty, and if name allowed
    if (namesEmpty(getNewBrokerName(), getNewBrokerShortDescription())) {
      return;
    }
    else if (nameExists(getNewBrokerName())) {
      return;
    }

    User user = User.getCurrentUser();
    user.addBroker(getNewBrokerName().trim(),
        getNewBrokerShortDescription().trim());
    newBrokerName = "";
    newBrokerShortDescription = "";
  }

  public List<Broker> getBrokers ()
  {
    User user = User.getCurrentUser();
    return user.getBrokers();
  }

  public void deleteBroker (Broker b)
  {
    User user = User.getCurrentUser();
    user.deleteBroker(b.getBrokerId());
  }

  public void editBroker (Broker b)
  {
    User user = User.getCurrentUser();
    user.setEdit(true);
    b.setEdit(true);
    b.setNewAuth(b.getBrokerAuthToken());
    b.setNewName(b.getBrokerName());
    b.setNewShort(b.getShortDescription());
  }

  public void saveBroker (Broker b)
  {
    // Check if name and description not empty, and if name allowed (if changed)
    if (namesEmpty(b.getNewName(), b.getNewShort())) {
      return;
    }
    else if (!b.getBrokerName().equals(b.getNewName())) {
      if (nameExists(b.getNewName())) {
        return;
      }
    }

    User user = User.getCurrentUser();
    user.setEdit(false);
    b.setEdit(false);
    b.setBrokerName(b.getNewName());
    b.setShortDescription(b.getNewShort());
    b.setBrokerAuthToken(b.getNewAuth());

    Database db = new Database();
    try {
      db.startTrans();
      db.updateBrokerByBrokerId(b.getBrokerId(), b.getBrokerName(),
                                b.getBrokerAuthToken(), b.getShortDescription());
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  public void cancelBroker (Broker b)
  {
    User user = User.getCurrentUser();
    user.setEdit(false);
    b.setEdit(false);
  }

  private boolean namesEmpty (String name, String description)
  {
    if (name.trim().isEmpty() || description.trim().isEmpty()) {
      String msg = "Broker requires a Name and a Description";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("accountForm", fm);
      return true;
    }
    return false;
  }

  private boolean nameExists (String name)
  {
    boolean exists = false;
    Database db = new Database();
    try {
      db.startTrans();
      exists = db.brokerNameExists(name);
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
    }

    if (exists) {
      String msg = "Broker Name taken, please select a new name";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("accountForm", fm);
    }

    return exists;
  }

  public List<Tournament> getAvailableTournaments (Broker b)
  {
    List<Tournament> availableTourneys = new Vector<Tournament>();

    if (b != null) {
      availableTourneys = b.getAvailableTournaments();
    }

    return availableTourneys;
  }

  // TODO This should be a Broker method
  public void register (Broker b)
  {
    String tournamentName = b.getSelectedTourney();
    if (tournamentName == null || tournamentName.equals("")) {
      return;
    }

    List<Tournament> allTournaments = Tournament.getTournamentList();
    Database db = new Database();
    try {
      db.startTrans();
      for (Tournament t: allTournaments) {
        if (!db.isRegistered(t.getTournamentId(), b.getBrokerId())
            && t.getTournamentName().equalsIgnoreCase(tournamentName)) {

          if (t.getMaxBrokers() == -1 ||
              t.getNumberRegistered() < t.getMaxBrokers()) {
            log.info(String.format("Registering broker: %s with tournament: %s",
                b.getBrokerId(), t.getTournamentId()));
            db.registerBroker(t.getTournamentId(), b.getBrokerId());

            if (t.typeEquals(Tournament.TYPE.MULTI_GAME)) {
              continue;
            }

            // Only for single game, the scheduler handles multigame tourneys
            for (Game g: t.getGames()) {
              db.addBrokerToGame(g.getGameId(), b.getBrokerId());
              log.info(String.format("Registering broker: %s with game: %s",
                  b.getBrokerId(), g.getGameId()));
            }
          }
        }
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  public String getNewBrokerShortDescription ()
  {
    return newBrokerShortDescription;
  }

  public void setNewBrokerShortDescription (String newBrokerShortDescription)
  {
    this.newBrokerShortDescription = newBrokerShortDescription;
  }

  public String getNewBrokerName ()
  {
    return newBrokerName;
  }

  public void setNewBrokerName (String newBrokerName)
  {
    this.newBrokerName = newBrokerName;
  }
}
