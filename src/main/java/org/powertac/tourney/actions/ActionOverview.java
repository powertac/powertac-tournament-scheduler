package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@ManagedBean
@RequestScoped
public class ActionOverview
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String sortColumnBrokers = null;
  private boolean sortAscendingBrokers = true;
  private String sortColumnTournaments = null;
  private boolean sortAscendingTournaments = true;
  private String sortColumnGames = null;
  private boolean sortAscendingGames = true;

  public ActionOverview()
  {
  }

  public List<Broker> getBrokerList ()
  {
    return Broker.getBrokerList();
  }

  public List<Tournament> getTournamentList ()
  {
    return Tournament.getTournamentList();
  }

  public List<Game> getGameList ()
  {
    return Game.getGameList();
  }

  public void startNow (Tournament t)
  {
    // Set startTime of game to now
    Database db = new Database();
    try {
      db.startTrans();

      // Set startTime of the tournament to now
      if (t.getStartTime().after(new Date())) {
        db.setTournamentStartTime(t.getTournamentId());
        String msg = "Setting tournament: " + t.getTournamentId() + " to start now";
        log.info(msg);
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
        FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
      }

      // Reload a loaded tourney, just to be sure
      Scheduler scheduler =
          (Scheduler) SpringApplicationContext.getBean("scheduler");
      if (!scheduler.isNullTourney()) {
          scheduler.reloadTournament();
      }
      else  {
        // Set all single games to now, multi will be handled by scheduler
        List<Game> games = db.getGamesInTourney(t.getTournamentId());
        for (Game g: games) {
          if (g.getStartTime().after(new Date())) {
            db.setGameStartTime(g.getGameId());
            String msg = "Setting game: " + g.getGameId() + " to start now";
            log.info(msg);
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
            FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
          }
        }
      }

      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      String msg = "Failed to set startTime to now";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
  }

  public void stopGame (Game game)
  {
    log.info("Trying to stop game: " + game.getGameId());

    int gameId = game.getGameId();
    int machineId = game.getMachineId();
    Database db = new Database();
    try {
      db.startTrans();

      // Kill the job on Jenkins and the slave
      new KillJob(db, game);

      // Reset game and machine on TM
      if (game.isBooting()) {
        log.info("Resetting boot game: " + gameId +" on machine: "+ machineId);
        game.removeBootFile();
        db.updateGameStatusById(gameId, Game.STATE.boot_pending);
        Scheduler scheduler =
            (Scheduler) SpringApplicationContext.getBean("scheduler");
        if (scheduler.checkedBootstraps.contains(gameId)) {
          scheduler.checkedBootstraps.remove(
              scheduler.checkedBootstraps.indexOf(gameId));
        }
      }
      else if (game.isRunning()) {
        log.info("Resetting sim game: " + gameId + " on machine: " + machineId);
        db.updateGameStatusById(gameId, Game.STATE.boot_complete);
        for (Broker broker: db.getBrokersInGame(gameId)) {
          db.updateAgentStatus(gameId, broker.getBrokerId(), Agent.STATE.pending);
        }
      }

      db.clearGameReadyTime(gameId);
      db.updateGameJmsUrlById(gameId, "");
      db.updateGameFreeMachine(gameId);
      delayMachineUpdate(machineId);

      db.commitTrans();
    }
    catch (Exception e) {
      e.printStackTrace();
      db.abortTrans();

      log.error("Failed to completely stop game: " + gameId);
      String msg = "Error stopping game : " + gameId;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
  }

  public void restartGame (Game game)
  {
    log.info("Trying to restart game: " + game.getGameId());

    int gameId = game.getGameId();
    Database db = new Database();
    try {
      db.startTrans();

      // Reset game and machine on TM
      if (game.stateEquals(Game.STATE.boot_failed)) {
        log.info("Resetting boot game: " + gameId);
        game.removeBootFile();
        db.updateGameStatusById(gameId, Game.STATE.boot_pending);
        Scheduler scheduler =
            (Scheduler) SpringApplicationContext.getBean("scheduler");
        if (scheduler.checkedBootstraps.contains(gameId)) {
          scheduler.checkedBootstraps.remove(
              scheduler.checkedBootstraps.indexOf(gameId));
        }
      }
      if (game.stateEquals(Game.STATE.game_failed)) {
        log.info("Resetting sim game: " + gameId);
        db.updateGameStatusById(gameId, Game.STATE.boot_complete);
      }

      db.clearGameReadyTime(gameId);
      db.updateGameJmsUrlById(gameId, "");
      db.commitTrans();
    }
    catch (Exception e) {
      e.printStackTrace();
      db.abortTrans();

      log.error("Failed to restart game: " + gameId);
      String msg = "Error restarting game : " + gameId;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg,null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
  }

  public void delayMachineUpdate (int machineId)
  {
    // We're delaying setting the machine to idle, because after a job kill,
    // the viz doesn't get an end-of-sim message. It takes a viz 2 mins to
    // recover from this. To be on the safe side, we delay for 5 mins.
    class updateThread implements Runnable {
      private int machineId;

      public updateThread(int machineId) {
        this.machineId = machineId;
      }

      public void run() {
        Utils.secondsSleep(300);

        Database db = new Database();
        try {
          db.startTrans();
          db.setMachineStatus(machineId, Machine.STATE.idle);
          db.commitTrans();
          log.info("Settig machine " + machineId + " to idle");
        }
        catch (SQLException e) {
          db.abortTrans();
          e.printStackTrace();
          log.error("Error updating machine status after job kill");
        }
      }
    }
    Runnable r = new updateThread(machineId);
    new Thread(r).start();
  }

  public void refresh ()
  {
  }

  //<editor-fold desc="Setters and Getters">
  public boolean isSortAscendingBrokers()
  {
    return sortAscendingBrokers;
  }
  public void setSortAscendingBrokers(boolean sortAscendingBrokers)
  {
    this.sortAscendingBrokers = sortAscendingBrokers;
  }

  public String getSortColumnBrokers()
  {
    return sortColumnBrokers;
  }
  public void setSortColumnBrokers(String sortColumnBrokers)
  {
    this.sortColumnBrokers = sortColumnBrokers;
  }

  public String getSortColumnTournaments ()
  {
    return sortColumnTournaments;
  }
  public void setSortColumnTournaments (String sortColumnTournaments)
  {
    this.sortColumnTournaments = sortColumnTournaments;
  }

  public boolean isSortAscendingTournaments ()
  {
    return sortAscendingTournaments;
  }
  public void setSortAscendingTournaments (boolean sortAscendingTournaments)
  {
    this.sortAscendingTournaments = sortAscendingTournaments;
  }

  public String getSortColumnGames ()
  {
    return sortColumnGames;
  }
  public void setSortColumnGames (String sortColumnGames)
  {
    this.sortColumnGames = sortColumnGames;
  }

  public boolean isSortAscendingGames ()
  {
    return sortAscendingGames;
  }
  public void setSortAscendingGames (boolean sortAscendingGames)
  {
    this.sortAscendingGames = sortAscendingGames;
  }
  //</editor-fold>
}
