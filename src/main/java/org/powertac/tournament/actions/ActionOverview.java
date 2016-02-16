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
import org.powertac.tournament.runners.RunAbort;
import org.powertac.tournament.runners.RunKill;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@ManagedBean
public class ActionOverview implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  private List<Broker> brokerList;
  private List<Game> notCompleteGamesList;
  private List<Round> notCompleteRoundList;
  private Map<Integer, Set<Integer>> runningGames;

  public ActionOverview ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    runningGames = new HashMap<Integer, Set<Integer>>();

    brokerList = Broker.getBrokerList();
    Set<Integer> brokerIds = new HashSet<Integer>();
    for (Broker broker : brokerList) {
      brokerIds.add(broker.getBrokerId());
      runningGames.put(broker.getBrokerId(), new HashSet<Integer>());
    }

    notCompleteRoundList = Round.getNotCompleteRoundList();
    notCompleteGamesList = new ArrayList<Game>();
    for (Round round : notCompleteRoundList) {
      for (Game game : round.getGameMap().values()) {
        if (game.getState().isComplete()) {
          continue;
        }

        notCompleteGamesList.add(game);

        for (Agent agent : game.getAgentMap().values()) {
          if (agent.isInProgress() && brokerIds.contains(agent.getBrokerId())) {
            runningGames.get(agent.getBrokerId()).add(game.getGameId());
          }
        }
      }
    }
  }

  public String getBrokerState (int brokerId)
  {
    return MemStore.getBrokerState(brokerId) ? "enabled" : "disabled";
  }

  public void abortGame (Game game)
  {
    log.info("Trying to abort game: " + game.getGameId());

    new RunAbort(game.getMachine().getMachineName()).run();

    Utils.growlMessage("Notice", "Aborting games takes some time, please wait");
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
      if (game.getState().isBooting()) {
        log.info("Resetting boot game: " + gameId + " on machine: " + machineId);

        game.setState(Game.GameState.boot_pending);
        game.removeBootFile();
      }
      else if (game.getState().isRunning()) {
        log.info("Resetting sim game: " + gameId + " on machine: " + machineId);

        game.setState(Game.GameState.boot_complete);
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
  }

  public void restartGame (Game game)
  {
    log.info("Trying to restart game: " + game.getGameId());

    int gameId = game.getGameId();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (game.getState().isBootFailed()) {
        log.info("Resetting boot game: " + gameId);
        game.removeBootFile();
        game.setState(Game.GameState.boot_pending);
      }
      if (game.getState().isGameFailed()) {
        log.info("Resetting sim game: " + gameId);
        game.setState(Game.GameState.boot_complete);

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
  }

  //<editor-fold desc="Collections">
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

  public String getRunningGames (int brokerId)
  {
    Set<Integer> tmp = runningGames.get(brokerId);
    if (tmp == null) {
      return "";
    }
    return tmp.toString().replace("[", "").replace("]", "");
  }
  //</editor-fold>
}