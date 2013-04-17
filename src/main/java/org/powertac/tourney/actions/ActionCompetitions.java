package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.beans.Competition;
import org.powertac.tourney.beans.Level;
import org.powertac.tourney.beans.Pom;
import org.powertac.tourney.beans.Round;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionCompetitions
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int competitionId;
  private String competitionName;
  private int selectedPom;

  private int nofLevels = 4;
  private List<Level> levels;
  private boolean[] disabledArray;

  public ActionCompetitions ()
  {
    resetValues();
  }

  public List<Competition> getCompetitionList ()
  {
    return Competition.getNotCompleteCompetitionList();
  }

  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public List<String> getLevelInfo (Competition competition)
  {
    List<String> results = new ArrayList<String>();
    String base = "<a href=\"round.xhtml?roundId=%d\">%d</a> ";

    for (Level level: competition.getLevelMap().values()) {
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

  public String getLevelStyle (Competition competition, int levelNr)
  {
    if (competition.getCurrentLevelNr() == levelNr) {
      return "left running";
    } else {
      return "left";
    }
  }

  public void closeCompetition (Competition competition)
  {
    log.info("Closing competition : " + competition.getCompetitionId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      competition.setStateToClosed();
      session.saveOrUpdate(competition);

      // Also close round(s) of first level
      Level level = competition.getLevelMap().get(0);
      for (Round round : level.getRoundMap().values()) {
        round.setClosed(true);
        session.update(round);
      }

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      log.error("Error closing competition " + competition.getCompetitionId());
      e.printStackTrace();
      message(0, "Error closing the competition");
    } finally {
      message(0, "Competition closed, schedule next level when done editing");
      session.close();
    }
  }

  public void scheduleCompetition (Competition competition)
  {
    log.info("Scheduling competition : " + competition.getCompetitionId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (competition.scheduleNextLevel(session)) {
        session.saveOrUpdate(competition);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error scheduling next competition level");
      message(0, "Error scheduling next competition level");
    } finally {
      if (transaction.wasCommitted()) {
        log.info("Next level scheduled for competition "
            + competition.getCompetitionId());
        message(0, "Level scheduled, manually load the rounds(s)");
        resetValues();
      }
      session.close();
    }
  }

  public void completingCompetition (Competition competition)
  {
    log.info("Completing competition : " + competition.getCompetitionId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (competition.completeLevel()) {
        session.saveOrUpdate(competition);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error completing competition level");
    } finally {
      if (transaction.wasCommitted()) {
        log.info(String.format("Level completed for competition %s",
            competition.getCompetitionId()));
        if (competition.isComplete()) {
          message(0, "Level completed. Last level so competition completed.");
        }
        else {
          message(0, "Level completed, schedule next level when done editing");
        }
        resetValues();
      }
      session.close();
    }
  }

  public void saveCompetition ()
  {
    if (!inputsValidated()) {
      if (competitionId != -1) {
        resetValues();
      }
      return;
    }

    if (competitionId != -1) {
      updateCompetition();
    } else {
      createCompetition();
    }
  }

  private void createCompetition ()
  {
    log.info("Creating competition");

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    Competition competition = new Competition();
    try {
      setValues(session, competition);
      createLevels(session, competition);
      // Create first round(s) so brokers can register
      competition.scheduleRounds(session);
      transaction.commit();
    } catch (ConstraintViolationException ignored) {
      transaction.rollback();
      message(1, "The competition name already exists");
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error creating competition");
    } finally {
      if (transaction.wasCommitted()) {
        log.info(String.format("Created competition %s",
            competition.getCompetitionId()));
        resetValues();
      }
      session.close();
    }
  }

  public void loadCompetition (Competition competition)
  {
    competitionId = competition.getCompetitionId();
    competitionName = competition.getCompetitionName();
    selectedPom = competition.getPomId();
    int currentLevel = competition.getCurrentLevelNr();

    disabledArray = new boolean[competition.getLevelMap().size()];

    levels = new ArrayList<Level>();
    for (Level level: competition.getLevelMap().values()) {
      levels.add(level);

      if (currentLevel >= level.getLevelNr()) {
        disabledArray[level.getLevelNr()] = true;
      }
    }
  }

  public void updateCompetition ()
  {
    log.info("Saving competition " + competitionId);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Competition competition = (Competition) session.get(Competition.class, competitionId);
      setValues(session, competition);
      updateLevels(session, competition);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error updating competition");
      message(1, "Error updating competition");
    } finally {
      if (transaction.wasCommitted()) {
        resetValues();
      }
      session.close();
    }
  }

  private void setValues (Session session, Competition competition)
  {
    if (competitionId == -1) {
      competition.setCompetitionName(competitionName);
      competition.setPomId(selectedPom);
      session.saveOrUpdate(competition);
    }
  }

  private void updateLevels (Session session, Competition competition)
  {
    for (Level posted: levels) {
      Level level = competition.getLevelMap().get(posted.getLevelNr());

      if (level.getLevelNr() > competition.getCurrentLevelNr()) {
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

  private void createLevels (Session session, Competition competition)
  {
    for (Level level: levels) {
      log.info("Creating level " + level.getLevelNr()
          + " : " + level.getLevelName());
      level.setCompetitionId(competition.getCompetitionId());
      session.save(level);
      competition.getLevelMap().put(level.getLevelNr(), level);
    }
  }

  public void resetValues ()
  {
    competitionId = -1;
    competitionName = "";
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
  }

  public boolean editingAllowed (Competition competition)
  {
    return competition.editingAllowed() && competitionId == -1;
  }

  public boolean closingAllowed (Competition competition)
  {
    return competition.closingAllowed() && competitionId == -1;
  }

  public boolean schedulingAllowed (Competition competition)
  {
    return competition.schedulingAllowed() && competitionId == -1;
  }

  public boolean completingAllowed (Competition competition)
  {
    return competition.completingAllowed() && competitionId == -1;
  }

  private boolean inputsValidated ()
  {
    List<String> messages = new ArrayList<String>();

    if (competitionName.trim().isEmpty()) {
      messages.add("The competition name cannot be empty");
    }

    int previousWinners = -1;
    boolean previousUsed = false;
    for (Level level: levels) {
      int levelNr = level.getLevelNr();
      String levelName = level.getLevelName();

      if (levelNr == 0 && levelName.isEmpty()) {
        messages.add("A competition needs at least level 0");
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
          messages.add("The # winners of level " + levelNr + "  is smaller than 1");
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
      FacesContext.getCurrentInstance().addMessage("runningCompetitions", fm);
    } else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("saveCompetition", fm);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public int getCompetitionId ()
  {
    return competitionId;
  }
  public void setCompetitionId (int competitionId)
  {
    this.competitionId = competitionId;
  }

  public String getCompetitionName ()
  {
    return competitionName;
  }
  public void setCompetitionName (String competitionName)
  {
    this.competitionName = competitionName;
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