package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.beans.Competition;
import org.powertac.tourney.beans.CompetitionRound;
import org.powertac.tourney.beans.Pom;
import org.powertac.tourney.beans.Tournament;
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

  private int nofRounds = 4;
  private List<CompetitionRound> rounds;
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

  public List<String> getRoundInfo (Competition competition)
  {
    List<String> results = new ArrayList<String>();
    String base = "<a href=\"tournament.xhtml?tournamentId=%d\">%d</a> ";

    for (CompetitionRound round: competition.getRoundMap().values()) {
      String links = round.getNofTournaments() + " / "+ round.getNofWinners();
      if (round.getTournamentMap().size() != 0) {
        links += " | ";
      }
      for (Tournament tournament: round.getTournamentMap().values()) {
        links += String.format(base, tournament.getTournamentId(),
            tournament.getTournamentId());
      }
      results.add(links);
    }

    while (results.size() < nofRounds) {
      results.add("");
    }

    return results;
  }

  public String getCompetitionRound (Competition competition, int roundNr)
  {
    if (competition.getCurrentRoundNr() == roundNr) {
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

      // Also close tournaments of first round
      CompetitionRound round = competition.getRoundMap().get(0);
      for (Tournament tournament: round.getTournamentMap().values()) {
        tournament.setClosed(true);
        session.update(tournament);
      }

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      log.error("Error closing competition " + competition.getCompetitionId());
      e.printStackTrace();
      message(0, "Error closing the competition");
    } finally {
      message(0, "Competition closed, schedule next round when done editing");
      session.close();
    }
  }

  public void scheduleCompetition (Competition competition)
  {
    log.info("Scheduling competition : " + competition.getCompetitionId());

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (competition.scheduleNextRound(session)) {
        session.saveOrUpdate(competition);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error scheduling next competition round");
      message(0, "Error scheduling next competition round");
    } finally {
      if (transaction.wasCommitted()) {
        log.info("Next round scheduled for competition "
            + competition.getCompetitionId());
        message(0, "Round scheduled, manually load the tournament(s)");
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
      if (competition.completeRound()) {
        session.saveOrUpdate(competition);
        transaction.commit();
      }
      else {
        transaction.rollback();
      }
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error completing competition round");
    } finally {
      if (transaction.wasCommitted()) {
        log.info(String.format("Round completed for competition %s",
            competition.getCompetitionId()));
        if (competition.isComplete()) {
          message(0, "Round completed. Last round so competition completed.");
        }
        else {
          message(0, "Round completed, schedule next round when done editing");
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
      createRounds(session, competition);
      // Create first tournament(s) so brokers can register
      competition.scheduleTournaments(session);
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
    int currentRound = competition.getCurrentRoundNr();

    disabledArray = new boolean[competition.getRoundMap().size()];

    rounds = new ArrayList<CompetitionRound>();
    for (CompetitionRound round: competition.getRoundMap().values()) {
      rounds.add(round);

      if (currentRound >= round.getRoundNr()) {
        disabledArray[round.getRoundNr()] = true;
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
      updateRounds(session, competition);
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

  private void updateRounds (Session session, Competition competition)
  {
    for (CompetitionRound posted: rounds) {
      CompetitionRound round = competition.getRoundMap().get(posted.getRoundNr());

      if (round.getRoundNr() > competition.getCurrentRoundNr()) {
        round.setRoundName(posted.getRoundName());
        round.setNofTournaments(posted.getNofTournaments());
        round.setNofWinners(posted.getNofWinners());
        round.setStartTime(posted.getStartTime());
      }
      else {
        round.setNofTournaments(round.getTournamentMap().size());
        round.setRoundName(posted.getRoundName());
        round.setNofWinners(round.getMaxBrokers());
      }

      session.saveOrUpdate(round);
    }
  }

  private void createRounds (Session session, Competition competition)
  {
    for (CompetitionRound round: rounds) {
      log.info("Creating round " + round.getRoundNr()
          + " : " + round.getRoundName());
      round.setCompetitionId(competition.getCompetitionId());
      session.save(round);
      competition.getRoundMap().put(round.getRoundNr(), round);
    }
  }

  public void resetValues ()
  {
    competitionId = -1;
    competitionName = "";
    selectedPom = 0;

    disabledArray = new boolean[nofRounds];

    rounds = new ArrayList<CompetitionRound>();
    for (int i = 0; i < nofRounds; i++) {
      CompetitionRound round = new CompetitionRound();
      round.setRoundName("");
      round.setNofTournaments(0);
      round.setNofWinners(0);
      round.setRoundNr(i);
      round.setStartTime(Utils.offsetDate(2));
      rounds.add(round);
    }

    rounds.get(0).setRoundName("qualifying");
    rounds.get(0).setNofTournaments(1);
    rounds.get(0).setNofWinners(100);
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
    for (CompetitionRound round: rounds) {
      int roundNr = round.getRoundNr();
      String roundName = round.getRoundName();

      if (roundNr == 0 && roundName.isEmpty()) {
        messages.add("A competition needs at least round 0");
      }

      if (roundName.isEmpty()) {
        if (round.getNofTournaments() != 0) {
          messages.add("Round " + roundNr + " has tournaments, but no name");
        }
        if (round.getNofWinners() != 0)  {
          messages.add("Round " + roundNr + " has winners, but no name");
        }
      }
      else if (!roundName.isEmpty()) {
        if (round.getNofTournaments() < 1) {
          messages.add("The # tournaments of round " + roundNr + " is smaller than 1");
        }
        if (round.getNofWinners() < 1)  {
          messages.add("The # winners of round " + roundNr + "  is smaller than 1");
        }

        if (roundNr > 0) {
          if (!previousUsed) {
            messages.add("Round " + roundNr +
                " can't be used if round " + (roundNr - 1)  + " is unused");
          }
          else {
            if (previousWinners < round.getNofWinners()) {
              messages.add("The # winners of round " + (roundNr-1) +
                  " is smaller than the NOF winners of round " + roundNr);
            }

            if (round.getNofTournaments() > 0 &&
                (previousWinners % round.getNofTournaments()) != 0) {
              messages.add("The # tournaments of round " + roundNr + " must be "
                  + "a multiple of the # of winners of round " + (roundNr-1));
            }
          }
        }
      }

      previousWinners = round.getNofWinners();
      previousUsed = (!roundName.isEmpty() &&
                      round.getNofTournaments() > 0 &&
                      round.getNofWinners() > 0);
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

  public List<CompetitionRound> getRounds ()
  {
    return rounds;
  }
  public void setRounds (List<CompetitionRound> rounds)
  {
    this.rounds = rounds;
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