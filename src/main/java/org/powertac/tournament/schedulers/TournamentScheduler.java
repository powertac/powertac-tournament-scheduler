package org.powertac.tournament.schedulers;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Tournament;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;
import org.powertac.tournament.states.TournamentState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class TournamentScheduler
{
  private static Logger log = Utils.getLogger();

  private Tournament tournament;
  private int tournamentId;

  public TournamentScheduler (Tournament tournament)
  {
    this.tournament = tournament;
    tournamentId = tournament.getTournamentId();
  }

  public boolean createTournament (List<Level> levels)
  {
    boolean reset = false;

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(tournament);
      createLevels(session, levels);
      // Create first round(s) so brokers can register
      scheduleLevel(session);
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
      if (transaction.getStatus() == TransactionStatus.COMMITTED) {
        log.info(String.format("Created tournament %s", tournamentId));
        reset = true;
      }
      session.close();
    }

    return reset;
  }

  public boolean scheduleTournament ()
  {
    log.info("Scheduling tournament : " + tournamentId);

    boolean reset = false;

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (scheduleNextLevel(session)) {
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
      if (transaction.getStatus() == TransactionStatus.COMMITTED) {
        log.info("Next level scheduled for tournament " + tournamentId);
        Utils.growlMessage("Notice",
            "Level scheduled, edit and then manually load the rounds(s).");
        reset = true;
      }
      session.close();
    }

    return reset;
  }

  public boolean closeTournament ()
  {
    log.info("Closing tournament : " + tournamentId);

    boolean reset = false;

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournament.setState(TournamentState.closed);
      session.saveOrUpdate(tournament);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      log.error("Error closing tournament " + tournamentId);
      e.printStackTrace();
      Utils.growlMessage("Failed to close the tournament.");
    }
    finally {
      if (transaction.getStatus() == TransactionStatus.COMMITTED) {
        Utils.growlMessage("Notice",
            "Tournament closed, schedule next level when done editing");
        reset = true;
      }
      session.close();
    }

    return reset;
  }

  public boolean completeTournament ()
  {
    log.info("Completing tournament : " + tournamentId);

    boolean reset = false;

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (completeLevel()) {
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
      if (transaction.getStatus() == TransactionStatus.COMMITTED) {
        log.info(String.format("Level completed for tournament %s",
            tournamentId));
        if (tournament.getState().isComplete()) {
          Utils.growlMessage("Notice",
              "Level completed.<br/>Last level so tournament completed.");
        }
        else {
          Utils.growlMessage("Notice",
              "Level completed.<br/>Schedule next level when done editing");
        }
        reset = true;
      }
      session.close();
    }

    return reset;
  }

  private void createLevels (Session session, List<Level> levels)
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

  private boolean completeLevel ()
  {
    String oldState = tournament.getState().toString();


    switch (tournament.getState()) {
      case scheduled0: tournament.setState(TournamentState.completed0);
        break;
      case scheduled1: tournament.setState(TournamentState.completed1);
        break;
      case scheduled2: tournament.setState(TournamentState.completed2);
        break;
      case scheduled3: tournament.setState(TournamentState.completed3);
        break;
      default: log.error("CloseLevel : This shouldn't happen!");
        return false;
    }

    Level nextLevel = tournament.getNextLevel();
    if (nextLevel == null ||
        nextLevel.getNofRounds() == 0 || nextLevel.getNofWinners() == 0) {
      tournament.setState(TournamentState.complete);
    }

    log.info(String.format(
        "Changing state from %s to %s", oldState, tournament.getState()));
    return true;
  }

  private boolean scheduleNextLevel (Session session)
  {
    String oldState = tournament.getState().toString();

    switch (tournament.getState()) {
      case closed: tournament.setState(TournamentState.scheduled0);
        break;
      case completed0: tournament.setState(TournamentState.scheduled1);
        break;
      case completed1: tournament.setState(TournamentState.scheduled2);
        break;
      case completed2: tournament.setState(TournamentState.scheduled3);
        break;
      default:
        log.error("ScheduleNextlevel : This shouldn't happen!");
        return false;
    }

    // Levels, rounds are already scheduled during tournament creation
    // Brokers are already scheduled via registering for the tournament
    if (tournament.getState() != TournamentState.scheduled0) {
      scheduleLevel(session);
      scheduleBrokers(session);
    }

    log.info(String.format(
        "Changing state from %s to %s", oldState, tournament.getState()));
    return true;
  }

  private void scheduleLevel (Session session)
  {
    Level level = tournament.getCurrentLevel();

    log.info("Scheduling rounds for level " + level.getLevelNr());

    for (int i = 0; i < level.getNofRounds(); i++) {
      String roundName = String.format("%s_%s%s%s",
          tournament.getTournamentName(), level.getLevelName(),
          level.getNofRounds() == 1 ? "" : "_",
          level.getNofRounds() == 1 ? "" : i);
      scheduleRound(session, roundName, level);
    }
  }

  private void scheduleRound (Session session, String name, Level level)
  {
    log.info("Creating round : " + name);

    int nofBrokers = Math.max(100, level.getNofWinners());
    if (level.getLevelNr() != 0) {
      Level prevLevel = tournament.getPreviousLevel();
      nofBrokers = Math.min(prevLevel.getNofBrokers(), prevLevel.getNofWinners());
    }
    int maxBrokers = (int) Math.ceil(nofBrokers / level.getNofRounds());

    int size1 = level.getLevelNr() == 0 ? 1 : maxBrokers;
    int size2 = level.getLevelNr() == 0 ? 1 : Math.max(maxBrokers / 2, 2);
    int size3 = level.getLevelNr() == 0 ? 1 : Math.min(maxBrokers, 2);

    Date startDate = level.getStartTime();
    if (startDate.compareTo(Utils.offsetDate()) < 0) {
      startDate = Utils.offsetDate(1);
    }

    Round round = new Round();
    round.setRoundName(name);
    round.setLevel(level);
    round.setMaxBrokers(maxBrokers);
    round.setMaxAgents(level.getTournament().getMaxAgents());
    round.setSize1(size1);
    round.setSize2(size2);
    round.setSize3(size3);
    round.setMultiplier1(1);
    round.setMultiplier2(1);
    round.setMultiplier3(1);
    round.setStartTime(startDate);
    Location location = new Location();
    round.setDateFrom(location.getDateFrom());
    round.setDateTo(location.getDateTo());
    round.setLocations(location.getLocation() + ",");
    session.save(round);

    level.getRoundMap().put(round.getRoundId(), round);

    log.debug("Round created : " + round.getRoundId());
  }

  private void scheduleBrokers (Session session)
  {
    Level previousLevel = tournament.getPreviousLevel();
    Level level = tournament.getCurrentLevel();

    log.info("Scheduling brokers for level " + level.getLevelNr());

    // Loop through rounds, pick top winners
    int winnersPerRound =
        previousLevel.getNofWinners() / previousLevel.getNofRounds();
    List<Broker> winners = new ArrayList<>();
    for (Round round : previousLevel.getRoundMap().values()) {
      List<Broker> roundWinners = round.rankList();
      winners.addAll(roundWinners.subList(0,
          Math.min(winnersPerRound, roundWinners.size())));
    }

    log.debug("Winners from previous level : " + winners);

    // Randomly shuffle picked brokers into rounds via registering
    Random randomGenerator = new Random();
    int winnerCount = Math.min(previousLevel.getNofWinners(), winners.size());
    int brokersPerRound = winnerCount / level.getNofRounds();

    log.debug("winnerCount /  brokersPerRound " +
        winnerCount + " / " + brokersPerRound);

    for (Round round : level.getRoundMap().values()) {
      log.debug("Round : " + round.getRoundName());
      for (int i = 0; i < brokersPerRound; i++) {
        Broker broker = winners.remove(randomGenerator.nextInt(winners.size()));
        broker.registerForRound(session, round.getRoundId());
      }
    }
  }

  public boolean completingAllowed ()
  {
    if (!TournamentState.completingAllowed.contains(tournament.getState())) {
      return false;
    }

    // Loop over rounds, check all complete
    Level level = tournament.getCurrentLevel();
    for (Round round : level.getRoundMap().values()) {
      if (!round.getState().isComplete()) {
        return false;
      }
    }

    return true;
  }
}
