package org.powertac.tourney.actions;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.beans.Pom;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.services.CreateProperties;
import org.powertac.tourney.services.Database;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@ManagedBean
@RequestScoped
public class ActionTournamentManager
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int selectedPom;

  private Date startTime = new Date();
  private Date fromTime = new Date();
  private Date toTime = new Date();

  private String tournamentName;
  private int maxBrokers;
  private int maxAgents = 2;

  private String sortColumn = null;
  private boolean sortAscending = true;

  private List<String> locations;
  private Tournament.TYPE type = Tournament.TYPE.SINGLE_GAME;

  private int size1 = 2;
  private int size2 = 4;
  private int size3 = 8;

  public ActionTournamentManager()
  {
    Calendar initTime = Calendar.getInstance();

    initTime.set(2009, Calendar.MARCH, 3);
    fromTime.setTime(initTime.getTimeInMillis());
    initTime.set(2011, Calendar.MARCH, 3);
    toTime.setTime(initTime.getTimeInMillis());
  }

  // Method to list the type enumeration in the jsf select Item component
  public SelectItem[] getTypes ()
  {
    SelectItem[] items = new SelectItem[Tournament.TYPE.values().length];
    int i = 0;
    for (Tournament.TYPE t: Tournament.TYPE.values()) {
      items[i++] = new SelectItem(t, t.name());
    }
    return items;
  }

  public List<Tournament> getTournamentList() {
    return Tournament.getTournamentList();
  }

  public synchronized void createTournament ()
  {
    String allLocations = "";
    for (String s: locations) {
      allLocations += s + ",";
    }

    boolean created = false;
    if (type == Tournament.TYPE.SINGLE_GAME) {
      created = createSingleGameTournament(allLocations);
    }
    else if (type == Tournament.TYPE.MULTI_GAME) {
      created = createMultiGameTournament(allLocations);
    }

    if (created) {
      tournamentName = "";
      maxBrokers = 0;
      maxAgents = 2;
      type = Tournament.TYPE.SINGLE_GAME;
      size1 = 2;
      size2 = 4;
      size3 = 8;
    }
  }

  private boolean createSingleGameTournament(String allLocations)
  {
    log.info("Singlegame tournament selected");

    // Create a tournament and insert it into the application context
    Database db = new Database();
    try {
      // Starts new transaction to prevent race conditions
      db.startTrans();

      // Add new tournament to the database
      int tourneyId = db.addTournament(tournamentName, startTime, fromTime,
          toTime, Tournament.TYPE.SINGLE_GAME, selectedPom,
          allLocations, maxBrokers, new int[] {0, 0, 0});
      log.info("Created tournament " + tourneyId);

      // Add a new game to the database
      int gameId = db.addGame(tournamentName, tourneyId, maxBrokers, startTime);
      log.info("Created game " + gameId);

      // Create game properties
      CreateProperties.genProperties(db, gameId, locations, fromTime, toTime);
      log.info("Created game: " + gameId + " properties");

      db.commitTrans();
      return true;
    }
    catch (MySQLIntegrityConstraintViolationException me) {
      db.abortTrans();
      String msg = "The tournament name already exists";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      log.error("Scheduling exception (single game tournament) !");
    }
    return false;
  }

  private boolean createMultiGameTournament(String allLocations)
  {
    log.info("Multigame tournament selected");

    Database db = new Database();
    try {
      db.startTrans();
      int tourneyId = db.addTournament(tournamentName, startTime, fromTime,
          toTime, Tournament.TYPE.MULTI_GAME, selectedPom,
          allLocations, maxBrokers, new int[] {size1, size2, size3});
      db.commitTrans();
      log.info("Created tournament " + tourneyId);
      return true;
    }
    catch (MySQLIntegrityConstraintViolationException me) {
      db.abortTrans();
      String msg = "The tournament name already exists";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
    catch (Exception e) {
      db.abortTrans();
      log.error("Scheduling exception (multi game tournament) !");
      e.printStackTrace();
    }
    return false;
  }

  public List<Pom> getPomList ()
  {
    List<Pom> poms = new ArrayList<Pom>();

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

  public List<Location> getLocationList(){
    List<Location> locations = new ArrayList<Location>();

    Database db = new Database();
    try {
      db.startTrans();
      locations = db.getLocations();
      db.commitTrans();
    }
    catch(Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
    return locations;
  }

  public void removeTournament(Tournament t)
  {
    if (t == null) {
      return;
    }

    if (!t.getTournamentName().toLowerCase().contains("test")) {
      log.info("Someone tried to remove a non-test Tournament!");
      String msg = "Nice try, hacker!" ;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
      FacesContext.getCurrentInstance().addMessage("removeTournament", fm);
      return;
    }

    String msg = t.remove();
    if (!msg.isEmpty()) {
      log.info(String.format("Something went wrong with removing tournament "
          + "%s\n%s", t.getTournamentName(), msg));
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
      FacesContext.getCurrentInstance().addMessage("removeTournament", fm);
    }
  }

  public void refresh ()
  {
  }

  //<editor-fold desc="Setters and getters">
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

  public int getMaxAgents()
  {
    return maxAgents;
  }
  public void setMaxAgents(int maxAgents)
  {
    this.maxAgents = maxAgents;
  }

  public String getSortColumn ()
  {
    return sortColumn;
  }
  public void setSortColumn (String sortColumn)
  {
    this.sortColumn = sortColumn;
  }

  public boolean isSortAscending ()
  {
    return sortAscending;
  }
  public void setSortAscending (boolean sortAscending)
  {
    this.sortAscending = sortAscending;
  }

  public Tournament.TYPE getType ()
  {
    return type;
  }
  public void setType (Tournament.TYPE type)
  {
    this.type = type;
  }

  public Date getStartTime ()
  {
    return startTime;
  }
  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  public int getMaxBrokers ()
  {
    return maxBrokers;
  }
  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  public String getTournamentName ()
  {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  public Date getFromTime ()
  {
    return fromTime;
  }
  public void setFromTime (Date fromTime)
  {
    this.fromTime = fromTime;
  }

  public Date getToTime ()
  {
    return toTime;
  }
  public void setToTime (Date toTime)
  {
    this.toTime = toTime;
  }
  //</editor-fold>
}
