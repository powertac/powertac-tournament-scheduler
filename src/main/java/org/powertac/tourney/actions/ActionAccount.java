package org.powertac.tourney.actions;

import org.apache.commons.codec.digest.DigestUtils;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ManagedBean
@RequestScoped
public class ActionAccount
{
  private String brokerName;
  private String brokerShort;

  private List<Broker> brokers = new ArrayList<Broker>();

  public ActionAccount ()
  {
  }

  @SuppressWarnings("unchecked")
  public List<Broker> getBrokers ()
  {
    if (brokers.size() == 0) {
      User user = User.getCurrentUser();

      for (Broker broker: user.getBrokerMap().values()) {
        brokers.add(broker);
      }
    }
    Collections.sort(brokers, new Utils.AlphanumComparator());
    return brokers;
  }

  public void addBroker ()
  {
    // Check if name and description not empty, and if name allowed
    if (namesEmpty(brokerName, brokerShort, "accountForm2") ||
        nameExists(brokerName, "accountForm2") ||
        nameAllowed(brokerName, "accountForm2")) {
      return;
    }

    User user = User.getCurrentUser();
    if (user.isEditing() || !user.isLoggedIn()) {
      return;
    }

    String brokerAuth = DigestUtils.md5Hex(brokerName +
        (new Date()).toString() + Math.random());

    Broker broker = new Broker();
    broker.setBrokerAuth(brokerAuth);
    broker.setBrokerName(brokerName);
    broker.setShortDescription(brokerShort);
    broker.setUser(user);

    boolean added = broker.save();
    if (added) {
      brokerName = "";
      brokerShort = "";
      brokers = new ArrayList<Broker>();
      User.reloadUser(user);
    } else {
      String msg = "Error adding broker";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("accountForm2", fm);
    }
  }

  public void deleteBroker (Broker broker)
  {
    User user = User.getCurrentUser();
    if (user.isEditing() || !user.isLoggedIn()) {
      return;
    }

    brokers = new ArrayList<Broker>();
    boolean deleted = broker.delete();

    if (deleted) {
      User.reloadUser(user);
    } else {
      String msg = "Error deleting broker";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("accountForm1", fm);
    }
  }

  public void updateBroker(Broker broker)
  {
    // Check if name and description not empty, and if name allowed (if changed)
    if (namesEmpty(broker.getNewName(), broker.getNewShort(), "accountForm1")) {
      return;
    }
    if (nameAllowed(broker.getNewName(), "accountForm1")) {
      return;
    }
    else if (!broker.getBrokerName().equals(broker.getNewName())) {
      if (nameExists(broker.getNewName(), "accountForm1")) {
        return;
      }
    }

    User user = User.getCurrentUser();
    if (!user.isLoggedIn()) {
      return;
    }
    user.setEditing(false);

    broker.setEdit(false);
    broker.setBrokerName(broker.getNewName());
    broker.setShortDescription(broker.getNewShort());
    broker.setBrokerAuth(broker.getNewAuth());

    boolean saved = broker.update();
    if (!saved) {
      String msg = "Error saving broker";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("accountForm1", fm);
    }
  }

  public void editBroker (Broker broker)
  {
    User user = User.getCurrentUser();
    user.setEditing(true);

    broker.setEdit(true);
    broker.setNewAuth(broker.getBrokerAuth());
    broker.setNewName(broker.getBrokerName());
    broker.setNewShort(broker.getShortDescription());
  }

  public void cancelBroker (Broker broker)
  {
    User user = User.getCurrentUser();
    user.setEditing(false);
    broker.setEdit(false);
  }

  private boolean namesEmpty (String name, String description, String form)
  {
    if (name == null || description == null ||
        name.trim().isEmpty() || description.trim().isEmpty()) {
      String msg = "Broker requires a Name and a Description";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage(form, fm);
      return true;
    }
    return false;
  }

  private boolean nameExists (String brokerName, String form)
  {
    Broker broker = Broker.getBrokerByName(brokerName);
    if (broker != null) {
      String msg = "Brokername taken, please select a new name";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage(form, fm);
      return true;
    }
    return false;
  }

  // We can't allow commas, used in end-of-game message from server
  private boolean nameAllowed (String brokerName, String form)
  {
    Pattern ALPHANUMERIC = Pattern.compile("[A-Za-z0-9\\-\\_]+");
    Matcher m = ALPHANUMERIC.matcher(brokerName);

    if (!m.matches()) {
      String msg = "Brokername contains illegal characters, please select a "
          + "new name (only alphanumeric, '-' and '_' allowed)";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage(form, fm);
      return true;
    }
    return false;
  }

  public List<Tournament> getAvailableTournaments (Broker b)
  {
    return b.getAvailableTournaments();
  }

  public void register (Broker b)
  {
    if (!(b.getSelectedTourney() > 0)) {
      return;
    }

    boolean registered = b.register(b.getSelectedTourney());
    if (!registered) {
      String msg = "Error registering broker";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("accountForm0", fm);
    } else {
      brokers = new ArrayList<Broker>();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public String getBrokerShort () {
    return brokerShort;
  }
  public void setBrokerShort (String brokerShort) {
    this.brokerShort = brokerShort;
  }

  public String getBrokerName () {
    return brokerName;
  }
  public void setBrokerName (String brokerName) {
    this.brokerName = brokerName;
  }
  //</editor-fold>
}
