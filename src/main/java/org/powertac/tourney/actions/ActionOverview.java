package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.KillJob;
import org.powertac.tourney.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@RequestScoped
public class ActionOverview
{
  private static Logger log = Logger.getLogger("TMLogger");

  private List<Broker> brokerList = new ArrayList<Broker>();
  private List<Game> notCompleteGamesList = new ArrayList<Game>();
  private List<Tournament> notCompleteTournamentList = new ArrayList<Tournament>();

  public ActionOverview()
  {
    loadData();
  }

  private void loadData ()
  {
    brokerList = Broker.getBrokerList();
    notCompleteGamesList = Game.getNotCompleteGamesList();
    notCompleteTournamentList = Tournament.getNotCompleteTournamentList();
  }

  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public List<Tournament> getNotCompleteTournamentList ()
  {
    return notCompleteTournamentList;
  }

  public List<Game> getNotCompleteGamesList ()
  {
    return notCompleteGamesList;
  }

  public void startNow (Tournament tournament)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournament.setStartTime(Utils.offsetDate());
      session.update(tournament);
      session.flush();

      // Reschedule all games of a SINGLE_GAME tournament
      if (tournament.typeEquals(Tournament.TYPE.SINGLE_GAME)) {
        for (Game game: tournament.getGameMap().values()) {
          game.setStartTime(Utils.offsetDate());
          session.update(game);
          session.flush();

          String msg = "Setting game: " + game.getGameId() + " to start now";
          log.info(msg);
          FacesMessage fm =
              new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
          FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
        }
      }

      String msg =
          "Setting tournament: "+ tournament.getTournamentId() +" to start now";
      log.info(msg);
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);

      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      String msg =
          "Failed to start tournament "+ tournament.getTournamentId() +" to now";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
    session.close();

    // If a MULTI_GAME tournament is loaded, just reload
    Scheduler scheduler = Scheduler.getScheduler();
    if (!scheduler.isNullTourney() &&
        tournament.getTournamentId() == scheduler.getRunningTournament().getTournamentId()) {
      scheduler.reloadTournament();
    }
  }

  public void stopGame (Game game)
  {
    log.info("Trying to stop game: " + game.getGameId());

    int gameId = game.getGameId();
    int machineId = game.getMachine().getMachineId();
    Scheduler scheduler = Scheduler.getScheduler();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      // Kill the job on Jenkins and the slave
      new KillJob(game.getMachine());

      // Reset game and machine on TM
      if (game.isBooting()) {
        log.info("Resetting boot game: " + gameId + " on machine: " + machineId);

        game.removeBootFile();
        game.setStatus(Game.STATE.boot_pending.toString());

        List<Integer> checkedBoots = scheduler.getCheckedBootstraps();
        if (checkedBoots.contains(gameId)) {
          int index = checkedBoots.indexOf(gameId);
          checkedBoots.remove(index);
          scheduler.setCheckedBootstraps(checkedBoots);
        }
      }
      else if (game.isRunning()) {
        log.info("Resetting sim game: " + gameId + " on machine: " + machineId);

        game.setStatus(Game.STATE.boot_complete.toString());

        for (Agent agent: game.getAgentMap().values()) {
          agent.setStatus(Agent.STATE.pending.toString());
          session.update(agent);
        }

        List<Integer> checkedSims = scheduler.getCheckedSims();
        if (checkedSims.contains(gameId)) {
          int index = checkedSims.indexOf(gameId);
          checkedSims.remove(index);
          scheduler.setCheckedSims(checkedSims);
        }
      }

      game.setReadyTime(null);
      game.setMachine(null);
      session.update(game);
      transaction.commit();

      Machine.delayedMachineUpdate(machineId, 300);
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      log.error("Failed to completely stop game: " + gameId);
      String msg = "Error stopping game : " + gameId;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
    session.close();
  }

  public void restartGame (Game game)
  {
    log.info("Trying to restart game: " + game.getGameId());

    int gameId = game.getGameId();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (game.stateEquals(Game.STATE.boot_failed)) {
        log.info("Resetting boot game: " + gameId);
        game.removeBootFile();
        game.setStatus(Game.STATE.boot_pending.toString());

        Scheduler scheduler = Scheduler.getScheduler();
        List<Integer> checkedBoots = scheduler.getCheckedBootstraps();
        if (checkedBoots.contains(gameId)) {
          int index = checkedBoots.indexOf(gameId);
          checkedBoots.remove(index);
          scheduler.setCheckedBootstraps(checkedBoots);
        }
      }
      if (game.stateEquals(Game.STATE.game_failed)) {
        log.info("Resetting sim game: " + gameId);
        game.setStatus(Game.STATE.boot_complete.toString());

        Scheduler scheduler = Scheduler.getScheduler();
        List<Integer> checkedSims = scheduler.getCheckedSims();
        if (checkedSims.contains(gameId)) {
          int index = checkedSims.indexOf(gameId);
          checkedSims.remove(index);
          scheduler.setCheckedSims(checkedSims);
        }

        for (Agent agent: game.getAgentMap().values()) {
          agent.setStatus(Agent.STATE.pending.toString());
          session.update(agent);
        }
        session.flush();
      }

      game.setMachine(null);
      session.update(game);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      log.error("Failed to restart game: " + gameId);
      String msg = "Error restarting game : " + gameId;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg,null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
    session.close();
  }

  public void refresh ()
  {
  }
}