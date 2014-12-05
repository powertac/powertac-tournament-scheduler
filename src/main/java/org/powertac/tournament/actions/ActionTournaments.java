package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Pom;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Tournament;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
public class ActionTournaments implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  private int tournamentId;
  private String tournamentName;
  private int selectedPom;
  private int maxAgents;

  private int nofLevels = 4;
  private List<Level> levels;
  private boolean[] disabled;

  private List<Tournament> tournamentList;
  private List<Pom> pomList;
  private List<Broker> brokerList;

  public ActionTournaments ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    resetValues();
  }

  public void register (Broker b)
  {
    if (!(b.getRegisterTournamentId() > 0)) {
      return;
    }

    boolean registered = b.registerForTournament(b.getRegisterTournamentId());
    if (!registered) {
      Utils.growlMessage("Failed to register broker.");
    }
    else {
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
      Utils.growlMessage("Failed to unregister broker.");
    }
    else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public String getLevelInfo (Tournament tournament, int levelNr)
  {
    Level level = tournament.getLevelMap().get(levelNr);
    if (level == null) {
      return "";
    }

    String links = level.getNofRounds() + " / " + level.getNofWinners();
    if (level.getRoundMap().size() != 0) {
      links += " | ";
    }
    for (Round round : level.getRoundMap().values()) {
      links += String.format("<a href=\"round.xhtml?roundId=%d\">%d</a> ",
          round.getRoundId(), round.getRoundId());
    }
    return links;
  }

  public String getLevelStyle (Tournament tournament, int levelNr)
  {
    if (tournament.getCurrentLevelNr() == levelNr) {
      return "left running";
    }
    else {
      return "left";
    }
  }

  public void closeTournament (Tournament tournament)
  {
    log.info("Closing tournament : " + tournament.getTournamentId());

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournament.setStateToClosed();
      session.saveOrUpdate(tournament);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      log.error("Error closing tournament " + tournament.getTournamentId());
      e.printStackTrace();
      Utils.growlMessage("Failed to close the tournament.");
    }
    finally {
      Utils.growlMessage("Notice",
          "Tournament closed, schedule next level when done editing");
      session.close();
    }
  }

  public void scheduleTournament (Tournament tournament)
  {
    log.info("Scheduling tournament : " + tournament.getTournamentId());

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (tournament.scheduleNextLevel(session)) {
        session.saveOrUpdate(tournament);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error scheduling next tournament level");
      Utils.growlMessage("Failed to schedule next tournament level.");
    }
    finally {
      if (transaction.wasCommitted()) {
        log.info("Next level scheduled for tournament "
            + tournament.getTournamentId());
        Utils.growlMessage("Notice",
            "Level scheduled, edit and then manually load the rounds(s).");
        resetValues();
      }
      session.close();
    }
  }

  public void completingTournament (Tournament tournament)
  {
    log.info("Completing tournament : " + tournament.getTournamentId());

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (tournament.completeLevel()) {
        session.saveOrUpdate(tournament);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error completing tournament level");
    }
    finally {
      if (transaction.wasCommitted()) {
        log.info(String.format("Level completed for tournament %s",
            tournament.getTournamentId()));
        if (tournament.isComplete()) {
          Utils.growlMessage("Notice",
              "Level completed.<br/>Last level so tournament completed.");
        }
        else {
          Utils.growlMessage("Notice",
              "Level completed.<br/>Schedule next level when done editing");
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
    }
    else {
      createTournament();
    }
  }

  private void createTournament ()
  {
    log.info("Creating tournament");

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    Tournament tournament = new Tournament();
    try {
      setValues(session, tournament);
      createLevels(session, tournament);
      // Create first round(s) so brokers can register
      tournament.scheduleLevel(session);
      transaction.commit();
    }
    catch (ConstraintViolationException ignored) {
      transaction.rollback();
      Utils.growlMessage("The tournament name already exists.");
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error creating tournament");
    }
    finally {
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

    disabled = new boolean[tournament.getLevelMap().size()];
    levels = new ArrayList<Level>();
    for (Level level : tournament.getLevelMap().values()) {
      levels.add(level);

      if (currentLevel >= level.getLevelNr()) {
        disabled[level.getLevelNr()] = true;
      }
    }
  }

  public void updateTournament ()
  {
    log.info("Saving tournament " + tournamentId);

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = (Tournament) session.get(Tournament.class, tournamentId);
      setValues(session, tournament);
      updateLevels(session, tournament);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error updating tournament");
      Utils.growlMessage("Failed to update tournament.");
    }
    finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  private void setValues (Session session, Tournament tournament)
  {
    if (tournamentId == -1) {
      tournament.setTournamentName(tournamentName.trim().replace(" ", "_"));
      tournament.setPomId(selectedPom);
      tournament.setMaxAgents(maxAgents);
      session.saveOrUpdate(tournament);
    }
  }

  private void updateLevels (Session session, Tournament tournament)
  {
    for (Level posted : levels) {
      Level level = tournament.getLevelMap().get(posted.getLevelNr());

      if (level.getLevelNr() > tournament.getCurrentLevelNr()) {
        level.setLevelName(posted.getLevelName().trim().replace(" ", "_"));
        level.setNofRounds(posted.getNofRounds());
        level.setNofWinners(posted.getNofWinners());
        level.setStartTime(posted.getStartTime());
      }
      else {
        level.setLevelName(posted.getLevelName());
        level.setNofRounds(level.getRoundMap().size());
        level.setNofWinners(level.getMaxBrokers());
      }

      session.saveOrUpdate(level);
    }
  }

  private void createLevels (Session session, Tournament tournament)
  {
    for (Level level : levels) {
      level.setLevelName(level.getLevelName().trim().replace(" ", "_"));
      log.info("Creating level " + level.getLevelNr()
          + " : " + level.getLevelName());
      level.setTournament(tournament);
      session.save(level);
      tournament.getLevelMap().put(level.getLevelNr(), level);
    }
  }

  public void resetValues ()
  {
    tournamentId = -1;
    tournamentName = "";
    selectedPom = 0;
    maxAgents = 2;

    disabled = new boolean[nofLevels];

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

    tournamentList = Tournament.getNotCompleteTournamentList();
    pomList = Pom.getPomList();
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
    for (Level level : levels) {
      int levelNr = level.getLevelNr();
      String levelName = level.getLevelName();

      if (levelNr == 0 && levelName.isEmpty()) {
        messages.add("A tournament needs at least level 0");
      }

      if (levelName.isEmpty()) {
        if (level.getNofRounds() != 0) {
          messages.add("Level " + levelNr + " has rounds, but no name");
        }
        if (level.getNofWinners() != 0) {
          messages.add("Level " + levelNr + " has winners, but no name");
        }
      }
      else if (!levelName.isEmpty()) {
        if (level.getNofRounds() < 1) {
          messages.add("The # rounds of level " + levelNr + " is smaller than 1");
        }
        if (level.getNofWinners() < 1) {
          messages.add("The # winners of level " + levelNr + " is smaller than 1");
        }
        if (level.getNofRounds() > level.getNofWinners() &&
            level.getNofRounds() >= 1 && level.getNofWinners() >= 1) {
          messages.add("The # rounds of level " + levelNr + " is larger than the # of winners");
        }

        if (levelNr > 0) {
          if (!previousUsed) {
            messages.add("Level " + levelNr +
                " can't be used if level " + (levelNr - 1) + " is unused");
          }
          else {
            if (previousWinners < level.getNofWinners()) {
              messages.add("The # winners of level " + (levelNr - 1) +
                  " is smaller than the NOF winners of level " + levelNr);
            }

            if (level.getNofRounds() > 0 &&
                (previousWinners % level.getNofRounds()) != 0) {
              messages.add("The # rounds of level " + levelNr + " must be "
                  + "a multiple of the # of winners of level " + (levelNr - 1));
            }
          }
        }
      }

      previousWinners = level.getNofWinners();
      previousUsed = (!levelName.isEmpty() &&
          level.getNofRounds() > 0 &&
          level.getNofWinners() > 0);
    }

    for (String msg : messages) {
      Utils.growlMessage(msg);
    }

    return messages.size() == 0;
  }

  //<editor-fold desc="Collections">
  public List<Tournament> getTournamentList ()
  {
    return tournamentList;
  }

  public List<Pom> getPomList ()
  {
    return pomList;
  }

  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public List<Tournament> getAvailableTournaments (Broker broker)
  {
    return broker.getAvailableTournaments(tournamentList);
  }
  //</editor-fold>

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

  public int getMaxAgents ()
  {
    return maxAgents;
  }

  public void setMaxAgents (int maxAgents)
  {
    this.maxAgents = maxAgents;
  }

  public List<Level> getLevels ()
  {
    return levels;
  }

  public void setLevels (List<Level> levels)
  {
    this.levels = levels;
  }

  public boolean[] getDisabled ()
  {
    return disabled;
  }

  public void setDisabled (boolean[] disabled)
  {
    this.disabled = disabled;
  }

  public int getNofLevels ()
  {
    return nofLevels;
  }
  //</editor-fold>
}