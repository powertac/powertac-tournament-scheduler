package org.powertac.tourney.actions;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.hibernate.Session;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.powertac.tourney.services.Utils.log;

@Component("actionAdmin")
@Scope("request")
public class ActionAdmin
{
  @Autowired
  private Upload upload;

  @Autowired
  private Scheduler scheduler;

  private String sortColumn = null;
  private boolean sortAscending = true;
  private int rowCount = 5;

  private String sortColumnMachine = null;
  private boolean sortAscendingMachine = true;
  private int rowCountMachine = 5;

  private String sortColumnUsers = null;
  private boolean sortAscendingUsers = true;
  private int rowCountUsers = 5;

  private String sortColumnTournaments = null;
  private boolean sortAscendingTournaments = true;
  private int rowCountTournaments = 5;

  private String sortColumnGames = null;
  private boolean sortAscendingGames = true;
  private int rowCountGames = 5;

  private String newLocationName = "";
  private Date newLocationStartTime = null;
  private Date newLocationEndTime = null;

  private int machineId = -1;

  private String newName = "";
  private String newUrl = "";
  private String newViz = "";
  private String newQueue = "";

  private UploadedFile pom;
  private String pomName;

  private TournamentProperties properties = TournamentProperties.getProperties();

  public ActionAdmin ()
  {
  }

  public List<String> getConfigErrors()
  {
    return properties.getConfigErrors();
  }

  public List<Game> getGameList()
  {
    List<Game> games = new ArrayList<Game>();
    
    Database db = new Database();
    try{
      db.startTrans();
      games = db.getGames();
      db.commitTrans();
    }catch (SQLException e){
      db.abortTrans();
      e.printStackTrace();
    }

    return games;
  }
  
  public List<User> getUserList()
  {
    List<User> users = new ArrayList<User>();
    
    Database db = new Database();
    try {
      db.startTrans();
      users = db.getAllUsers();
      db.commitTrans();
    }
    catch (SQLException e){
      db.abortTrans();
      e.printStackTrace();
    }

    return users;
  }

  public void submitPom ()
  {
    if (pomName.isEmpty()) {
      // Show succes message.
      String msg = "Error: You need to fill in the pom name";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      return;
    }

    if (pom == null) {
      String msg = "Error: You need to choose a pom file";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      return;
    }

    User currentUser =
        (User) FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().get(User.getKey());

    Session session = HibernateUtil.getSessionFactory().openSession();
    session.beginTransaction();

    Pom p = new Pom();
    p.setName(this.getPomName());
    p.setUploadingUser(currentUser.getUsername());

    session.save(p);

    upload.setUploadedFile(pom);
    upload.setUploadLocation(properties.getProperty("pomLocation"));
    boolean pomStored = upload.submit("pom." + p.getPomId() + ".xml");

    if (pomStored) {
      session.getTransaction().commit();
    }
    else {
      session.getTransaction().rollback();
    }
  }

