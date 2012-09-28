package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.beans.Pom;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.*;


@ManagedBean
@RequestScoped
public class ActionTournamentManager
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String sortColumn = null;
  private boolean sortAscending = true;
  private boolean[] disabled = new boolean[13];

  private int tourneyId = -1;
  private String tournamentName;
  private Tournament.TYPE type;
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
  private int selectedPom;
  private List<String> locations;
  private boolean closed;

  public ActionTournamentManager()
  {
    resetValues();
  }

  public List<Tournament.TYPE> getTypes ()
  {
    return Arrays.asList(Tournament.TYPE.values());
  }

  public List<Tournament> getTournamentList()
  {
    return Tournament.getNotCompleteTournamentList();
  }

  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public List<Location> getLocationList()
  {
    return Location.getLocationList();
  }

  public void saveTournament()
  {
    if (tournamentName.trim().isEmpty()) {
      String msg = "The tournament name cannot be empty";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
      if (tourneyId != -1) {
        resetValues();
      }
      return;
    }

    if ((locations.size() < 1) && (tourneyId == -1 || selectedPom != 0)) {
      String msg = "Choose at least one location";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
      if (tourneyId != -1) {
        resetValues();
      }
      return;
    }

    if (tourneyId != -1) {
      saveEditedTournament();
    } else {
      createTournament();
    }
  }

  private void createTournament ()
  {
    log.info("Creating " + type.toString() + " tournament");

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = new Tournament();
      setValues(tournament);
      tournament.setStatus(Tournament.STATE.pending.toString());
      session.save(tournament);

      log.info(String.format("Created %s tournament %s",
          type.toString(), tournament.getTournamentId()));

      if (type == Tournament.TYPE.SINGLE_GAME) {
        Game game = Game.createGame(tournament, tournamentName);
        session.save(game);
        log.info("Created game " + game.getGameId());
      }

      transaction.commit();
      resetValues();
    }
    catch (ConstraintViolationException ignored) {
      transaction.rollback();
      String msg = "The tournament name already exists";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error creating tournament");
    }
    finally {
      session.close();
    }
  }

  public void editTournament (Tournament tournament)
  {
    tourneyId = tournament.getTournamentId();
    tournamentName = tournament.getTournamentName();
    type = Tournament.TYPE.valueOf(tournament.getType());
    maxBrokers = tournament.getMaxBrokers();
    maxAgents = tournament.getMaxAgents();
    size1 = tournament.getSize1();
    size2 = tournament.getSize2();
    size3 = tournament.getSize3();
    multiplier1 = tournament.getMultiplier1();
    multiplier2 = tournament.getMultiplier2();
    multiplier3 = tournament.getMultiplier3();
    startTime = tournament.getStartTime();
    dateFrom = tournament.getDateFrom();
    dateTo = tournament.getDateTo();
    selectedPom = tournament.getPomId();
    locations = tournament.getLocationsList();
    closed = tournament.isClosed();

    // Once scheduled, these params can't change
    if (tournament.getGameMap().size() > 0) {
      disabled[0] = true; // name
      disabled[2] = true; // maxBrokers
      disabled[3] = true; // maxAgents
      disabled[4] = true; // size1
      disabled[5] = true; // size2
      disabled[6] = true; // size3



      disabled[7] = true; // startTime
      disabled[8] = true; // date from
      disabled[9] = true; // date to
      disabled[10] = true; // pom
      disabled[11] = true; // locations
    }
    disabled[1] = true; // type
    disabled[12] = true; // closed
  }

  public void saveEditedTournament()
  {
    log.info("Saving tournament " + tourneyId);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    boolean saved = false;
    try {
      Tournament tournament = (Tournament) session.get(Tournament.class, tourneyId);
      setValues(tournament);
      session.saveOrUpdate(tournament);
      transaction.commit();
      saved = true;
    }
    catch (ConstraintViolationException ignored) {
      transaction.rollback();
      String msg = "The tournament name already exists";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error saving tournament");
    }
    finally {
      session.close();
    }

    if (saved) {
      resetValues();
    }
  }

  public void removeTournament(Tournament tournament)
  {
    if (!tournament.getTournamentName().toLowerCase().contains("test")) {
      log.info("Someone tried to remove a non-test Tournament!");
      String msg = "Nice try, hacker!" ;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
      FacesContext.getCurrentInstance().addMessage("removeTournament", fm);
      return;
    }

    String msg = tournament.delete();
    if (!msg.isEmpty()) {
      log.info(String.format("Something went wrong with removing tournament "
          + "%s\n%s", tournament.getTournamentName(), msg));
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
      FacesContext.getCurrentInstance().addMessage("removeTournament", fm);
    }
  }

  public void setValues (Tournament tournament)
  {
    String allLocations = "";
    for (String s: locations) {
      allLocations += s + ",";
    }

    Integer[] gameTypes = {Math.max(1,size1), Math.max(1,size2),
        Math.max(1,size3)};
    Integer[] multipliers = {Math.max(0,multiplier1), Math.max(0,multiplier2),
        Math.max(0,multiplier3)};
    Arrays.sort(gameTypes,Collections.reverseOrder());
    maxBrokers = Math.max(maxBrokers, tournament.getBrokerMap().size());

    if (tournament.getGameMap().size() < 1) {
      tournament.setTournamentName(tournamentName);
      if (type != null) {
        tournament.setType(type.toString());
      }
      tournament.setMaxBrokers(maxBrokers);
      tournament.setMaxAgents(tournament.isMulti() ? maxAgents : 0);
      tournament.setSize1(tournament.isMulti() ? gameTypes[0] : 0);
      tournament.setSize2(tournament.isMulti() ? gameTypes[1] : 0);
      tournament.setSize3(tournament.isMulti() ? gameTypes[2] : 0);
      tournament.setMultiplier1(tournament.isMulti() ? multipliers[0] : 0);
      tournament.setMultiplier2(tournament.isMulti() ? multipliers[1] : 0);
      tournament.setMultiplier3(tournament.isMulti() ? multipliers[2] : 0);
      tournament.setStartTime(startTime);
      tournament.setDateFrom(dateFrom);
      tournament.setDateTo(dateTo);
      tournament.setPomId(selectedPom);
      tournament.setLocations(allLocations);
    }
    tournament.setClosed(closed);
  }

  public void resetValues ()
  {
    tourneyId = -1;
    tournamentName = "";
    type = Tournament.TYPE.SINGLE_GAME;
    maxBrokers = 0;
    maxAgents = 2;
    size1 = 8;
    size2 = 4;
    size3 = 2;
    multiplier1 = 1;
    multiplier2 = 1;
    multiplier3 = 1;
    startTime = Utils.offsetDate();
    Calendar initTime = Calendar.getInstance();
    initTime.set(2009, Calendar.MARCH, 3, 0, 0, 0);
    dateFrom = new Date();
    dateFrom.setTime(initTime.getTimeInMillis());
    initTime.set(2011, Calendar.MARCH, 3, 0, 0, 0);
    dateTo = new Date();
    dateTo.setTime(initTime.getTimeInMillis());
    selectedPom = 0;
    locations = new ArrayList<String>();
    closed = false;

    disabled = new boolean[13];
    Arrays.fill(disabled, Boolean.FALSE);
  }

  public void refresh ()
  {
  }

  //<editor-fold desc="Setters and getters">
  public int getTourneyId() {
    return tourneyId;
  }
  public void setTourneyId(int tourneyId) {
    this.tourneyId = tourneyId;
  }

  public String getTournamentName () {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName) {
    this.tournamentName = tournamentName;
  }

  public Tournament.TYPE getType () {
    return type;
  }
  public void setType (Tournament.TYPE type) {
    this.type = type;
  }

  public int getMaxBrokers () {
    return maxBrokers;
  }
  public void setMaxBrokers (int maxBrokers) {
    this.maxBrokers = maxBrokers;
  }

  public int getMaxAgents() {
    return maxAgents;
  }
  public void setMaxAgents(int maxAgents) {
    this.maxAgents = maxAgents;
  }

  public int getSize1 () {
    return size1;
  }
  public void setSize1 (int size1) {
    this.size1 = size1;
  }

  public int getSize2 () {
    return size2;
  }
  public void setSize2 (int size2) {
    this.size2 = size2;
  }

  public int getSize3 () {
    return size3;
  }
  public void setSize3 (int size3) {
    this.size3 = size3;
  }

  public int getMultiplier1() {
    return multiplier1;
  }
  public void setMultiplier1(int multiplier1) {
    this.multiplier1 = multiplier1;
  }

  public int getMultiplier2() {
    return multiplier2;
  }
  public void setMultiplier2(int multiplier2) {
    this.multiplier2 = multiplier2;
  }

  public int getMultiplier3() {
    return multiplier3;
  }
  public void setMultiplier3(int multiplier3) {
    this.multiplier3 = multiplier3;
  }

  public Date getStartTime () {
    return startTime;
  }
  public void setStartTime (Date startTime) {
    this.startTime = startTime;
  }

  public Date getDateFrom() {
    return dateFrom;
  }
  public void setDateFrom(Date dateFrom) {
    this.dateFrom = dateFrom;
  }

  public Date getDateTo() {
    return dateTo;
  }
  public void setDateTo(Date dateTo) {
    this.dateTo = dateTo;
  }

  public int getSelectedPom () {
    return selectedPom;
  }
  public void setSelectedPom (int selectedPom) {
    this.selectedPom = selectedPom;
  }

  public List<String> getLocations () {
    return locations;
  }
  public void setLocations (List<String> locations) {
    this.locations = locations;
  }

  public String getSortColumn () {
    return sortColumn;
  }
  public void setSortColumn (String sortColumn) {

    this.sortColumn = sortColumn;
  }

  public boolean isSortAscending () {
    return sortAscending;
  }
  public void setSortAscending (boolean sortAscending) {
    this.sortAscending = sortAscending;
  }

  public boolean[] getDisabled() {
    return disabled;
  }
  public void setDisabled(boolean[] disabled) {
    this.disabled = disabled;
  }

  public boolean isClosed() {
    return closed;
  }
  public void setClosed(boolean closed) {
    this.closed = closed;
  }
  //</editor-fold>
}
