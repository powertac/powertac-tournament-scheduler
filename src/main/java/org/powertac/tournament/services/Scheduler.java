package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Machine;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.jobs.RunBoot;
import org.powertac.tournament.jobs.RunSim;
import org.powertac.tournament.schedulers.RoundScheduler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


@Service("scheduler")
@ManagedBean
public class Scheduler implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  @Autowired
  private TournamentProperties properties;

  private Timer schedulerTimer = null;
  private long schedulerInterval;

  private List<Integer> runningRoundIds;

  private long lastSchedulerRun = 0;

  public Scheduler ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    runningRoundIds = new ArrayList<>();
    lazyStart();
  }

  private void lazyStart ()
  {
    schedulerInterval =
        properties.getPropertyInt("scheduler.schedulerInterval");

    Timer t = new Timer();
    TimerTask tt = new TimerTask()
    {
      @Override
      public void run ()
      {
        startScheduler();
      }
    };
    t.schedule(tt, 3000);
  }

  private synchronized void startScheduler ()
  {
    if (schedulerTimer != null) {
      log.warn("Scheduler already running");
      return;
    }

    log.info("Starting Scheduler...");

    lastSchedulerRun = System.currentTimeMillis();

    TimerTask schedulerTimerTask = new TimerTask()
    {
      @Override
      public void run ()
      {
        // Empty line to make logs more readable
        log.info(System.getProperty("line.separator"));
        try {
          MemStore.getNameMapping(false);
          List<Game> notCompleteGames = Game.getNotCompleteGamesList();
          List<Machine> freeMachines = Machine.checkMachines();
          //createGamesForLoadedRounds();
          RunSim.startRunnableGames(runningRoundIds, notCompleteGames, freeMachines);
          RunBoot.startBootableGames(runningRoundIds, notCompleteGames, freeMachines);
          checkWedgedBoots(notCompleteGames);
          checkWedgedSims(notCompleteGames);
          lastSchedulerRun = System.currentTimeMillis();
        }
        catch (Exception e) {
          log.error("Severe error in SchedulerTimer!");
          e.printStackTrace();
        }
      }
    };

    schedulerTimer = new Timer();
    schedulerTimer.schedule(schedulerTimerTask, new Date(), schedulerInterval);
  }

  private void stopScheduler ()
  {
    if (schedulerTimer != null) {
      schedulerTimer.cancel();
      schedulerTimer.purge();
      schedulerTimer = null;
      log.info("Stopping Scheduler...");
    }
    else {
      log.warn("SchedulerTimer Already Stopped");
    }
  }

  public boolean restartScheduler ()
  {
    if ((System.currentTimeMillis() - lastSchedulerRun) < 55000) {
      stopScheduler();
      startScheduler();
      return true;
    }
    else {
      return false;
    }
  }

  public void loadRounds (List<Integer> roundIDs)
  {
    runningRoundIds = new ArrayList<>();
    for (int roundId : roundIDs) {
      Round round = Round.getRoundFromId(roundId, true);
      if (round != null && !round.getState().isComplete()) {
        runningRoundIds.add(round.getRoundId());
      }
    }
  }

  public void unloadRounds (boolean logInfo)
  {
    if (logInfo) {
      for (int roundId : runningRoundIds) {
        log.info("Unloading Round " + roundId);
      }
      log.info("All rounds are unloaded");
    }

    runningRoundIds.clear();
  }

  // This function removes the given round from 'runningRounds'.
  public void unloadRound (Integer roundId)
  {
    int index = runningRoundIds.indexOf(roundId);
    runningRoundIds.remove(index);
  }

  /**
   * Check if it's time to schedule the round
   */
  private void createGamesForLoadedRounds ()
  {
    if (runningRoundIds.isEmpty()) {
      log.info("No rounds available for scheduling");
      return;
    }

    boolean roundsChanged = false;
    for (int roundId : runningRoundIds) {
      Round fatRound = Round.getRoundFromId(roundId, false);
      roundsChanged |= new RoundScheduler(fatRound).createGamesForLoadedRound();
    }

    if (roundsChanged) {
      log.info("Some rounds were scheduled, reload rounds.");
      MemStore.getNameMapping(true);
    }

    System.gc();
  }

  private void checkWedgedBoots (List<Game> notCompleteGames)
  {
    log.info("SchedulerTimer Looking for Wedged Bootstraps");

    for (Game game : notCompleteGames) {
      if (!game.getState().isBooting() || game.getReadyTime() == null) {
        continue;
      }

      long wedgedDeadline =
          properties.getPropertyInt("scheduler.bootstrapWedged");
      long nowStamp = Utils.offsetDate().getTime();
      long minStamp = game.getReadyTime().getTime() + wedgedDeadline;
      long maxStamp = minStamp + schedulerInterval;

      if (nowStamp > minStamp && nowStamp < maxStamp) {
        String msg = String.format(
            "Bootstrapping of game %s seems to take too long : %s seconds",
            game.getGameId(), ((nowStamp - game.getReadyTime().getTime()) / 1000));
        log.error(msg);
        Utils.sendMail("Bootstrap seems stuck", msg,
            properties.getProperty("scheduler.mailRecipient"));
        properties.addErrorMessage(msg);
      }
    }
    log.debug("SchedulerTimer No Bootstraps seems Wedged");
  }

  private void checkWedgedSims (List<Game> notCompleteGames)
  {
    log.info("SchedulerTimer Looking for Wedged Sims");

    for (Game game : notCompleteGames) {
      if (!game.getState().isRunning() || game.getReadyTime() == null) {
        continue;
      }

      long wedgedDeadline =
          properties.getPropertyInt("scheduler.simWedged");
      if (game.getGameName().toLowerCase().contains("test")) {
        wedgedDeadline =
            properties.getPropertyInt("scheduler.simTestWedged");
      }
      long nowStamp = Utils.offsetDate().getTime();
      long minStamp = game.getReadyTime().getTime() + wedgedDeadline;
      long maxStamp = minStamp + schedulerInterval;

      if (nowStamp > minStamp && nowStamp < maxStamp) {
        String msg = String.format(
            "Sim of game %s seems to take too long : %s seconds",
            game.getGameId(), ((nowStamp - game.getReadyTime().getTime()) / 1000));
        log.error(msg);
        Utils.sendMail("Sim seems stuck", msg,
            properties.getProperty("scheduler.mailRecipient"));
        properties.addErrorMessage(msg);
      }
    }
    log.debug("SchedulerTimer No Sim seems Wedged");
  }

  public boolean isRunning ()
  {
    return schedulerTimer != null;
  }

  public List<Integer> getRunningRoundIds ()
  {
    return runningRoundIds;
  }

  public static Scheduler getScheduler ()
  {
    return (Scheduler) SpringApplicationContext.getBean("scheduler");
  }

  @PreDestroy
  private void cleanUp () throws Exception
  {
    log.info("Spring Container is destroyed! Scheduler clean up");

    stopScheduler();
  }

  //<editor-fold desc="Setters and Getters">
  public long getSchedulerInterval ()
  {
    return schedulerInterval;
  }

  public void setSchedulerInterval (long schedulerInterval)
  {
    this.schedulerInterval = schedulerInterval;
  }

  public String getLastSchedulerRun ()
  {
    if (lastSchedulerRun == 0) {
      return "";
    }
    else {
      return String.format(" : ran %s secs ago",
          (int) (System.currentTimeMillis() - lastSchedulerRun) / 1000);
    }
  }
  //</editor-fold>
}