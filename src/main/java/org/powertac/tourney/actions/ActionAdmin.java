package org.powertac.tourney.actions;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.hibernate.Session;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Pom;
import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Upload;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.powertac.tourney.services.Utils.log;

@ManagedBean
@RequestScoped
public class ActionAdmin
{
  private String sortColumnPom = null;
  private boolean sortAscendingPom = true;
  private String sortColumnMachine = null;
  private boolean sortAscendingMachine = true;
  private String sortColumnUsers = null;
  private boolean sortAscendingUsers = true;

  private String newLocationName = "";
  private Date newLocationStartTime = null;
  private Date newLocationEndTime = null;

  private int machineId = -1;

  private String newName = "";
  private String newUrl = "";
  private String newViz = "";

  private Upload upload = new Upload();
  private UploadedFile uploadedPom;
  private String pomName;

  private TournamentProperties properties = TournamentProperties.getProperties();

  public ActionAdmin ()
  {
  }

  public List<String> getConfigErrors()
  {
    return properties.getConfigErrors();
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

    if (uploadedPom == null) {
      String msg = "Error: You need to choose a pom file";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      return;
    }

    if (upload == null) {
      upload = new Upload();
    }

    User currentUser = User.getCurrentUser();

    Session session = HibernateUtil.getSessionFactory().openSession();
    session.beginTransaction();

    Pom p = new Pom();
    p.setName(getPomName());
    p.setUploadingUser(currentUser.getUsername());

    session.save(p);

    upload.setUploadedFile(uploadedPom);
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

  public UploadedFile getUploadedPom ()
  {
    return uploadedPom;
  }
  public void setUploadedPom (UploadedFile uploadedPom)
  {
    this.uploadedPom = uploadedPom;
  }

  public boolean isSortAscendingPom ()
  {
    return sortAscendingPom;
  }
  public void setSortAscendingPom (boolean sortAscendingPom)
  {
    this.sortAscendingPom = sortAscendingPom;
  }

  public String getSortColumnPom()
  {
    return sortColumnPom;
  }
  public void setSortColumnPom(String sortColumnPom)
  {
    this.sortColumnPom = sortColumnPom;
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
  //</editor-fold>
}
