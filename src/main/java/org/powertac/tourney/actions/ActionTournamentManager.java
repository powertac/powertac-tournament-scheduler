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

  private int selectedPom;

  private Date startTime = new Date();
  private Date dateFrom = new Date();
  private Date dateTo = new Date();

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
    dateFrom.setTime(initTime.getTimeInMillis());
    initTime.set(2011, Calendar.MARCH, 3);
    dateTo.setTime(initTime.getTimeInMillis());
  }

  public List<Tournament.TYPE> getTypes ()
  {
    return Arrays.asList(Tournament.TYPE.values());
  }

  public List<Tournament> getTournamentList()
  {
    return Tournament.getNotCompleteTournamentList();
  }

  public synchronized void createTournament ()
  {
    log.info("Creating " + type.toString() + " tournament");

    String allLocations = "";
    for (String s: locations) {
      allLocations += s + ",";
    }

    boolean created = createTournament(allLocations);
    if (created) {
      tournamentName = "";
      maxBrokers = 0;
      maxAgents = 2;
      size1 = 2;
      size2 = 4;
      size3 = 8;
      type = Tournament.TYPE.SINGLE_GAME;
      locations = new ArrayList<String>();
    }
  }

  private boolean createTournament (String allLocations)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = new Tournament();
      tournament.setTournamentName(tournamentName);
      tournament.setStartTime(Utils.offsetDate(startTime));
      tournament.setDateFrom(dateFrom);
      tournament.setDateTo(dateTo);
      tournament.setType(type.toString());
      tournament.setPomId(selectedPom);
      tournament.setLocations(allLocations);
      tournament.setMaxBrokers(maxBrokers);
      tournament.setMaxAgents(maxAgents);
      tournament.setSize1((type==Tournament.TYPE.MULTI_GAME) ? size1 : 0);
      tournament.setSize2((type==Tournament.TYPE.MULTI_GAME) ? size2 : 0);
      tournament.setSize3((type==Tournament.TYPE.MULTI_GAME) ? size3 : 0);
      session.save(tournament);

      log.info(String.format("Created %s tournament %s",
          type.toString(), tournament.getTournamentId()));

      if (type == Tournament.TYPE.SINGLE_GAME) {
        Game game = Game.createGame(tournament, tournamentName);
        session.save(game);
        log.info("Created game " + game.getGameId());
      }

      transaction.commit();
      return true;
    }

    catch (ConstraintViolationException ignored) {
      transaction.rollback();
      String msg = "The tournament name already exists";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
      return false;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Scheduling exception (" + type.toString() + " tournament) !");
      return false;
    }
    finally {
      session.close();
    }
  }

  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public List<Location> getLocationList()
  {
    return Location.getLocationList();
  }

  public void removeTournament(Tournament tournament)
  {
    if (tournament == null) {
      return;
    }

    if (!tournament.getTournamentName().toLowerCase().contains("test")) {
      log.info("Someone tried to remove a non-test Tournament!");
      String msg = "Nice try, hacker!" ;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
      FacesContext.getCurrentInstance().addMessage("removeTournament", fm);
      return;
    }

    //String msg = tournament.remove();
    String msg = tournament.delete();
    if (!msg.isEmpty()) {
      log.info(String.format("Something went wrong with removing tournament "
          + "%s\n%s", tournament.getTournamentName(), msg));
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
      FacesContext.getCurrentInstance().addMessage("removeTournament", fm);
    }
  }

  public void refresh ()
  {
  }

  //<editor-fold desc="Setters and getters">
  public List<String> getLocations () {
    return locations;
  }
  public void setLocations (List<String> locations) {
    this.locations = locations;
  }

  public int getSelectedPom () {
    return selectedPom;
  }
  public void setSelectedPom (int selectedPom) {
    this.selectedPom = selectedPom;
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

  public int getMaxAgents() {
    return maxAgents;
  }
  public void setMaxAgents(int maxAgents) {
    this.maxAgents = maxAgents;
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

  public Tournament.TYPE getType () {
    return type;
  }
  public void setType (Tournament.TYPE type) {
    this.type = type;
  }

  public Date getStartTime () {
    return startTime;
  }
  public void setStartTime (Date startTime) {
    this.startTime = startTime;
  }

  public int getMaxBrokers () {
    return maxBrokers;
  }
  public void setMaxBrokers (int maxBrokers) {
    this.maxBrokers = maxBrokers;
  }

  public String getTournamentName () {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName) {
    this.tournamentName = tournamentName;
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
  //</editor-fold>
}
