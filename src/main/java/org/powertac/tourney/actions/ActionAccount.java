package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.TournamentProperties;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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

  public String getNewBrokerName ()
  {
    return newBrokerName;
  }

  public void setNewBrokerName (String newBrokerName)
  {
    this.newBrokerName = newBrokerName;
  }

  public String addBroker ()
  {
    User user =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());
    if (getNewBrokerName().equalsIgnoreCase("")
          || getNewBrokerShortDescription().equalsIgnoreCase("")) {
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Broker requires a Name and an AuthToken", null);
      FacesContext.getCurrentInstance().addMessage("accountForm", fm);
      return "Account";
    }
    
    // Check if user is null?
    user.addBroker(getNewBrokerName(), getNewBrokerShortDescription());

    return "Account";
  }

  public List<Broker> getBrokers ()
  {
    User user =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());
    return user.getBrokers();
  }

  public void deleteBroker (Broker b)
  {
    User user =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());
    user.deleteBroker(b.getBrokerId());
  }

  public void editBroker (Broker b)
  {
    User user =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());
    user.setEdit(true);
    b.setEdit(true);
    b.setNewAuth(b.getBrokerAuthToken());
    b.setNewName(b.getBrokerName());
    b.setNewShort(b.getShortDescription());
  }

  public void saveBroker (Broker b)
  {
    User user =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());
    
    if(b.getNewName().equalsIgnoreCase("") || b.getBrokerAuthToken().equalsIgnoreCase("")){
      FacesContext.getCurrentInstance()
      .addMessage("accountForm",
                  new FacesMessage(FacesMessage.SEVERITY_INFO,
                                   "Broker requires a Name and an AuthToken", null));
      return;
    }
    
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
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void cancelBroker (Broker b)
  {
    User user =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());
    user.setEdit(false);
    b.setEdit(false);
  }

  public List<Tournament> getAvailableTournaments (Broker b)
  {
    List<Tournament> allTournaments = new ArrayList<Tournament>();
    Vector<Tournament> availableTourneys = new Vector<Tournament>();

    if (b == null) {
      return availableTourneys;
    }

    Database db = new Database();
    try {
      db.startTrans();
      allTournaments = db.getTournaments(Tournament.STATE.pending);
      allTournaments.addAll(db.getTournaments(Tournament.STATE.in_progress));
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    TournamentProperties properties = TournamentProperties.getProperties();
    long loginDeadline = Integer.parseInt(
        properties.getProperty("loginDeadline", "3600000"));
    long nowStamp = new Date().getTime();

    for (Tournament t: allTournaments) {
      try {
        long startStamp = t.getStartTime().getTime();

        if (!db.isRegistered(t.getTournamentId(), b.getBrokerId())
            && t.getNumberRegistered() < t.getMaxBrokers()
            && (startStamp-nowStamp) > loginDeadline ) {
          availableTourneys.add(t);
        }
        else if (t.getNumberRegistered() >= t.getMaxBrokers()) {
          log("Cannot register for {0}: maxBrokers", t.getTournamentName());
        }
        else if ((startStamp-nowStamp) <= loginDeadline ) {
          log("Cannot register for {0}: too late", t.getTournamentName());
        }
      }
      catch (SQLException e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    db.commitTrans();

    return availableTourneys;
  }

  public String register (Broker b)
  {
    String tournamentName = b.getSelectedTourney();
    if (tournamentName == null || tournamentName.equals("")) {
      return null;
    }

    Database db = new Database();
    List<Tournament> allTournaments;

    try {
      db.startTrans();
      allTournaments = db.getTournaments(Tournament.STATE.pending);
      allTournaments.addAll(db.getTournaments(Tournament.STATE.in_progress));
      for (Tournament t: allTournaments) {
        if (!db.isRegistered(t.getTournamentId(), b.getBrokerId())
            && t.getTournamentName().equalsIgnoreCase(tournamentName)) {

          if (t.getNumberRegistered() < t.getMaxBrokers()) {
            log("Registering broker: {0} with tournament: {1}",
                b.getBrokerId(), t.getTournamentId());
            db.registerBroker(t.getTournamentId(), b.getBrokerId());

            // Only do this for single game, otherwise the scheduler handles multigame tourneys
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
}
