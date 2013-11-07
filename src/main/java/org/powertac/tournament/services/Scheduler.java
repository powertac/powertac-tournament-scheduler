package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.*;
import org.powertac.tournament.constants.Constants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import java.util.*;


@Service("scheduler")
@ManagedBean
public class Scheduler implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  @Autowired
  private TournamentProperties properties;

  private Timer watchDogTimer = null;
  private long watchDogInterval;

  private Round runningRound = null;
  private long lastWatchdogRun = 0;

  public Scheduler ()
  {
    super();
  }

  public void afterPropertiesSet () throws Exception
  {
    lazyStart();
  }

  private void lazyStart ()
  {
    watchDogInterval = properties.getPropertyInt("scheduler.watchDogInterval");

    Timer t = new Timer();
    TimerTask tt = new TimerTask()
    {
      @Override
      public void run ()
      {
        startWatchDog();
      }
    };
    t.schedule(tt, 3000);
  }

  private synchronized void startWatchDog ()
  {
    if (watchDogTimer != null) {
      log.warn("Watchdog already running");
      return;
    }

    log.info("Starting WatchDog...");

    lastWatchdogRun = System.currentTimeMillis();

    TimerTask watchDog = new TimerTask()
    {
      @Override
      public void run ()
      {
        // Empty line to make logs more readable
        log.info(System.getProperty("line.separator"));
        try {
          Machine.checkMachines();
          scheduleLoadedRound();
          RunGame.startRunnableGames(runningRound);
          RunBoot.startBootableGames(runningRound);
          checkWedgedBoots();
          checkWedgedSims();

          lastWatchdogRun = System.currentTimeMillis();
        } catch (Exception e) {
          log.error("Severe error in WatchDogTimer!");
          e.printStackTrace();
        }
      }
    };

    watchDogTimer = new Timer();
    watchDogTimer.schedule(watchDog, new Date(), watchDogInterval);
  }

  private void stopWatchDog ()
  {
    if (watchDogTimer != null) {
      watchDogTimer.cancel();
      watchDogTimer.purge();
      watchDogTimer = null;
      log.info("Stopping WatchDog...");
    } else {
      log.warn("WatchDogTimer Already Stopped");
    }
  }

  public boolean restartWatchDog ()
  {
    if ((System.currentTimeMillis() - lastWatchdogRun) < 55000) {
      stopWatchDog();
      startWatchDog();
      return true;
    } else {
      return false;
    }
  }

  public void loadRound (int roundId)
  {
    log.info("Loading Round " + roundId);

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_ROUND_BY_ID);
      query.setInteger("roundId", roundId);
      runningRound = (Round) query.uniqueResult();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public void unloadRound ()
  {
    log.info("Unloading Round " + runningRound.getRoundName());
    runningRound = null;
  }

  public void reloadRound ()
  {
    if (runningRound == null) {
      return;
    }
    int runningId = runningRound.getRoundId();
    unloadRound();
    loadRound(runningId);
  }

  /**
   * Check if it's time to schedule the round
   */
  private void scheduleLoadedRound ()
  {
    if (isNullRound()) {
      log.info("No round available for scheduling");
      return;
    }
    else if (runningRound.getSize() > 0) {
      log.info("Round already scheduled : " + runningRound.getRoundName());
      return;
    }
    else if (!runningRound.isStarted()) {
      log.info("Round not ready : " + runningRound.getRoundName());
      return;
    }
    log.info("Round available : "+ runningRound.getRoundName());

    // Brokers might have (un)registered after the round was loaded
    reloadRound();

    // Get array of gametypes
    int[] gameTypes = {runningRound.getSize1(),
        runningRound.getSize2(), runningRound.getSize3()};
    int[] multipliers = {runningRound.getMultiplier1(),
        runningRound.getMultiplier2(), runningRound.getMultiplier3()};

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      List<Broker> brokers = new ArrayList<Broker>();
      for (Broker broker: runningRound.getBrokerMap().values()) {
        brokers.add(broker);
      }

      if (brokers.size() == 0) {
        log.info("Round has no brokers registered, setting to complete");
        runningRound.setStateToComplete();
        session.update(runningRound);
        unloadRound();
        transaction.commit();
        return;
      }

      for (int i = 0; i < (gameTypes.length); i++) {
        for (int j = 0; j < multipliers[i]; j++) {
          doTheKailash(session, gameTypes[i], i, j, brokers);
        }
      }

      runningRound.setStateToInProgress();
      session.update(runningRound);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    } finally {
      session.close();
    }

    reloadRound();
  }

  private void doTheKailash (Session session, int gameType, int gameNumber,
                             int multiplier, List<Broker> brokers)
  {
    log.info(String.format("Doing the Kailash with gameType = %s ; "
        + "maxBrokers = %s", gameType, brokers.size()));
    String brokersString = "";
    for (Broker b: brokers) {
      brokersString += b.getBrokerId() + " ";
    }
    log.info("Broker ids : " + brokersString);

    // No use scheduling gamesTypes > # brokers
    gameType = Math.min(gameType, brokers.size());
    if (gameType < 1 || brokers.size() < 1) {
      return;
    }

    // Get binary string representations of games
    List<String> games = new ArrayList<String>();
    for (int i = 0; i < (int) Math.pow(2, brokers.size()); i++) {
      // Write as binary + pad with leading zeros
      String gameString = Integer.toBinaryString(i);
      while (gameString.length() < brokers.size()) {
        gameString = '0' + gameString;
      }

      // Count number of 1's, representing participating players
      int count = 0;
      for (int j = 0; j < gameString.length(); j++) {
        if (gameString.charAt(j) == '1') {
          count++;
        }
      }

      // We need an equal amount of participants as the gameType
      if (count == gameType) {
        games.add(gameString);
      }
    }

    // Make games of every gameString
    for (int j = 0; j < games.size(); j++) {
      String gameString = games.get(j);

      String gameName = String.format("%s_%s_%s_%s",
          runningRound.getRoundName(),
          (gameNumber + 1), gameType, j + multiplier * games.size());
      Game game = Game.createGame(runningRound, gameName);
      session.save(game);

      log.info("Created game " + game.getGameId());

      for (int i = 0; i < gameString.length(); i++) {
        if (gameString.charAt(i) == '1') {
          Broker broker = brokers.get(i);
          Agent agent = Agent.createAgent(broker, game);
          session.save(agent);
          log.debug(String.format("Registering broker: %s with game: %s",
              broker.getBrokerId(), game.getGameId()));
        }
      }
    }
  }

  private void checkWedgedBoots ()
  {
    log.info("WatchDogTimer Looking for Wedged Bootstraps");

    for (Game game: Game.getNotCompleteGamesList()) {
      if (!game.isBooting() || game.getReadyTime() == null) {
        continue;
      }

      long wedgedDeadline =
          properties.getPropertyInt("scheduler.bootstrapWedged");
      long nowStamp = Utils.offsetDate().getTime();
      long minStamp = game.getReadyTime().getTime() + wedgedDeadline;
      long maxStamp = minStamp + watchDogInterval;

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
    log.debug("WatchDogTimer No Bootstraps seems Wedged");
  }

  private void checkWedgedSims ()
  {
    log.info("WatchDogTimer Looking for Wedged Sims");

    for (Game game: Game.getNotCompleteGamesList()) {
      if (!game.isRunning() || game.getReadyTime() == null) {
        continue;
      }

      long wedgedDeadline = properties.getPropertyInt("scheduler.simWedged");
      if (game.getGameName().toLowerCase().contains("test")) {
        wedgedDeadline = properties.getPropertyInt("scheduler.simTestWedged");
      }
      long nowStamp = Utils.offsetDate().getTime();
      long minStamp = game.getReadyTime().getTime() + wedgedDeadline;
      long maxStamp = minStamp + watchDogInterval;

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
    log.debug("WatchDogTimer No Sim seems Wedged");
  }

  public boolean isNullRound ()
  {
    return runningRound == null;
  }

  public boolean isRunning ()
  {
    return watchDogTimer != null;
  }

  public Round getRunningRound ()
  {
    return runningRound;
  }

  public static Scheduler getScheduler ()
  {
    return (Scheduler) SpringApplicationContext.getBean("scheduler");
  }

  @PreDestroy
  private void cleanUp () throws Exception
  {
    log.info("Spring Container is destroyed! Scheduler clean up");

    stopWatchDog();
  }

  //<editor-fold desc="Setters and Getters">
  public long getWatchDogInterval ()
  {
    return watchDogInterval;
  }

  public void setWatchDogInterval (long watchDogInterval)
  {
    this.watchDogInterval = watchDogInterval;
  }

  public String getLastWatchdogRun ()
  {
    if (lastWatchdogRun == 0) {
      return "";
    } else {
      return String.format(" : ran %s secs ago",
          (int) (System.currentTimeMillis() - lastWatchdogRun) / 1000);
    }
  }
  //</editor-fold>
}