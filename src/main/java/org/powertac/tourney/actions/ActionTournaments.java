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
public class ActionTournaments
{
  private static Logger log = Logger.getLogger("TMLogger");

  private boolean disabled;
  private List<Broker> brokerList = new ArrayList<Broker>();
  private int slavesCount;

  private int tournamentId;
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
  private List<String> locations;
  private int selectedPom;
  private boolean closed;

  public ActionTournaments ()
  {
    resetValues();
  }

  public List<Tournament.TYPE> getTypes ()
  {
    return Arrays.asList(Tournament.TYPE.values());
  }

  public List<Tournament> getTournamentList ()
  {
    return Tournament.getNotCompleteTournamentList();
  }

  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public List<Location> getLocationList ()
  {
    return Location.getLocationList();
  }

  public void saveTournament ()
  {
    if (!inputsValidated()) {
      if (tournamentId != -1) {
        resetValues();
      }
      return;
    }

    if (tournamentId != -1) {
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
      tournament.setStateToPending();
      session.save(tournament);

      log.info(String.format("Created %s tournament %s",
          type.toString(), tournament.getTournamentId()));

      if (type == Tournament.TYPE.SINGLE_GAME) {
        Game game = Game.createGame(tournament, tournamentName);
        session.save(game);
        log.info("Created game " + game.getGameId());
      }

      transaction.commit();
    } catch (ConstraintViolationException ignored) {
      transaction.rollback();
      message(2, "The tournament name already exists");
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error creating tournament");
    } finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  public void loadTournament (Tournament tournament)
  {
    tournamentId = tournament.getTournamentId();
    tournamentName = tournament.getTournamentName();
    type = tournament.getType();
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
    locations = tournament.getLocationsList();
    selectedPom = tournament.getPomId();
    closed = tournament.isClosed();

    // Once scheduled, params can't change (type can never change)
    if (tournament.getSize() > 0) {
      disabled = true;
    }
  }

  public void saveEditedTournament ()
  {
    log.info("Saving tournament " + tournamentId);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = (Tournament) session.get(Tournament.class, tournamentId);
      setValues(tournament);
      session.saveOrUpdate(tournament);
      transaction.commit();
    } catch (ConstraintViolationException ignored) {
      transaction.rollback();
      message(2, "The tournament name already exists");
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error saving tournament");
    } finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  public void removeTournament (Tournament tournament)
  {
    if (!tournament.getTournamentName().toLowerCase().contains("test")) {
      log.info("Someone tried to remove a non-test Tournament!");
      message(0, "Nice try, hacker!");
      return;
    }

    String msg = tournament.delete();
    if (!msg.isEmpty()) {
      log.info(String.format("Something went wrong with removing tournament "
          + "%s\n%s", tournament.getTournamentName(), msg));
      message(0, msg);
    }
  }

  public void setValues (Tournament tournament)
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
    maxBrokers = Math.max(maxBrokers, tournament.getBrokerMap().size());

    if (tournament.getSize() < 1) {
      tournament.setTournamentName(tournamentName);
      if (type != null) {
        tournament.setType(type);
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
      tournament.setLocations(allLocations);
      tournament.setPomId(selectedPom);
    }
    tournament.setClosed(closed);
  }

  public void resetValues ()
  {
    tournamentId = -1;
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

  public List<Tournament> getAvailableTournaments (Broker b)
  {
    return b.getAvailableTournaments(false);
  }

  public List<Tournament> getRegisteredTournaments (Broker b)
  {
    return b.getRegisteredTournaments();
  }

  public void register (Broker b)
  {
    if (!(b.getSelectedTournamentRegister() > 0)) {
      return;
    }

    boolean registered = b.register(b.getSelectedTournamentRegister());
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
    if (!(b.getSelectedTournamentUnregister() > 0)) {
      return;
    }

    boolean registered = b.unregister(b.getSelectedTournamentUnregister());
    if (!registered) {
      message(1, "Error unregistering broker");
    } else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public boolean allowEdit (Tournament tournament)
  {
    return tournamentId == -1 && tournament.isPending();
  }

  private boolean inputsValidated ()
  {
    List<String> messages = new ArrayList<String>();

    if (tournamentName.trim().isEmpty()) {
      messages.add("The tournament name cannot be empty");
    }

    if ((locations.size() < 1) && (tournamentId == -1 || selectedPom != 0)) {
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
      FacesContext.getCurrentInstance().addMessage("runningTournaments", fm);
    } else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("tournamentRegistered", fm);
    } else if (field == 2) {
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
  }

  //<editor-fold desc="Setters and getters">
  public int getTournamentId ()
  {
    return tournamentId;
  }
  public void setTournamentId (int tournamentId)
  {
    this.tournamentId = tournamentId;
  }

  public String getTournamentName ()
  {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  public Tournament.TYPE getType ()
  {
    return type;
  }
  public void setType (Tournament.TYPE type)
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
