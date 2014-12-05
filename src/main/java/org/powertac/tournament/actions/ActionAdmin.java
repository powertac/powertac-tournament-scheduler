package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.Machine;
import org.powertac.tournament.beans.Pom;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.CheckWeatherServer;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Scheduler;
import org.powertac.tournament.services.SpringApplicationContext;
import org.powertac.tournament.services.TournamentProperties;
import org.powertac.tournament.services.Upload;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


@ManagedBean
public class ActionAdmin implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  private TournamentProperties properties = TournamentProperties.getProperties();

  private List<Integer> selectedRounds;

  private int locationId = -1;
  private String locationName = "";
  private int locationTimezone = 0;
  private Date locationStartTime = null;
  private Date locationEndTime = null;

  private UploadedFile uploadedPom;
  private String pomName;

  private int machineId = -1;
  private String machineName = "";
  private String machineUrl = "";
  private String machineViz = "";

  private List<Round> availableRounds;
  private List<Location> locationList;
  private List<Location> possibleLocations;
  private List<Pom> pomList;
  private List<Machine> machineList;
  private List<User> userList;

  public ActionAdmin ()
  {
  }

  @SuppressWarnings("unchecked")
  public void afterPropertiesSet () throws Exception
  {
    availableRounds = new ArrayList<Round>();
    for (Round round : Round.getNotCompleteRoundList()) {
      availableRounds.add(round);
    }
    Collections.sort(availableRounds, new Utils.AlphanumComparator());
    // If only one round, select it for the user
    if (availableRounds.size() == 1) {
      selectedRounds = new ArrayList<Integer>();
      selectedRounds.add(availableRounds.get(0).getRoundId());
    }

    locationList = Location.getLocationList();

    possibleLocations = MemStore.getAvailableLocations();
    for (Location location : getLocationList()) {
      Iterator<Location> iter = possibleLocations.iterator();
      while (iter.hasNext()) {
        if (iter.next().getLocation().equals(location.getLocation())) {
          iter.remove();
        }
      }
    }
    MemStore.setAvailableLocations(possibleLocations);

    pomList = Pom.getPomList();
    machineList = Machine.getMachineList();
    userList = User.getUserList();
  }

  //<editor-fold desc="Header stuff">
  public void restartScheduler ()
  {
    log.info("Restarting Scheduler");
    Scheduler scheduler = Scheduler.getScheduler();
    if (!scheduler.restartScheduler()) {
      log.info("Not restarting Scheduler, too close");
    }
  }

  /* selectedRounds is a list of Integers that are the IDs of the rounds selected
   * by the user in the 'Admin'-form of the tournament scheduler.
   * This function gives this list to the scheduler to make sure that this will
   * be the list of running rounds.
   */
  public void loadRounds ()
  {
    for (Integer roundId : selectedRounds) {
      log.info("Loading Round " + roundId);
    }
    log.info("End of list of rounds that are loaded");

    Scheduler scheduler = Scheduler.getScheduler();
    scheduler.loadRounds(selectedRounds);
  }

  /* This function is run when the user presses the 'Unload'-button on the
   * 'Admin'-form of the tournament scheduler. It makes sure that the scheduler
   * will clear the list with running rounds.
   */
  public void unloadRounds ()
  {
    log.info("Unloading Rounds");

    Scheduler scheduler = Scheduler.getScheduler();
    scheduler.unloadRounds(true);
  }

  public List<Round> getAvailableRounds ()
  {
    return availableRounds;
  }

  public List<String> getConfigErrors ()
  {
    return properties.getErrorMessages();
  }

  public void removeMessage (String message)
  {
    properties.removeErrorMessage(message);
  }
  //</editor-fold>

  //<editor-fold desc="Location stuff">
  public List<Location> getLocationList ()
  {
    return locationList;
  }

  public List<Location> getPossibleLocationList ()
  {
    return possibleLocations;
  }

  public void addLocation (Location l)
  {
    locationName = l.getLocation();
    locationTimezone = l.getTimezone();
    locationStartTime = l.getDateFrom();
    locationEndTime = l.getDateTo();
  }

  public void editLocation (Location l)
  {
    locationId = l.getLocationId();
    locationName = l.getLocation();
    locationTimezone = l.getTimezone();
    locationStartTime = l.getDateFrom();
    locationEndTime = l.getDateTo();
  }

  public void saveLocation ()
  {
    if (locationName.isEmpty() || locationStartTime == null || locationEndTime == null) {
      Utils.growlMessage("Location not saved.<br/>Some fields were empty!");
      return;
    }

    if (locationId == -1) {
      addLocation();
    }
    else {
      editLocation();
    }
  }

  public void addLocation ()
  {
    Location location = new Location();
    location.setLocation(locationName);
    location.setDateFrom(locationStartTime);
    location.setDateTo(locationEndTime);
    location.setTimezone(locationTimezone);

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(location);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    if (transaction.wasCommitted()) {
      log.info("Added new location " + locationName);
      resetLocationData();
    }

    session.close();
  }

  public void editLocation ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Location location = (Location) session.get(Location.class, locationId);
      location.setLocation(locationName);
      location.setDateFrom(locationStartTime);
      location.setDateTo(locationEndTime);
      location.setTimezone(locationTimezone);

      session.update(location);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      Utils.growlMessage("Location not edited.<br/>" + e.getMessage());
    }
    if (transaction.wasCommitted()) {
      log.info("Edited location " + locationName);
      resetLocationData();
    }

    session.close();
  }

  public void deleteLocation (Location location)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.delete(location);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
    resetLocationData();

    CheckWeatherServer checkWeatherServer = (CheckWeatherServer)
        SpringApplicationContext.getBean("checkWeatherServer");
    checkWeatherServer.loadExtraLocations();
  }

  private void resetLocationData ()
  {
    locationId = -1;
    locationName = "";
    locationTimezone = 0;
    locationStartTime = null;
    locationEndTime = null;
  }
  //</editor-fold>

  //<editor-fold desc="Pom stuff">
  public List<Pom> getPomList ()
  {
    return pomList;
  }

  public void submitPom ()
  {
    if (pomName.isEmpty()) {
      Utils.growlMessage("You need to fill in the pom name.");
      return;
    }

    if (uploadedPom == null) {
      Utils.growlMessage("You need to choose a pom file.");
      return;
    }

    User currentUser = User.getCurrentUser();
    Pom pom = new Pom();
    pom.setPomName(getPomName());
    pom.setUser(currentUser);

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(pom);
    }
    catch (ConstraintViolationException e) {
      transaction.rollback();
      session.close();
      Utils.growlMessage("This name is already used.");
      return;
    }

    Upload upload = new Upload(uploadedPom);
    String msg = upload.submit("pomLocation", pom.pomFileName());
    Utils.growlMessage(msg);

    if (msg.toLowerCase().contains("success")) {
      transaction.commit();
    }
    else {
      transaction.rollback();
    }
    session.close();
  }
  //</editor-fold>

  //<editor-fold desc="Machine stuff">
  public List<Machine> getMachineList ()
  {
    return machineList;
  }

  public void toggleAvailable (Machine machine)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      machine.setAvailable(!machine.isAvailable());
      session.update(machine);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public void toggleState (Machine machine)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (machine.isInProgress()) {
        machine.setStateIdle();
      }
      else {
        machine.setStateRunning();
      }
      session.update(machine);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public void editMachine (Machine m)
  {
    machineId = m.getMachineId();
    machineName = m.getMachineName();
    machineUrl = m.getMachineUrl();
    machineViz = m.getVizUrl();
  }

  public void saveMachine ()
  {
    machineUrl = machineUrl.replace("https://", "").replace("http://", "");
    machineViz = machineViz.replace("https://", "").replace("http://", "");

    if (machineName.isEmpty() || machineUrl.isEmpty() || machineViz.isEmpty()) {
      Utils.growlMessage("Machine not saved.<br/>Some fields were empty!");
      return;
    }

    // Make sure we get a new list of IPs
    MemStore.resetMachineIPs();
    MemStore.resetVizIPs();

    // It's a new machine
    if (machineId == -1) {
      addMachine();
    }
    else {
      editMachine();
    }
  }

  public void addMachine ()
  {
    Machine machine = new Machine();
    machine.setMachineName(machineName);
    machine.setMachineUrl(machineUrl);
    machine.setVizUrl(machineViz);
    machine.setStateIdle();
    machine.setAvailable(false);

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(machine);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      Utils.growlMessage("Machine not added.<br/>" + e.getMessage());
    }
    if (transaction.wasCommitted()) {
      log.info("Added new machine " + machineName);
      resetMachineData();
    }

    session.close();
  }

  public void editMachine ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Machine machine = (Machine) session.get(Machine.class, machineId);
      machine.setMachineName(machineName);
      machine.setMachineUrl(machineUrl);
      machine.setVizUrl(machineViz);

      session.update(machine);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      Utils.growlMessage("Machine not edited.<br/>" + e.getMessage());
    }
    if (transaction.wasCommitted()) {
      log.info("Edited machine " + machineName);
      resetMachineData();
    }

    session.close();
  }

  public void deleteMachine (Machine machine)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      log.info("Deleting machine " + machine.getMachineId());
      session.delete(machine);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      Utils.growlMessage("Machine not deleted.<br/>" + e.getMessage());
    }
    session.close();
    resetMachineData();
  }

  private void resetMachineData ()
  {
    machineId = -1;
    machineViz = "";
    machineName = "";
    machineUrl = "";
  }
  //</editor-fold>

  //<editor-fold desc="User stuff">
  public List<User> getUserList ()
  {
    return userList;
  }

  public void increasePermissions (User user)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      user.increasePermission();
      session.update(user);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      log.warn("Error increasing permissions for : " + user.getUserId());
      e.printStackTrace();
    }
    if (transaction.wasCommitted()) {
      log.info("Increased permissions for : " + user.getUserName());
    }

    session.close();
  }

  public void decreasePermissions (User user)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      user.decreasePermission();
      session.update(user);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      log.warn("Error decreasing permissions for : " + user.getUserId());
      e.printStackTrace();
    }
    if (transaction.wasCommitted()) {
      log.info("decreased permissions for : " + user.getUserName());
    }

    session.close();
  }
  //</editor-fold>

  //<editor-fold desc="Setters and Getters">
  public int getLocationId ()
  {
    return locationId;
  }

  public void setLocationId (int locationId)
  {
    this.locationId = locationId;
  }

  public String getLocationName ()
  {
    return locationName;
  }

  public void setLocationName (String locationName)
  {
    this.locationName = locationName;
  }

  public int getLocationTimezone ()
  {
    return locationTimezone;
  }

  public void setLocationTimezone (int locationTimezone)
  {
    this.locationTimezone = locationTimezone;
  }

  public Date getLocationStartTime ()
  {
    return locationStartTime;
  }

  public void setLocationStartTime (Date locationStartTime)
  {
    this.locationStartTime = locationStartTime;
  }

  public Date getLocationEndTime ()
  {
    return locationEndTime;
  }

  public void setLocationEndTime (Date locationEndTime)
  {
    this.locationEndTime = locationEndTime;
  }

  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    this.pomName = pomName.trim();
  }

  public UploadedFile getUploadedPom ()
  {
    return uploadedPom;
  }

  public void setUploadedPom (UploadedFile uploadedPom)
  {
    this.uploadedPom = uploadedPom;
  }

  public int getMachineId ()
  {
    return machineId;
  }

  public void setMachineId (int machineId)
  {
    this.machineId = machineId;
  }

  public String getMachineName ()
  {
    return machineName;
  }

  public void setMachineName (String machineName)
  {
    this.machineName = machineName;
  }

  public String getMachineUrl ()
  {
    return machineUrl;
  }

  public void setMachineUrl (String machineUrl)
  {
    this.machineUrl = machineUrl;
  }

  public String getMachineViz ()
  {
    return machineViz;
  }

  public void setMachineViz (String machineViz)
  {
    this.machineViz = machineViz;
  }

  public List<Integer> getSelectedRounds ()
  {
    return selectedRounds;
  }

  public void setSelectedRounds (List<Integer> selectedRounds)
  {
    this.selectedRounds = selectedRounds;
  }
  //</editor-fold>
}
