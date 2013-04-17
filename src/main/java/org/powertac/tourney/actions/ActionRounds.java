package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.*;


@ManagedBean
@RequestScoped
public class ActionRounds
{
  private static Logger log = Logger.getLogger("TMLogger");

  private boolean disabled;
  private List<Broker> brokerList = new ArrayList<Broker>();
  private int slavesCount;

  private int roundId;
  private String roundName;
  private Round.TYPE type;
  private int maxBrokers;
  private int maxAgents;
  private int size1;
  private int size2;
  private int size3;
  private int multiplier1;
  private int multiplier2;
  private int multiplier3;
  private Date startTime;
  private Date dateFrom;
  private Date dateTo;
  private List<String> locations;
  private int selectedPom;
  private boolean closed;

  public ActionRounds ()
  {
    resetValues();
  }

  public List<Round.TYPE> getTypes ()
  {
    return Arrays.asList(Round.TYPE.values());
  }

  public List<Round> getRoundList ()
  {
    return Round.getNotCompleteRoundList();
  }

  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public List<Location> getLocationList ()
  {
    return Location.getLocationList();
  }

  public void saveRound ()
  {
    if (!inputsValidated()) {
      if (roundId != -1) {
        resetValues();
      }
      return;
    }

    if (roundId != -1) {
      saveEditedRound();
    } else {
      createRound();
    }
  }

  private void createRound ()
  {
    log.info("Creating " + type.toString() + " round");

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round = new Round();
      setValues(round);
      round.setStateToPending();
      session.save(round);

      log.info(String.format("Created %s round %s",
          type.toString(), round.getRoundId()));

      if (type == Round.TYPE.SINGLE_GAME) {
        Game game = Game.createGame(round, roundName);
        session.save(game);
        log.info("Created game " + game.getGameId());
      }

      transaction.commit();
    } catch (ConstraintViolationException ignored) {
      transaction.rollback();
      message(2, "The round name already exists");
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error creating round");
    } finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  public void loadRound (Round round)
  {
    roundId = round.getRoundId();
    roundName = round.getRoundName();
    type = round.getType();
    maxBrokers = round.getMaxBrokers();
    maxAgents = round.getMaxAgents();
    size1 = round.getSize1();
    size2 = round.getSize2();
    size3 = round.getSize3();
    multiplier1 = round.getMultiplier1();
    multiplier2 = round.getMultiplier2();
    multiplier3 = round.getMultiplier3();
    startTime = round.getStartTime();
    dateFrom = round.getDateFrom();
    dateTo = round.getDateTo();
    locations = round.getLocationsList();
    selectedPom = round.getPomId();
    closed = round.isClosed();

    // Once scheduled, params can't change (type can never change)
    if (round.getSize() > 0) {
      disabled = true;
    }
  }

  public void saveEditedRound ()
  {
    log.info("Saving round " + roundId);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round = (Round) session.get(Round.class, roundId);
      setValues(round);
      session.saveOrUpdate(round);
      transaction.commit();
    } catch (ConstraintViolationException ignored) {
      transaction.rollback();
      message(2, "The round name already exists");
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error saving round");
    } finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  public void removeRound (Round round)
  {
    if (!round.getRoundName().toLowerCase().contains("test")) {
      log.info("Someone tried to remove a non-test Round!");
      message(0, "Nice try, hacker!");
      return;
    }

    String msg = round.delete();
    if (!msg.isEmpty()) {
      log.info(String.format("Something went wrong with removing round "
          + "%s\n%s", round.getRoundName(), msg));
      message(0, msg);
    }
  }

  public void setValues (Round round)
  {
    String allLocations = "";
    for (String s: locations) {
      allLocations += s + ",";
    }

    Integer[] gameTypes = {Math.max(1, size1), Math.max(1, size2),
        Math.max(1, size3)};
    Integer[] multipliers = {Math.max(0, multiplier1), Math.max(0, multiplier2),
        Math.max(0, multiplier3)};
    Arrays.sort(gameTypes, Collections.reverseOrder());
    maxBrokers = Math.max(maxBrokers, round.getBrokerMap().size());

    if (round.getSize() < 1) {
      round.setRoundName(roundName);
      if (type != null) {
        round.setType(type);
      }
      round.setMaxBrokers(maxBrokers);
      round.setMaxAgents(round.isMulti() ? maxAgents : 0);
      round.setSize1(round.isMulti() ? gameTypes[0] : 0);
      round.setSize2(round.isMulti() ? gameTypes[1] : 0);
      round.setSize3(round.isMulti() ? gameTypes[2] : 0);
      round.setMultiplier1(round.isMulti() ? multipliers[0] : 0);
      round.setMultiplier2(round.isMulti() ? multipliers[1] : 0);
      round.setMultiplier3(round.isMulti() ? multipliers[2] : 0);
      round.setStartTime(startTime);
      round.setDateFrom(dateFrom);
      round.setDateTo(dateTo);
      round.setLocations(allLocations);
      round.setPomId(selectedPom);
    }
    round.setClosed(closed);
  }

