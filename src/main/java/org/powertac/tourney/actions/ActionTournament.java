package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.scheduling.MainScheduler;
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
public class ActionTournament
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int selectedPom;

  private Date startTime = new Date();
  private Date fromTime = new Date();
  private Date toTime = new Date();

  private String tournamentName;
  private int maxBrokers;
  private int maxBrokerInstances = 2;

  private String sortColumn = null;
  private boolean sortAscending = true;

  private List<String> locations;
  private Tournament.TYPE type = Tournament.TYPE.SINGLE_GAME;

  private int size1 = 2;
  private int numberSize1 = 2;
  private int size2 = 4;
  private int numberSize2 = 4;
  private int size3 = 8;
  private int numberSize3 = 4;

  public ActionTournament ()
  {
    Calendar initTime = Calendar.getInstance();

    initTime.set(2009, 2, 3);
    fromTime.setTime(initTime.getTimeInMillis());
    initTime.set(2011, 2, 3);
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

  public synchronized String createTournament ()
  {
    String allLocations = "";
    for (String s: locations) {
      allLocations += s + ",";
    }
    int[] gtypes = {size1, size2, size3};
    int[] mxs = {numberSize1, numberSize2, numberSize3};

    if (type == Tournament.TYPE.SINGLE_GAME) {
      return createSingleGameTournament(allLocations, gtypes, mxs);
    }
    else if (type == Tournament.TYPE.MULTI_GAME) {
      return createMultiGameTournament(allLocations, gtypes, mxs);
    }

    return "Success??";
  }

  private String createSingleGameTournament(String allLocations,
                                            int[] gtypes, int[] mxs)
  {
    log.info("Singlegame tournament selected");

    // Create a tournament and insert it into the application context
    Tournament newTourney = new Tournament();
    newTourney.setPomId(selectedPom);
    newTourney.setMaxBrokers(getMaxBrokers());
    newTourney.setStartTime(getStartTime());
    newTourney.setTournamentName(tournamentName);

    Database db = new Database();
    try {
      // Starts new transaction to prevent race conditions
      db.startTrans();
      log.info("Starting transaction");

      // Make sure tournament name is unique (case insensitive)
      if (db.getTournamentsByName(tournamentName).size() > 0) {
        String msg = "The tournament name already exists";
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
        FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
        db.abortTrans();
        return "Error";
      }

      // Adds new tournament to the database
      int tourneyId = db.addTournament(tournamentName, true, size1,
          startTime, Tournament.TYPE.SINGLE_GAME.toString(), selectedPom,
          allLocations, maxBrokers, gtypes, mxs);
      log.info("Adding new tourney " + tourneyId);

      // Adds a new game to the database
      int gameId = db.addGame(tournamentName, tourneyId, size1, startTime);
      log.info("Adding game " + gameId);

      // Create game properties
      log.info("Creating game: " + gameId + "properties");
      CreateProperties.genProperties(gameId, db, locations, fromTime, toTime);

      db.commitTrans();
      log.info("Committing transaction");
    }
    catch (SQLException sqle) {
      db.abortTrans();
      sqle.printStackTrace();
      log.error("Scheduling exception (single game tournament) !");
      return "Error";
    }

    return "Success";
  }

  private String createMultiGameTournament(String allLocations,
                                           int[] gtypes, int[] mxs)
  {
    log.info("Multigame tournament selected");

    truncateScheduler();

    Database db = new Database();
    try {
      // Starts new transaction to prevent race conditions
      db.startTrans();
      log.info("Starting transaction");

      int noofagents = maxBrokers;
      int noofcopies = maxBrokerInstances;
      int noofservers = db.getMachines().size();

      log.info("Starting MainScheduler..");
      log.info(String.format("Params -- Servers: %s Agents: %s Copies: %s games"
          + "=[ %s: %s, %s: %s, %s: %s ]", noofservers, noofagents, noofcopies,
          size1, numberSize1, size2, numberSize2, size3, numberSize3));

      MainScheduler gamescheduler = new MainScheduler(noofagents, noofservers);
      gamescheduler.initServerPanel(noofservers);
      gamescheduler.initializeAgentsDB(noofagents, noofcopies);
      gamescheduler.initGameCube(gtypes, mxs);

      int numberOfGames = gamescheduler.getGamesEstimate();

      log.info("No. of games: " + numberOfGames);
      gamescheduler.resetCube();

      // Adds new tournament to the database
      int tourneyId = db.addTournament(tournamentName, true, numberOfGames,
          startTime, Tournament.TYPE.MULTI_GAME.toString(), selectedPom,
          allLocations, maxBrokers, gtypes, mxs);
      log.info("Adding new tourney " + tourneyId);

      // Adds new games to the database
      for (int i=0; i<numberOfGames; i++) {
        int gameId = db.addGame(tournamentName + "_" + i,
            tourneyId, getMaxBrokers(), startTime);
        log.info("Adding game " + gameId);

        CreateProperties.genProperties(gameId, db, locations, fromTime, toTime);
        log.info("Creating game: " + gameId + " properties");
      }

      db.commitTrans();
      log.info("Committing transaction");

      String msg = "Number of games in tournament: " + numberOfGames;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
    catch (SQLException sqle) {
      db.abortTrans();
      sqle.printStackTrace();
    }
    catch (Exception e) {
      db.abortTrans();
      log.error("Scheduling exception (multi game tournament) !");
      e.printStackTrace();
      return "Error";
    }

    return "Success";
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

  public List<Location> getLocationList(){
    List<Location> locations = new ArrayList<Location>();

    Database db = new Database();

    try{
      db.startTrans();
      locations = db.getLocations();
      db.commitTrans();
    }catch(Exception e){
      e.printStackTrace();
    }
    return locations;
  }

  private void truncateScheduler() {
    Database db = new Database();
    try {
      db.startTrans();
      db.truncateScheduler();
      db.commitTrans();
    }
    catch(Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
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

  public int getNumberSize1 ()
  {
    return numberSize1;
  }
  public void setNumberSize1 (int numberSize1)
  {
    this.numberSize1 = numberSize1;
  }

  public int getSize2 ()
  {
    return size2;
  }
  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  public int getNumberSize2 ()
  {
    return numberSize2;
  }
  public void setNumberSize2 (int numberSize2)
  {
    this.numberSize2 = numberSize2;
  }

  public int getSize3 ()
  {
    return size3;
  }
  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  public int getNumberSize3 ()
  {
    return numberSize3;
  }
  public void setNumberSize3 (int numberSize3)
  {
    this.numberSize3 = numberSize3;
  }

  public int getMaxBrokerInstances ()
  {
    return maxBrokerInstances;
  }
  public void setMaxBrokerInstances (int maxBrokerInstances)
  {
    this.maxBrokerInstances = maxBrokerInstances;
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
