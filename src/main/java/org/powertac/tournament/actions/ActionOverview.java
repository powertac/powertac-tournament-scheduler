package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.*;
import org.powertac.tournament.services.*;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.util.List;


@ManagedBean
public class ActionOverview  implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  private List<Broker> brokerList;
  private List<Game> notCompleteGamesList;
  private List<Round> notCompleteRoundList;

  public ActionOverview ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    brokerList = Broker.getBrokerList();
    notCompleteGamesList = Game.getNotCompleteGamesList();
    notCompleteRoundList = Round.getNotCompleteRoundList();
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
      enabled = MemStore.getBrokerState(brokerId);
    } catch (Exception ignored) {
    }

    MemStore.setBrokerState(brokerId, !enabled);
  }

  public void abortGame (Game game)
  {
    log.info("Trying to abort game: " + game.getGameId());

    new RunAbort(game.getMachine().getMachineName()).run();

    message(2, "Aborting games takes some time, please wait");
  }

  public void killGame (Game game)
  {
    log.info("Trying to kill game: " + game.getGameId());

    int gameId = game.getGameId();
    int machineId = game.getMachine().getMachineId();
    Machine machine = game.getMachine();
    String machineName = game.getMachine().getMachineName();

    Session session = HibernateUtil.getSession();
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
    new RunKill(machineName).run();
  }

  public void restartGame (Game game)
  {
    log.info("Trying to restart game: " + game.getGameId());

    int gameId = game.getGameId();

    Session session = HibernateUtil.getSession();
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

  //<editor-fold desc="Collections">
  public List<Broker> getBrokerList ()
  {
    return brokerList;
  }

  public List<Game> getNotCompleteGamesList ()
  {
    return notCompleteGamesList;
  }

  public List<Round> getNotCompleteRoundList ()
  {
    return notCompleteRoundList;
  }
  //</editor-fold>

  //<editor-fold desc="Getters and Setters">
  public boolean isHideInactiveBrokers ()
  {
    return MemStore.isHideInactiveBrokers();
  }
  public void setHideInactiveBrokers (boolean ignored)
  {
    MemStore.setHideInactiveBrokers(!MemStore.isHideInactiveBrokers());
  }

  public boolean isHideInactiveGames ()
  {
    return MemStore.isHideInactiveGames();
  }
  public void setHideInactiveGames (boolean ignored)
  {
    MemStore.setHideInactiveGames(!MemStore.isHideInactiveGames());
  }
  //</editor-fold>
}