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
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionOverview
{
  private static Logger log = Logger.getLogger("TMLogger");

  private List<Broker> brokerList = new ArrayList<Broker>();
  private List<Game> notCompleteGamesList = new ArrayList<Game>();
  private List<Round> notCompleteRoundList = new ArrayList<Round>();

  private boolean hideSample = false;
  private boolean hideInactive = true;

  public ActionOverview ()
  {
    loadData();
  }

  private void loadData ()
  {
    brokerList = Broker.getBrokerList();
    notCompleteGamesList = Game.getNotCompleteGamesList();
    notCompleteRoundList = Round.getNotCompleteRoundList();
  }

  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public List<Round> getNotCompleteRoundList ()
  {
    return notCompleteRoundList;
  }

  public List<Game> getNotCompleteGamesList ()
  {
    return notCompleteGamesList;
  }

  public String getBrokerState (int brokerId)
  {
    if (MemStore.getBrokerState(brokerId)) {
      return "enabled";
    } else {
      return "disabled";
    }
  }

  public void toggleState (int brokerId)
  {
    boolean enabled = true;

    try {
      enabled = MemStore.brokerState.get(brokerId);
    } catch (Exception ignored) {
    }

    MemStore.setBrokerState(brokerId, !enabled);
  }

  public void abortGame (Game game)
  {
    log.info("Trying to abort game: " + game.getGameId());

    new RunAbort(game.getMachine().getMachineName());

    message(2, "Aborting games takes some time, please wait");
  }

  public void killGame (Game game)
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

        game.setStateBootPending();
        game.removeBootFile();
      } else if (game.isRunning()) {
        log.info("Resetting sim game: " + gameId + " on machine: " + machineId);

        game.setStateBootComplete();
        for (Agent agent: game.getAgentMap().values()) {
          if (!agent.isPending()) {
            MemStore.setBrokerState(agent.getBrokerId(), false);
          }
          agent.setStatePending();
          agent.setBalance(0);
          session.update(agent);
        }
      }

      game.setReadyTime(null);
      game.setMachine(null);
      session.update(game);
      Machine.delayedMachineUpdate(machine, 300);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      log.error("Failed to completely kill game: " + gameId);
      message(2, "Error killing game : " + gameId);
    } finally {
      session.close();
    }

    // Kill the job on Jenkins and the slave
    new RunKill(machineName);
  }

  public void restartGame (Game game)
  {
    log.info("Trying to restart game: " + game.getGameId());

    int gameId = game.getGameId();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (game.isBootFailed()) {
        log.info("Resetting boot game: " + gameId);
        game.removeBootFile();
        game.setStateBootPending();
      }
      if (game.isGameFailed()) {
        log.info("Resetting sim game: " + gameId);
        game.setStateBootComplete();

        for (Agent agent: game.getAgentMap().values()) {
          agent.setStatePending();
          session.update(agent);
        }
        session.flush();
      }

      game.setMachine(null);
      session.update(game);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      log.error("Failed to restart game: " + gameId);
      message(2, "Error restarting game : " + gameId);
    }
    session.close();
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("brokersForm", fm);
    } else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("roundForm", fm);
    } else if (field == 2) {
      FacesContext.getCurrentInstance().addMessage("gamesForm", fm);
    }
  }

  public boolean isHideSample ()
  {
    return hideSample;
  }
  public void setHideSample (boolean hideSample)
  {
    this.hideSample = hideSample;
  }

  public boolean isHideInactive ()
  {
    return hideInactive;
  }
  public void setHideInactive (boolean hideInactive)
  {
    this.hideInactive = hideInactive;
  }
}