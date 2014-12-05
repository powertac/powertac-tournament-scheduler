package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Machine;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.RunAbort;
import org.powertac.tournament.services.RunKill;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.List;


@ManagedBean
public class ActionOverview implements InitializingBean
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
    }
    else {
      return "disabled";
    }
  }

  public void toggleState (int brokerId)
  {
    boolean enabled = true;

    try {
      enabled = MemStore.getBrokerState(brokerId);
    }
    catch (Exception ignored) {
    }

    MemStore.setBrokerState(brokerId, !enabled);
  }

  public void abortGame (Game game)
  {
    log.info("Trying to abort game: " + game.getGameId());

    new RunAbort(game.getMachine().getMachineName()).run();

    Utils.growlMessage("Notice", "Aborting games takes some time, please wait");

    // Somehow the Show/Hide event is fired
    MemStore.setHideInactiveGames(!MemStore.isHideInactiveGames());
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
      }
      else if (game.isRunning()) {
        log.info("Resetting sim game: " + gameId + " on machine: " + machineId);

        game.setStateBootComplete();
        for (Agent agent : game.getAgentMap().values()) {
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
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      log.error("Failed to completely kill game: " + gameId);
      Utils.growlMessage("Failed to kill game : " + gameId);
    }
    finally {
      session.close();
    }

    // Kill the job on Jenkins and the slave
    new RunKill(machineName).run();

    // Removed MemStored info about game
    MemStore.removeGameInfo(gameId);

    // Somehow the Show/Hide event is fired
    MemStore.setHideInactiveGames(!MemStore.isHideInactiveGames());
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

        for (Agent agent : game.getAgentMap().values()) {
          agent.setStatePending();
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
      Utils.growlMessage("Failed to restart game : " + gameId);
    }
    session.close();

    // Somehow the Show/Hide event is fired
    MemStore.setHideInactiveGames(!MemStore.isHideInactiveGames());
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