package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tournament.beans.*;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionTournaments
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int tournamentId;
  private String tournamentName;
  private int selectedPom;

  private int nofLevels = 4;
  private List<Level> levels;
  private boolean[] disabledArray;
  private List<Broker> brokerList = new ArrayList<Broker>();

  public ActionTournaments ()
  {
    resetValues();
  }

  public List<Tournament> getTournamentList ()
  {
    return Tournament.getNotCompleteTournamentList();
  }

  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public void register (Broker b)
  {
    if (!(b.getRegisterTournamentId() > 0)) {
      return;
    }

    boolean registered = b.registerForTournament(b.getRegisterTournamentId());
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
    if (!(b.getUnregisterTournamentId() > 0)) {
      return;
    }

    boolean registered = b.unRegisterFromTournament(b.getUnregisterTournamentId());
    if (!registered) {
      message(1, "Error unregistering broker");
    } else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public List<String> getLevelInfo (Tournament tournament)
  {
    List<String> results = new ArrayList<String>();
    String base = "<a href=\"round.xhtml?roundId=%d\">%d</a> ";

    for (Level level: tournament.getLevelMap().values()) {
      String links = level.getNofRounds() + " / "+ level.getNofWinners();
      if (level.getRoundMap().size() != 0) {
        links += " | ";
      }
      for (Round round : level.getRoundMap().values()) {
        links += String.format(base, round.getRoundId(),
            round.getRoundId());
      }
      results.add(links);
    }

    while (results.size() < nofLevels) {
      results.add("");
    }

    return results;
  }

  public String getLevelStyle (Tournament tournament, int levelNr)
  {
    if (tournament.getCurrentLevelNr() == levelNr) {
      return "left running";
    } else {
      return "left";
    }
  }

  public void closeTournament (Tournament tournament)
  {
    log.info("Closing tournament : " + tournament.getTournamentId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournament.setStateToClosed();
      session.saveOrUpdate(tournament);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      log.error("Error closing tournament " + tournament.getTournamentId());
      e.printStackTrace();
      message(0, "Error closing the tournament");
    } finally {
      message(0, "Tournament closed, schedule next level when done editing");
      session.close();
    }
  }

  public void scheduleTournament (Tournament tournament)
  {
    log.info("Scheduling tournament : " + tournament.getTournamentId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (tournament.scheduleNextLevel(session)) {
        session.saveOrUpdate(tournament);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error scheduling next tournament level");
      message(0, "Error scheduling next tournament level");
    } finally {
      if (transaction.wasCommitted()) {
        log.info("Next level scheduled for tournament "
            + tournament.getTournamentId());
        message(0, "Level scheduled, edit and then manually load the rounds(s)");
        resetValues();
      }
      session.close();
    }
  }

  public void completingTournament (Tournament tournament)
  {
    log.info("Completing tournament : " + tournament.getTournamentId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (tournament.completeLevel()) {
        session.saveOrUpdate(tournament);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error completing tournament level");
    } finally {
      if (transaction.wasCommitted()) {
        log.info(String.format("Level completed for tournament %s",
            tournament.getTournamentId()));
        if (tournament.isComplete()) {
          message(0, "Level completed. Last level so tournament completed.");
        }
        else {
          message(0, "Level completed, schedule next level when done editing");
        }
        resetValues();
      }
      session.close();
    }
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
      updateTournament();
    } else {
      createTournament();
    }
  }

  private void createTournament ()
  {
    log.info("Creating tournament");

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    Tournament tournament = new Tournament();
    try {
      setValues(session, tournament);
      createLevels(session, tournament);
      // Create first round(s) so brokers can register
      tournament.scheduleLevel(session);
      transaction.commit();
    } catch (ConstraintViolationException ignored) {
      transaction.rollback();
      message(1, "The tournament name already exists");
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error creating tournament");
    } finally {
      if (transaction.wasCommitted()) {
        log.info(String.format("Created tournament %s",
            tournament.getTournamentId()));
        resetValues();
      }
      session.close();
    }
  }

  public void loadTournament (Tournament tournament)
  {
    tournamentId = tournament.getTournamentId();
    tournamentName = tournament.getTournamentName();
    selectedPom = tournament.getPomId();
    int currentLevel = tournament.getCurrentLevelNr();

    disabledArray = new boolean[tournament.getLevelMap().size()];
    levels = new ArrayList<Level>();
    for (Level level: tournament.getLevelMap().values()) {
      levels.add(level);

      if (currentLevel >= level.getLevelNr()) {
        disabledArray[level.getLevelNr()] = true;
      }
    }
  }

  public void updateTournament ()
  {
    log.info("Saving tournament " + tournamentId);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = (Tournament) session.get(Tournament.class, tournamentId);
      setValues(session, tournament);
      updateLevels(session, tournament);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error updating tournament");
      message(1, "Error updating tournament");
    } finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  private void setValues (Session session, Tournament tournament)
  {
    if (tournamentId == -1) {
      tournament.setTournamentName(tournamentName);
      tournament.setPomId(selectedPom);
      session.saveOrUpdate(tournament);
    }
  }

  private void updateLevels (Session session, Tournament tournament)
  {
    for (Level posted: levels) {
      Level level = tournament.getLevelMap().get(posted.getLevelNr());

      if (level.getLevelNr() > tournament.getCurrentLevelNr()) {
        level.setLevelName(posted.getLevelName());
        level.setNofRounds(posted.getNofRounds());
        level.setNofWinners(posted.getNofWinners());
        level.setStartTime(posted.getStartTime());
      }
      else {
        level.setNofRounds(level.getRoundMap().size());
        level.setLevelName(posted.getLevelName());
        level.setNofWinners(level.getMaxBrokers());
      }

      session.saveOrUpdate(level);
    }
  }

  private void createLevels (Session session, Tournament tournament)
  {
    for (Level level: levels) {
      log.info("Creating level " + level.getLevelNr()
          + " : " + level.getLevelName());
      level.setTournamentId(tournament.getTournamentId());
      session.save(level);
      tournament.getLevelMap().put(level.getLevelNr(), level);
    }
  }

  public void resetValues ()
  {
    tournamentId = -1;
    tournamentName = "";
    selectedPom = 0;

    disabledArray = new boolean[nofLevels];

    levels = new ArrayList<Level>();
    for (int i = 0; i < nofLevels; i++) {
      Level level = new Level();
      level.setLevelName("");
      level.setNofRounds(0);
      level.setNofWinners(0);
      level.setLevelNr(i);
      level.setStartTime(Utils.offsetDate(2));
      levels.add(level);
    }

    levels.get(0).setLevelName("qualifying");
    levels.get(0).setNofRounds(1);
    levels.get(0).setNofWinners(100);

    brokerList = Broker.getBrokerList();
  }

  public boolean editingAllowed (Tournament tournament)
  {
    return tournament.editingAllowed() && tournamentId == -1;
  }

  public boolean closingAllowed (Tournament tournament)
  {
    return tournament.closingAllowed() && tournamentId == -1;
  }

  public boolean schedulingAllowed (Tournament tournament)
  {
    return tournament.schedulingAllowed() && tournamentId == -1;
  }

  public boolean completingAllowed (Tournament tournament)
  {
    return tournament.completingAllowed() && tournamentId == -1;
  }

  private boolean inputsValidated ()
  {
    List<String> messages = new ArrayList<String>();

    if (tournamentName.trim().isEmpty()) {
      messages.add("The tournament name cannot be empty");
    }

    int previousWinners = -1;
    boolean previousUsed = false;
    for (Level level: levels) {
      int levelNr = level.getLevelNr();
      String levelName = level.getLevelName();

      if (levelNr == 0 && levelName.isEmpty()) {
        messages.add("A tournament needs at least level 0");
      }

      if (levelName.isEmpty()) {
        if (level.getNofRounds() != 0) {
          messages.add("Level " + levelNr + " has rounds, but no name");
        }
        if (level.getNofWinners() != 0)  {
          messages.add("Level " + levelNr + " has winners, but no name");
        }
      }
      else if (!levelName.isEmpty()) {
        if (level.getNofRounds() < 1) {
          messages.add("The # rounds of level " + levelNr + " is smaller than 1");
        }
        if (level.getNofWinners() < 1)  {
          messages.add("The # winners of level " + levelNr + " is smaller than 1");
        }
        if (level.getNofRounds() > level.getNofWinners() &&
            level.getNofRounds() >= 1 && level.getNofWinners() >= 1) {
          messages.add("The # rounds of level " + levelNr + " is larger than the # of winners");
        }

        if (levelNr > 0) {
          if (!previousUsed) {
            messages.add("Level " + levelNr +
                " can't be used if level " + (levelNr - 1)  + " is unused");
          }
          else {
            if (previousWinners < level.getNofWinners()) {
              messages.add("The # winners of level " + (levelNr-1) +
                  " is smaller than the NOF winners of level " + levelNr);
            }

            if (level.getNofRounds() > 0 &&
                (previousWinners % level.getNofRounds()) != 0) {
              messages.add("The # rounds of level " + levelNr + " must be "
                  + "a multiple of the # of winners of level " + (levelNr-1));
            }
          }
        }
      }

      previousWinners = level.getNofWinners();
      previousUsed = (!levelName.isEmpty() &&
                      level.getNofRounds() > 0 &&
                      level.getNofWinners() > 0);
    }

    for (String msg: messages) {
      message(1, msg);
    }

    return messages.size() == 0;
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("runningTournaments", fm);
    } else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("saveTournament", fm);
    }
  }

  //<editor-fold desc="Setters and Getters">
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

  public int getSelectedPom ()
  {
    return selectedPom;
  }
  public void setSelectedPom (int selectedPom)
  {
    this.selectedPom = selectedPom;
  }

  public List<Level> getLevels ()
  {
    return levels;
  }
  public void setLevels (List<Level> levels)
  {
    this.levels = levels;
  }

  public boolean[] getDisabledArray ()
  {
    return disabledArray;
  }
  public void setDisabledArray (boolean[] disabledArray)
  {
    this.disabledArray = disabledArray;
  }
  //</editor-fold>
}