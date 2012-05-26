package org.powertac.tourney.actions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;


import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.RunBootstrap;
import org.powertac.tourney.services.RunGame;
import org.powertac.tourney.services.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

  private String newLocationName = "";
  private Date newLocationStartTime;
  private Date newLocationEndTime;

  private String newName = "";
  private String newUrl = "";
  private String newViz = "";
  private String newQueue = "";

  private UploadedFile pom;
  private String pomName;
  private Properties props = new Properties();

  public ActionAdmin ()
  {

    try {
      props.load(Database.class.getClassLoader()
              .getResourceAsStream("/tournament.properties"));
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    this.pomName = pomName;
  }

  public UploadedFile getPom ()
  {
    return pom;
  }

  public void setPom (UploadedFile pom)
  {
    this.pom = pom;
  }

  public void submitPom ()
  {
    if (pom != null) {
      System.out.println("Pom is not null");
      upload.setUploadedFile(this.pom);
      String finalName = upload.submit(this.pomName);

      User currentUser =
        (User) FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().get(User.getKey());

      Database db = new Database();
      try {
        db.openConnection();
        db.addPom(currentUser.getUsername(), this.getPomName(),
                  upload.getUploadLocation() + finalName);
        db.closeConnection();

      }
      catch (SQLException e) {
        db.closeConnection();
        e.printStackTrace();
      }
    }
    else {
      System.out.println("Pom was null");
    }
  }

  public List<Database.Pom> getPomList ()
  {
    List<Database.Pom> poms = new ArrayList<Database.Pom>();

    Database db = new Database();

    try {
      db.openConnection();
      poms = db.getPoms();
      db.closeConnection();

    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }
    return poms;

  }

  public List<Location> getLocationList ()
  {
    List<Location> locations = new ArrayList<Location>();
    Database db = new Database();

    try {
      db.openConnection();
      locations = db.getLocations();
      db.closeConnection();

    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }

    return locations;

  }

  public List<Broker> getRegistered (int tourneyId)
  {
    List<Broker> registered = new ArrayList<Broker>();
    Database db = new Database();

    try {
      db.openConnection();
      registered = db.getBrokersInTournament(tourneyId);
      db.closeConnection();
    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }

    return registered;
  }

  public List<Machine> getMachineList ()
  {
    List<Machine> machines = new ArrayList<Machine>();
    Database db = new Database();

    try {
      db.openConnection();
      machines = db.getMachines();
      db.closeConnection();
    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }

    return machines;
  }

  public void toggleAvailable (Machine m)
  {
    Database db = new Database();

    try {
      db.openConnection();
      if (m.isAvailable()) {
        db.setMachineAvailable(m.getMachineId(), false);
      }
      else {
        db.setMachineAvailable(m.getMachineId(), true);
      }
      db.closeConnection();
    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }

  }

  public void deleteMachine (Machine m)
  {
    Database db = new Database();
    try {
      db.openConnection();
      db.deleteMachine(m.getMachineId());
      db.closeConnection();
    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }
  }

  public void addMachine ()
  {
    Database db = new Database();
    try {
      db.openConnection();
      db.addMachine(newName, newUrl, newViz, getNewQueue());
      db.closeConnection();
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public void restartGame (Game g)
  {
    Database db = new Database();
    int gameId = g.getGameId();
    System.out.println("[INFO] Restarting Game " + gameId + " has status: "
                       + g.getStatus());
    Tournament t = new Tournament();
    try {
      db.openConnection();
      db.startTrans();
      t = db.getTournamentByGameId(gameId);

      db.setMachineStatus(g.getMachineId(), "idle");
      System.out.println("[INFO] Setting machine: " + g.getMachineId()
                         + " to idle");
      db.commitTrans();
      db.closeConnection();

    }
    catch (SQLException e) {
      db.closeConnection();
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    String hostip = "http://";

    try {
      InetAddress thisIp = InetAddress.getLocalHost();
      hostip += thisIp.getHostAddress() + ":8080";
    }
    catch (UnknownHostException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }

    if (g.getStatus().equalsIgnoreCase("boot-failed")) {
      System.out.println("[INFO] Attempting to restart bootstrap " + gameId);
      scheduler.deleteBootTimer(gameId);
      scheduler
              .runBootTimer(gameId,
                            new RunBootstrap(gameId, hostip
                                                     + "/TournamentScheduler/",
                                             t.getPomUrl(),
                                             props.getProperty("destination")),
                            new Date());

    }
    else if (g.getStatus().equalsIgnoreCase("game-failed")) {
      // If restarting a failed game schedule it for immediate running
      System.out.println("[INFO] Attempting to restart sim " + gameId);
      scheduler.deleteSimTimer(gameId);
      scheduler.runSimTimer(gameId,
                            new RunGame(gameId, hostip
                                                + "/TournamentScheduler/", t
                                    .getPomUrl(), props
                                    .getProperty("destination")), new Date());

    }
    else if (g.getStatus().equalsIgnoreCase("boot-pending")) {
      System.out.println("[INFO] Attempting to restart bootstrap " + gameId);
      scheduler.deleteBootTimer(gameId);
      scheduler
              .runBootTimer(gameId,
                            new RunBootstrap(gameId, hostip
                                                     + "/TournamentScheduler/",
                                             t.getPomUrl(),
                                             props.getProperty("destination")),
                            new Date());

    }
    else if (g.getStatus().equalsIgnoreCase("boot-in-progress")) {
      System.out.println("[INFO] Attempting to restart bootstrap " + gameId);
      scheduler.deleteBootTimer(gameId);
      scheduler
              .runBootTimer(gameId,
                            new RunBootstrap(gameId, hostip
                                                     + "/TournamentScheduler/",
                                             t.getPomUrl(),
                                             props.getProperty("destination")),
                            new Date());

    }
    else if (g.getStatus().equalsIgnoreCase("game-pending")) {
      System.out.println("[INFO] Attempting to restart sim " + gameId);
      scheduler.deleteSimTimer(gameId);
      scheduler.runSimTimer(gameId,
                            new RunGame(gameId, hostip
                                                + "/TournamentScheduler/", t
                                    .getPomUrl(), props
                                    .getProperty("destination")), new Date());

    }
    else if (g.getStatus().equalsIgnoreCase("boot-complete")) {
      System.out.println("[INFO] Attempting to restart sim " + gameId);
      scheduler.deleteSimTimer(gameId);
      scheduler.runSimTimer(gameId,
                            new RunGame(gameId, hostip
                                                + "/TournamentScheduler/", t
                                    .getPomUrl(), props
                                    .getProperty("destination")), new Date());
    }
    else {
      // Nothing
    }

  }

  public void deleteGame (Game g)
  {
    // TODO: ARE YOU SURE?
    scheduler.deleteBootTimer(g.getGameId());
    scheduler.deleteSimTimer(g.getGameId());
  }

  public void refresh ()
  {

  }

  public void deleteLocation (Location l)
  {
    Database db = new Database();
    try {
      db.openConnection();
      db.deleteLocation(l.getLocationId());
      db.closeConnection();
    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }

  }

  public void addLocation ()
  {
    Database db = new Database();
    try {
      db.openConnection();
      db.addLocation(newLocationName, newLocationStartTime, newLocationEndTime);
      db.closeConnection();
    }
    catch (SQLException e) {
      db.closeConnection();
      e.printStackTrace();
    }

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

}