  public void resetValues ()
  {
    roundId = -1;
    roundName = "";
    type = Round.TYPE.SINGLE_GAME;
    maxBrokers = 0;
    maxAgents = 2;
    size1 = 8;
    size2 = 4;
    size3 = 2;
    multiplier1 = 1;
    multiplier2 = 1;
    multiplier3 = 1;
    startTime = Utils.offsetDate(2);

    if (getLocationList().size() == 1) {
      Location location = getLocationList().get(0);
      dateFrom = location.getDateFrom();
      dateTo = location.getDateTo();
      locations = Arrays.asList(location.getLocation());
    }
    else {
      dateFrom = new Date();
      dateTo = new Date();
      dateTo.setTime(0);
      for (Location loc: getLocationList()) {
        if (loc.getDateFrom().before(dateFrom)) {
          dateFrom = loc.getDateFrom();
        }
        if (loc.getDateTo().after(dateTo)) {
          dateTo = loc.getDateTo();
        }
      }
    }

    selectedPom = 0;
    closed = false;

    disabled = false;
    brokerList = Broker.getBrokerList();
    slavesCount = Machine.getMachineList().size();
  }

  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public List<Round> getAvailableRounds (Broker b)
  {
    return b.getAvailableRounds(false);
  }

  public List<Round> getRegisteredRounds (Broker b)
  {
    return b.getRegisteredRounds();
  }

  public void register (Broker b)
  {
    if (!(b.getSelectedRoundRegister() > 0)) {
      return;
    }

    boolean registered = b.register(b.getSelectedRoundRegister());
    if (!registered) {
      message(1, "Error registering broker");
    } else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public void unregister (Broker b)
  {
    if (!(b.getSelectedRoundUnregister() > 0)) {
      return;
    }

    boolean registered = b.unregister(b.getSelectedRoundUnregister());
    if (!registered) {
      message(1, "Error unregistering broker");
    } else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public boolean allowEdit (Round round)
  {
    return roundId == -1 && round.isPending();
  }

  private boolean inputsValidated ()
  {
    List<String> messages = new ArrayList<String>();

    if (roundName.trim().isEmpty()) {
      messages.add("The round name cannot be empty");
    }

    if ((locations.size() < 1) && (roundId == -1 || selectedPom != 0)) {
      messages.add("Choose at least one location");
    }

    if (dateFrom.after(dateTo)) {
      messages.add("End date should be after start date");
    }

    for (String msg: messages) {
      message(2, msg);
    }

    return messages.size() == 0;
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("runningRounds", fm);
    } else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("roundRegistered", fm);
    } else if (field == 2) {
      FacesContext.getCurrentInstance().addMessage("saveRound", fm);
    }
  }

  //<editor-fold desc="Setters and getters">
  public int getRoundId ()
  {
    return roundId;
  }
  public void setRoundId (int roundId)
  {
    this.roundId = roundId;
  }

  public String getRoundName ()
  {
    return roundName;
  }
  public void setRoundName (String roundName)
  {
    this.roundName = roundName;
  }

  public Round.TYPE getType ()
  {
    return type;
  }
  public void setType (Round.TYPE type)
  {
    this.type = type;
  }

  public int getMaxBrokers ()
  {
    return maxBrokers;
  }
  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  public int getMaxAgents ()
  {
    return maxAgents;
  }
  public void setMaxAgents (int maxAgents)
  {
    this.maxAgents = maxAgents;
  }

  public int getSize1 ()
  {
    return size1;
  }
  public void setSize1 (int size1)
  {
    this.size1 = size1;
  }

  public int getSize2 ()
  {
    return size2;
  }
  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  public int getSize3 ()
  {
    return size3;
  }
  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  public int getMultiplier1 ()
  {
    return multiplier1;
  }
  public void setMultiplier1 (int multiplier1)
  {
    this.multiplier1 = multiplier1;
  }

  public int getMultiplier2 ()
  {
    return multiplier2;
  }
  public void setMultiplier2 (int multiplier2)
  {
    this.multiplier2 = multiplier2;
  }

  public int getMultiplier3 ()
  {
    return multiplier3;
  }
  public void setMultiplier3 (int multiplier3)
  {
    this.multiplier3 = multiplier3;
  }

  public Date getStartTime ()
  {
    return startTime;
  }
  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  public Date getDateFrom ()
  {
    return dateFrom;
  }
  public void setDateFrom (Date dateFrom)
  {
    this.dateFrom = dateFrom;
  }

  public Date getDateTo ()
  {
    return dateTo;
  }
  public void setDateTo (Date dateTo)
  {
    this.dateTo = dateTo;
  }

  public List<String> getLocations ()
  {
    return locations;
  }
  public void setLocations (List<String> locations)
  {
    this.locations = locations;
  }

  public int getSelectedPom ()
  {
    return selectedPom;
  }
  public void setSelectedPom (int selectedPom)
  {
    this.selectedPom = selectedPom;
  }

  public boolean isClosed ()
  {
    return closed;
  }
  public void setClosed (boolean closed)
  {
    this.closed = closed;
  }

  public boolean isDisabled ()
  {
    return disabled;
  }

  public int getSlavesCount ()
  {
    return slavesCount;
  }
  //</editor-fold>
}
