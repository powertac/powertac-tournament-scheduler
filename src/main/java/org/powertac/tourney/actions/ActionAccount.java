package org.powertac.tourney.actions;

import org.apache.commons.codec.digest.DigestUtils;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Round;
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
  private User user = User.getCurrentUser();
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
    if (namesEmpty(brokerName, brokerShort, 2) ||
        nameExists(brokerName, 2) ||
        nameAllowed(brokerName, 2)) {
      return;
    }

    if (user.isEditingBroker() || !user.isLoggedIn()) {
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
      message(2, "Error adding broker");
    }
  }

  public void deleteBroker (Broker broker)
  {
    User user = User.getCurrentUser();
    if (user.isEditingBroker() || !user.isLoggedIn()) {
      return;
    }

    brokers = new ArrayList<Broker>();
    boolean deleted = broker.delete();

    if (deleted) {
      User.reloadUser(user);
    } else {
      message(1, "Error deleting broker");
    }
  }

  public void updateBroker (Broker broker)
  {
    // Check if name and description not empty, and if name allowed (if changed)
    if (namesEmpty(broker.getNewName(), broker.getNewShort(), 1)) {
      return;
    }
    if (nameAllowed(broker.getNewName(), 1)) {
      return;
    } else if (!broker.getBrokerName().equals(broker.getNewName())) {
      if (nameExists(broker.getNewName(), 1)) {
        return;
      }
    }

    User user = User.getCurrentUser();
    if (!user.isLoggedIn()) {
      return;
    }
    user.setEditingBroker(false);

    broker.setEdit(false);
    String orgName = broker.getBrokerName();
    String orgShort = broker.getShortDescription();
    String orgAuth = broker.getBrokerAuth();
    broker.setBrokerName(broker.getNewName());
    broker.setShortDescription(broker.getNewShort());
    broker.setBrokerAuth(broker.getNewAuth());

    String errorMessage = broker.update();
    if (errorMessage != null) {
      message(1, errorMessage);
      broker.setBrokerName(orgName);
      broker.setShortDescription(orgShort);
      broker.setBrokerAuth(orgAuth);
    }
  }

  public void editBroker (Broker broker)
  {
    User user = User.getCurrentUser();
    user.setEditingBroker(true);

    broker.setEdit(true);
    broker.setNewAuth(broker.getBrokerAuth());
    broker.setNewName(broker.getBrokerName());
    broker.setNewShort(broker.getShortDescription());
  }

  public void cancelBroker (Broker broker)
  {
    User user = User.getCurrentUser();
    user.setEditingBroker(false);
    broker.setEdit(false);
  }

  private boolean namesEmpty (String name, String description, int field)
  {
    if (name == null || description == null ||
        name.trim().isEmpty() || description.trim().isEmpty()) {
      message(field, "Broker requires a Name and a Description");
      return true;
    }
    return false;
  }

  private boolean nameExists (String brokerName, int field)
  {
    Broker broker = Broker.getBrokerByName(brokerName);
    if (broker != null) {
      message(field, "Brokername taken, please select a new name");
      return true;
    }
    return false;
  }

  // We can't allow commas, used in end-of-game message from server
  private boolean nameAllowed (String brokerName, int field)
  {
    Pattern ALPHANUMERIC = Pattern.compile("[A-Za-z0-9\\-\\_]+");
    Matcher m = ALPHANUMERIC.matcher(brokerName);

    if (!m.matches()) {
      message(field, "Brokername contains illegal characters, please select a "
          + "new name (only alphanumeric, '-' and '_' allowed)");
      return true;
    }
    return false;
  }

  public List<Round> getAvailableRounds (Broker b)
  {
    return b.getAvailableRounds(true);
  }

  public void register (Broker b)
  {
    if (!(b.getSelectedRoundRegister() > 0)) {
      return;
    }

    boolean registered = b.register(b.getSelectedRoundRegister());
    if (!registered) {
      message(0, "Error registering broker");
    } else {
      brokers = new ArrayList<Broker>();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public void editUserDetails ()
  {
    user.setEditingDetails(true);
  }

  public void saveUserDetails ()
  {
    user.save();
    user.setEditingDetails(false);
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("accountForm0", fm);
    } else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("accountForm1", fm);
    } else if (field == 2) {
      FacesContext.getCurrentInstance().addMessage("accountForm2", fm);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public String getBrokerShort ()
  {
    return brokerShort;
  }

  public void setBrokerShort (String brokerShort)
  {
    this.brokerShort = brokerShort;
  }

  public String getBrokerName ()
  {
    return brokerName;
  }

  public void setBrokerName (String brokerName)
  {
    this.brokerName = brokerName;
  }

  public User getUser ()
  {
    return user;
  }

  public void setUser (User user)
  {
    this.user = user;
  }
  //</editor-fold>
}
