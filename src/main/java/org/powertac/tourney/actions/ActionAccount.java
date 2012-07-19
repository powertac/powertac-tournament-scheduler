package org.powertac.tourney.actions;

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

import static org.powertac.tourney.services.Utils.log;

@ManagedBean
@RequestScoped
public class ActionAccount
{
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
    user.addBroker(getNewBrokerName(), getNewBrokerShortDescription());
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
  public String register (Broker b)
  {
    String tournamentName = b.getSelectedTourney();
    if (tournamentName == null || tournamentName.equals("")) {
      return null;
    }

    List<Tournament> allTournaments = Tournament.getTournamentList();
    Database db = new Database();
    try {
      db.startTrans();
      for (Tournament t: allTournaments) {
        if (!db.isRegistered(t.getTournamentId(), b.getBrokerId())
            && t.getTournamentName().equalsIgnoreCase(tournamentName)) {

          if (t.getNumberRegistered() < t.getMaxBrokers()) {
            log("Registering broker: {0} with tournament: {1}",
                b.getBrokerId(), t.getTournamentId());
            db.registerBroker(t.getTournamentId(), b.getBrokerId());

            // Only do this for single game, otherwise the scheduler handles
            // multigame tourneys
            if (t.typeEquals(Tournament.TYPE.SINGLE_GAME)) {
              for (Game g: t.getGames()) {
                if (g.getNumBrokersRegistered() < g.getMaxBrokers()) {
                  g.addBroker(b.getBrokerId());
                  log("Number registered: {0} of {1}",
                      g.getNumBrokersRegistered(), t.getMaxBrokers());
                }
              }
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

    return null;
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
