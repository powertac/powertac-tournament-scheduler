package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.Iterator;
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

  public List<Tournament> getAvailableTournaments (Broker b)
  {
    return b.getAvailableTournaments();
  }

  public List<Tournament> getRegisteredTournaments (Broker b)
  {
    return b.getRegisteredTournaments();
  }

  public String getLogins (int brokerId)
  {
    String result = "";

    List<Long> logins = Cache.brokerLogins.get(brokerId);
    if (logins == null) {
      return "";
    }

    Iterator<Long> iter = logins.iterator();
    while (iter.hasNext()) {
      int stamp = (int) (System.currentTimeMillis() - iter.next()) / 1000;
      if (stamp > 900) {
        iter.remove();
      } else {
        result += stamp + " ";
      }
    }

    return result;
  }

  public String getHeartbeat (int gameId)
  {
    String[] messages = Cache.gameHeartbeats.get(gameId);
    if (messages == null) {
      return "";
    }

    try {
      int ago = (int)
          (System.currentTimeMillis() - Long.parseLong(messages[1])) / 1000;
      return messages[0] +" ("+ ago + ")";
    }
    catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  public void register (Broker b)
  {
    if (!(b.getSelectedTourneyRegister() > 0)) {
      return;
    }

    boolean registered = b.register(b.getSelectedTourneyRegister());
    if (!registered) {
      String msg = "Error registering broker";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("formDatabrokers", fm);
    } else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
  }

  public void unregister (Broker b)
  {
    if (!(b.getSelectedTourneyUnregister() > 0)) {
      return;
    }

    boolean registered = b.unregister(b.getSelectedTourneyUnregister());
    if (!registered) {
      String msg = "Error unregistering broker";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,msg, null);
      FacesContext.getCurrentInstance().addMessage("formDatabrokers", fm);
    } else {
      brokerList = Broker.getBrokerList();
      User user = User.getCurrentUser();
      User.reloadUser(user);
    }
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
      if (tournament.isSingle()) {
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

  public void abortGame (Game game)
  {
    log.info("Trying to abort game: " + game.getGameId());

    Cache.removeSim(game.getGameId());

    new RunAbort(game.getMachine().getMachineName());
  }

  public void killGame(Game game)
  {
    log.info("Trying to kill game: " + game.getGameId());

    int gameId = game.getGameId();
    int machineId = game.getMachine().getMachineId();
    Machine machine = game.getMachine();
    String machineName = game.getMachine().getMachineName();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      // Reset game and machine on TM
      if (game.isBooting()) {
        log.info("Resetting boot game: " + gameId + " on machine: " + machineId);

        game.setStatus(Game.STATE.boot_pending.toString());
        game.removeBootFile();
      }
      else if (game.isRunning()) {
        log.info("Resetting sim game: " + gameId + " on machine: " + machineId);

        game.setStatus(Game.STATE.boot_complete.toString());
        for (Agent agent: game.getAgentMap().values()) {
          agent.setStatus(Agent.STATE.pending.toString());
          agent.setBalance(0);
          session.update(agent);
        }
      }

      game.setReadyTime(null);
      game.setMachine(null);
      session.update(game);
      Machine.delayedMachineUpdate(machine, 300);

      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      log.error("Failed to completely kill game: " + gameId);
      String msg = "Error killing game : " + gameId;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
    finally {
      session.close();
    }

    // Kill the job on Jenkins and the slave
    new RunKill(machineName);
    Cache.removeSim(gameId);
    Cache.removeBootstrap(gameId);
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

        Cache.removeBootstrap(gameId);
      }
      if (game.stateEquals(Game.STATE.game_failed)) {
        log.info("Resetting sim game: " + gameId);
        game.setStatus(Game.STATE.boot_complete.toString());

        Cache.removeSim(gameId);

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