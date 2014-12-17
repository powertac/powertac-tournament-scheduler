package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.Machine;
import org.powertac.tournament.beans.Pom;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.Forecaster;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Scheduler;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.powertac.tournament.services.Forecaster.Forecast;


@ManagedBean
public class ActionRounds implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  private List<Location> locationList;
  private List<Broker> brokerList;
  private List<Round> roundList;
  private List<Pom> pomList;

  private int slavesCount;

  private int roundId;
  private String roundName;
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
  private boolean enableChangeAllRounds;
  private boolean changeAllRoundsInLevel;

  // Forecast stuff
  private Forecast forecast;
  private Boolean parallelRound;
  private String paramString;
  private String dateString;
  private String nameString;

  public ActionRounds ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    resetValues();
  }

  public boolean allowEdit (Round round)
  {
    return roundId == -1 && round.isPending();
  }

  public boolean allowDelete (Round round)
  {
    if (!round.isPending()) {
      return false;
    }

    // Never allow last round to be removed
    if (round.getLevel().getNofRounds() <= 1) {
      return false;
    }

    if (!round.getRoundName().toLowerCase().contains("test")) {
      return false;
    }

    return true;
  }

  public void loadRound (Round round)
  {
    roundId = round.getRoundId();
    roundName = round.getRoundName();
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
    enableChangeAllRounds = enableChangeAllRounds(round);
    changeAllRoundsInLevel = enableChangeAllRounds(round);

    forecast = MemStore.getForecast(roundId);
    parallelRound = round.getLevel().getRoundMap().size() > 1;
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
    }
  }

  public boolean enableChangeAllRounds (Round round)
  {
    // This function checks whether it should be possible for the user to change all rounds at the same time
    // This can only be possible when there are more than one rounds in the level.
    Level level = round.getLevel();
    if (level.getRoundMap().size() > 1) {
      return true;
    }
    return false;
  }

  public void saveEditedRound ()
  {
    log.info("Saving round " + roundId);

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round = (Round) session.get(Round.class, roundId);
      // if we want all rounds from this level to be the same
      if (changeAllRoundsInLevel) {
        Level level = round.getLevel();
        for (Round round2 : level.getRoundMap().values()) {
          setValues(round2, false);
          session.saveOrUpdate(round2);
        }
      }
      else {
        setValues(round, true);
        session.saveOrUpdate(round);
      }
      transaction.commit();
    }
    catch (ConstraintViolationException ignored) {
      transaction.rollback();
      Utils.growlMessage("The round name already exists.");
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error saving round");
    }
    finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  public void deleteRound (Round round)
  {
    if (!round.getRoundName().toLowerCase().contains("test")) {
      log.info("Someone tried to remove a non-test Round!");
      Utils.growlMessage("You suck!", "Nice try, hacker!");
      return;
    }

    String msg = round.delete();
    if (!msg.isEmpty()) {
      log.info(String.format("Something went wrong with removing round "
          + "%s\n%s", round.getRoundName(), msg));
      Utils.growlMessage(msg);
    }
    else {
      log.info("Removed round : " + round.getRoundName());
      Utils.growlMessage("Notice", "Removed round : " + round.getRoundName());
    }
  }

  public void startNow (Round round)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      round.setStartTime(Utils.offsetDate());
      session.update(round);
      session.flush();

      String msg = "Setting round: " + round.getRoundId() + " to start now";
      log.info(msg);
      Utils.growlMessage("Notice", msg);

      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      Utils.growlMessage("Failed to start now : " + round.getRoundId());
    }
    session.close();

    // A round might already be loaded
    Scheduler.getScheduler().reloadRounds();
  }

  public void setValues (Round round, boolean changeSingleRound)
  {
    String allLocations = "";
    for (String s : locations) {
      allLocations += s + ",";
    }

    Integer[] gameTypes = {Math.max(1, size1),
        Math.max(1, size2),
        Math.max(1, size3)};
    Integer[] multipliers = {Math.max(0, multiplier1),
        Math.max(0, multiplier2),
        Math.max(0, multiplier3)};

    // Sort biggest games first, keep paired with multiplier
    sortGames(gameTypes, multipliers);

    maxBrokers = Math.max(maxBrokers, round.getBrokerMap().size());

    if (round.getSize() < 1) {
      if (changeSingleRound) {
        round.setRoundName(roundName);
      }
      round.setMaxBrokers(maxBrokers);
      round.setSize1(gameTypes[0]);
      round.setSize2(gameTypes[1]);
      round.setSize3(gameTypes[2]);
      round.setMultiplier1(multipliers[0]);
      round.setMultiplier2(multipliers[1]);
      round.setMultiplier3(multipliers[2]);
      round.setStartTime(startTime);
      round.setDateFrom(dateFrom);
      round.setDateTo(dateTo);
      round.setLocations(allLocations);
      round.setPomId(selectedPom);
    }
  }

  public void sortGames (Integer[] gameTypes, Integer[] multipliers)
  {
    // this function assumes both arrays have size 3
    if (gameTypes[0] < gameTypes[1]) {
      swapValues(gameTypes, multipliers, 0, 1, 3);
    }
    if (gameTypes[1] < gameTypes[2]) {
      swapValues(gameTypes, multipliers, 1, 2, 3);
    }
    if (gameTypes[0] < gameTypes[1]) {
      swapValues(gameTypes, multipliers, 0, 1, 3);
    }
  }

  public void swapValues (Integer[] array1, Integer[] array2, int index1, int index2, int n)
  {
    if (index1 == index2) {
      return;
    }
    if (0 > index1 || index1 >= n) {
      return;
    }
    if (0 > index2 || index2 >= n) {
      return;
    }
    int temp = array1[index1];
    array1[index1] = array1[index2];
    array1[index2] = temp;
    temp = array2[index1];
    array2[index1] = array2[index2];
    array2[index2] = temp;
  }

  public void resetValues ()
  {
    locationList = Location.getLocationList();
    brokerList = Broker.getBrokerList();
    roundList = Round.getNotCompleteRoundList();
    pomList = Pom.getPomList();
    slavesCount = Machine.getMachineList().size();

    roundId = -1;
    roundName = "";
    maxBrokers = 0;
    maxAgents = 2;
    size1 = 8;
    size2 = 4;
    size3 = 2;
    multiplier1 = 1;
    multiplier2 = 1;
    multiplier3 = 1;
    startTime = Utils.offsetDate(2);

    if (locationList.size() == 1) {
      Location location = getLocationList().get(0);
      dateFrom = location.getDateFrom();
      dateTo = location.getDateTo();
      locations = Arrays.asList(location.getLocation());
    }
    else {
      dateFrom = new Date();
      dateTo = new Date();
      for (Location loc : getLocationList()) {
        if (loc.getDateFrom().before(dateFrom)) {
          dateFrom = loc.getDateFrom();
        }
        if (loc.getDateTo().after(dateTo)) {
          dateTo = loc.getDateTo();
        }
      }
    }

    selectedPom = 0;
    enableChangeAllRounds = true;
    changeAllRoundsInLevel = false;

    forecast = null;
    parallelRound = null;
  }

  public void register (Broker b)
  {
    if (!(b.getRegisterRoundId() > 0)) {
      return;
    }

    boolean registered = b.registerForRound(b.getRegisterRoundId());
    if (!registered) {
      Utils.growlMessage("Failed to register broker.");
    }
    else {
      User user = User.getCurrentUser();
      User.reloadUser(user);
      roundList = Round.getNotCompleteRoundList();
      brokerList = Broker.getBrokerList();
    }
  }

  public void unregister (Broker b)
  {
    if (!(b.getUnregisterRoundId() > 0)) {
      return;
    }

    boolean registered = b.unregisterFromRound(b.getUnregisterRoundId());
    if (!registered) {
      Utils.growlMessage("Failed to unregister broker.");
    }
    else {
      User user = User.getCurrentUser();
      User.reloadUser(user);
      roundList = Round.getNotCompleteRoundList();
      brokerList = Broker.getBrokerList();
    }
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

    for (String msg : messages) {
      Utils.growlMessage(msg);
    }

    return messages.size() == 0;
  }

  //<editor-fold desc="Collections">
  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public List<Round> getRoundList ()
  {
    return roundList;
  }

  public List<Pom> getPomList ()
  {
    return pomList;
  }

  public List<Location> getLocationList ()
  {
    return locationList;
  }

  public List<Round> getAvailableRounds (Broker broker)
  {
    return broker.getAvailableRounds(roundList);
  }
  //</editor-fold>

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

  public int getSlavesCount ()
  {
    return slavesCount;
  }

  public boolean getChangeAllRoundsInLevel ()
  {
    return changeAllRoundsInLevel;
  }

  public void setChangeAllRoundsInLevel (boolean changeRounds)
  {
    this.changeAllRoundsInLevel = changeRounds;
  }

  public boolean getEnableChangeAllRounds ()
  {
    return enableChangeAllRounds;
  }

  public void setEnableChangeAllRounds (boolean enableChangeRounds)
  {
    this.enableChangeAllRounds = enableChangeRounds;
  }
  //</editor-fold>

  //<editor-fold desc="Forecaster">
  public String getParamString ()
  {
    return "";
  }

  public void setParamString (String paramString)
  {
    this.paramString = paramString;
  }

  public String getDateString ()
  {
    return "";
  }

  public void setDateString (String dateString)
  {
    this.dateString = dateString;
  }

  public String getNameString ()
  {
    return nameString;
  }

  public void setNameString (String nameString)
  {
    this.nameString = nameString;
  }

  public Boolean isParallelRound ()
  {
    return parallelRound;
  }

  public String getForecastString ()
  {
    try {
      if (forecast == null && dateString != null &&
          paramString != null && nameString != null) {

        forecast = Forecaster.createForRound(
            roundId, paramString, dateString, nameString);

        if (forecast == null) {
          return "Can't forecast for more than 500 games";
        }

        // Write to MemStore and ActionTimeline
        MemStore.setForecast(roundId, forecast);
        ActionTimeline.setForecast(forecast, "Round " + nameString);
      }

      if (forecast != null) {
        return forecast.getForecastString();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return "Creating forecast failed";
    }

    if (parallelRound == null || parallelRound) {
      return "";
    }
    else {
      return "<br/><br/>";
    }
  }
  //</editor-fold>
}