  public List<Database.Pom> getPomList ()
  {
    List<Database.Pom> poms = new ArrayList<Database.Pom>();

    Database db = new Database();

    try {
      db.startTrans();
      poms = db.getPoms();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return poms;
  }

  public List<Tournament> getTournamentList(){
    List<Tournament> ts = new ArrayList<Tournament>();
    
    Database db = new Database();
    try {
      db.startTrans();
      ts = db.getTournaments(Tournament.STATE.pending);
      ts.addAll(db.getTournaments(Tournament.STATE.in_progress));
      db.commitTrans();
    }
    catch(Exception e) {
      db.abortTrans();
    }

    return ts;
  }
  
  public List<Location> getLocationList ()
  {
    List<Location> locations = new ArrayList<Location>();
    Database db = new Database();

    try {
      db.startTrans();
      locations = db.getLocations();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return locations;
  }

  public List<Broker> getRegistered (int tourneyId)
  {
    List<Broker> registered = new ArrayList<Broker>();
    Database db = new Database();

    try {
      db.startTrans();
      registered = db.getBrokersInTournament(tourneyId);
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return registered;
  }

  public List<Machine> getMachineList ()
  {
    List<Machine> machines = new ArrayList<Machine>();
    Database db = new Database();

    try {
      db.startTrans();
      machines = db.getMachines();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return machines;
  }

  public void toggleAvailable (Machine m)
  {
    Database db = new Database();

    try {
      db.startTrans();
      if (m.isAvailable()) {
        db.setMachineAvailable(m.getMachineId(), false);
      }
      else {
        db.setMachineAvailable(m.getMachineId(), true);
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }
  public void toggleStatus(Machine m){
    Database db = new Database();
    
    try {
      db.startTrans();
      if(m.isInProgress()){
        db.setMachineStatus(m.getMachineId(), Machine.STATE.idle);
      }else{
        db.setMachineStatus(m.getMachineId(), Machine.STATE.running);
      }
      db.commitTrans();
    }
    catch(Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }
  
  public void editMachine(Machine m)
  {
    machineId = m.getMachineId();
    newName = m.getName();
    newUrl = m.getUrl();
    newViz = m.getVizUrl();
  }
  
  public void saveMachine()
  {
    newUrl = newUrl.replace("https://", "").replace("http://", "");
    newViz = newViz.replace("https://", "").replace("http://", "");

    if (newName.isEmpty() || newUrl.isEmpty() || newViz.isEmpty()) {
      String msg = "Error: machine not saved, some fields were empty!";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
  	  return;
  	}  
	  
    // It's a new machine
    if (machineId == -1) {
      addMachine();
      return;
    }
	  
    Database db = new Database();
    try {
      db.startTrans();
      db.editMachine(newName, newUrl, newViz, machineId);
      db.commitTrans();
      resetMachineData();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
      String msg = "Error : machine not edited " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
    }
  }

  public void deleteMachine (Machine m)
  {
    Database db = new Database();
    try {
      db.startTrans();
      db.deleteMachine(m.getMachineId());
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
      String msg = "Error : machine not added " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
    }
  }

  public void addMachine ()
  {
    Database db = new Database();
    try {
      db.startTrans();
      db.addMachine(newName, newUrl, newViz);
      db.commitTrans();
      
      resetMachineData();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
      String msg = "Error : machine not added " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
    }
  }

  public void restartGame (Game g)
  {
    Database db = new Database();
    int gameId = g.getGameId();
    log("[INFO] Restarting Game {0} has status: {1}", gameId, g.getStatus());
    Tournament t = new Tournament();

    try {
      db.startTrans();
      t = db.getTournamentByGameId(gameId);

      db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
      log("[INFO] Setting machine: {0} to idle", g.getMachineId());
      db.commitTrans();

    }
    catch (SQLException e) {
      db.abortTrans();
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (g.stateEquals(Game.STATE.boot_failed) ||
        g.stateEquals(Game.STATE.boot_pending) ||
        g.stateEquals(Game.STATE.boot_in_progress) ) {
      log("[INFO] Attempting to restart bootstrap {0}", gameId);

      RunBootstrap runBootstrap = new RunBootstrap(gameId, t.getPomId());
      new Thread(runBootstrap).start();
    }
    else if (g.stateEquals(Game.STATE.game_failed) ||
             g.stateEquals(Game.STATE.game_in_progress) ||
            g.stateEquals(Game.STATE.boot_failed) ) {
      log("[INFO] Attempting to restart sim {0}", gameId);

      RunGame runGame = new RunGame(g.getGameId(), t.getPomId());
      new Thread(runGame).start();
    }
  }

  /**
   * We should be able to delete games
   * But should we be able to abandon running games?
   * @param g : the Game to delete
   */
  public void deleteGame (Game g)
  {
    // TODO: ARE YOU SURE?
    //scheduler.deleteBootTimer(g.getGameId());
    //scheduler.deleteSimTimer(g.getGameId());
  }

  public void refresh ()
  {

  }

  public void deleteLocation (Location l)
  {
    Database db = new Database();
    try {
      db.startTrans();
      db.deleteLocation(l.getLocationId());
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  public void addLocation ()
  {
    if (newLocationName.isEmpty() || (newLocationStartTime == null) || (newLocationEndTime == null)) {
      log("Some location fields are empty!");
      return;
    }
	  
    Database db = new Database();
    try {
      db.startTrans();
      db.addLocation(newLocationName, newLocationStartTime, newLocationEndTime);
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  private void resetMachineData() {
    machineId = -1;
    newName = "";
    newUrl = "";
    newViz = "";
    newQueue = "";
  }

  //<editor-fold desc="Setters and Getters">
  public String getPomName ()
  {
    return pomName;
  }
  public void setPomName (String pomName)
  {
    this.pomName = pomName.trim();
  }

  public UploadedFile getPom ()
  {
    return pom;
  }
  public void setPom (UploadedFile pom)
  {
    this.pom = pom;
  }

  public int getRowCount ()
  {
    return rowCount;
  }
  public void setRowCount (int rowCount)
  {
    this.rowCount = rowCount;
  }

  public boolean isSortAscending ()
  {
    return sortAscending;
  }
  public void setSortAscending (boolean sortAscending)
  {
    this.sortAscending = sortAscending;
  }

  public String getSortColumn ()
  {
    return sortColumn;
  }
  public void setSortColumn (String sortColumn)
  {
    this.sortColumn = sortColumn;
  }

  public String getNewLocationName ()
  {
    return newLocationName;
  }
  public void setNewLocationName (String newLocationName)
  {
    this.newLocationName = newLocationName;
  }

  public Date getNewLocationStartTime ()
  {
    return newLocationStartTime;
  }
  public void setNewLocationStartTime (Date newLocationStartTime)
  {
    this.newLocationStartTime = newLocationStartTime;
  }

  public Date getNewLocationEndTime ()
  {
    return newLocationEndTime;
  }
  public void setNewLocationEndTime (Date newLocationEndTime)
  {
    this.newLocationEndTime = newLocationEndTime;
  }

  public String getSortColumnMachine ()
  {
    return sortColumnMachine;
  }
  public void setSortColumnMachine (String sortColumnMachine)
  {
    this.sortColumnMachine = sortColumnMachine;
  }

  public boolean isSortAscendingMachine ()
  {
    return sortAscendingMachine;
  }
  public void setSortAscendingMachine (boolean sortAscendingMachine)
  {
    this.sortAscendingMachine = sortAscendingMachine;
  }

  public int getRowCountMachine ()
  {
    return rowCountMachine;
  }
  public void setRowCountMachine (int rowCountMachine)
  {
    this.rowCountMachine = rowCountMachine;
  }

  public String getNewName ()
  {
    return newName;
  }
  public void setNewName (String newName)
  {
    this.newName = newName;
  }

  public String getNewUrl ()
  {
    return newUrl;
  }
  public void setNewUrl (String newUrl)
  {
    this.newUrl = newUrl;
  }

  public String getNewViz ()
  {
    return newViz;
  }
  public void setNewViz (String newViz)
  {
    this.newViz = newViz;
  }

  public String getNewQueue ()
  {
    return newQueue;
  }
  public void setNewQueue (String newQueue)
  {
    this.newQueue = newQueue;
  }

  public int getMachineId ()
  {
	  return machineId;
  }
  public void setMachineId(int machineId) {
	  this.machineId = machineId;
  }

  public String getSortColumnUsers ()
  {
    return sortColumnUsers;
  }
  public void setSortColumnUsers (String sortColumnUsers)
  {
    this.sortColumnUsers = sortColumnUsers;
  }

  public boolean isSortAscendingUsers ()
  {
    return sortAscendingUsers;
  }
  public void setSortAscendingUsers (boolean sortAscendingUsers)
  {
    this.sortAscendingUsers = sortAscendingUsers;
  }

  public int getRowCountUsers ()
  {
    return rowCountUsers;
  }
  public void setRowCountUsers (int rowCountUsers)
  {
    this.rowCountUsers = rowCountUsers;
  }

  public String getSortColumnTournaments ()
  {
    return sortColumnTournaments;
  }
  public void setSortColumnTournaments (String sortColumnTournaments)
  {
    this.sortColumnTournaments = sortColumnTournaments;
  }

  public boolean isSortAscendingTournaments ()
  {
    return sortAscendingTournaments;
  }
  public void setSortAscendingTournaments (boolean sortAscendingTournaments)
  {
    this.sortAscendingTournaments = sortAscendingTournaments;
  }

  public int getRowCountTournaments ()
  {
    return rowCountTournaments;
  }
  public void setRowCountTournaments (int rowCountTournaments)
  {
    this.rowCountTournaments = rowCountTournaments;
  }

  public String getSortColumnGames ()
  {
    return sortColumnGames;
  }
  public void setSortColumnGames (String sortColumnGames)
  {
    this.sortColumnGames = sortColumnGames;
  }

  public boolean isSortAscendingGames ()
  {
    return sortAscendingGames;
  }
  public void setSortAscendingGames (boolean sortAscendingGames)
  {
    this.sortAscendingGames = sortAscendingGames;
  }

  public int getRowCountGames ()
  {
    return rowCountGames;
  }
  public void setRowCountGames (int rowCountGames)
  {
    this.rowCountGames = rowCountGames;
  }
  //</editor-fold>
}
