package org.powertac.tournament.services;

import org.apache.log4j.Logger;
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

  private Timer schedulerTimer = null;
  private long schedulerInterval;

  private List<Round> runningRounds;
  private long lastSchedulerRun = 0;

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
          Machine.checkMachines();
          createGamesForLoadedRounds();
          RunGame.startRunnableGames(runningRounds);
          RunBoot.startBootableGames(runningRounds);
          checkWedgedBoots();
          checkWedgedSims();

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
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();

    runningRounds = new ArrayList<Round>();
    try {
      for (int roundId : roundIDs) {
        Round round = (Round) session.createQuery(Constants.HQL.GET_ROUND_BY_ID)
            .setInteger("roundId", roundId).uniqueResult();
        if (round != null && !round.isComplete()) {
          runningRounds.add(round);
        }
      }
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public void unloadRounds (boolean logInfo)
  {
    if (logInfo) {
      for (Round round : runningRounds) {
        log.info("Unloading Round " + round.getRoundName());
      }
      log.info("All rounds are unloaded");
    }
    runningRounds.clear();
  }

  // This function removes the given round from 'runningRounds'.
  public void unloadRound (Integer roundID)
  {
    Round round;
    Iterator<Round> roundIterator = runningRounds.listIterator();

    while (roundIterator.hasNext()) {
      round = roundIterator.next();
      if (round.getRoundId() == roundID) {
        roundIterator.remove();
      }
    }
  }

  public void reloadRounds ()
  {
    if (runningRounds == null) {
      return;
    }

    List<Integer> runningRoundIDs = new ArrayList<Integer>();
    for (Round round : runningRounds) {
      runningRoundIDs.add(round.getRoundId());
    }
    unloadRounds(false);
    loadRounds(runningRoundIDs);
  }

  /**
   * Check if it's time to schedule the round
   */
  private void createGamesForLoadedRounds ()
  {
    if (isRunningRoundsEmpty()) {
      log.info("No rounds available for scheduling");
      return;
    }

    // Brokers might have (un)registered after the rounds were loaded
    reloadRounds();

    boolean roundsChanged = false;
    for (Round round : runningRounds) {
      roundsChanged |= createGamesForLoadedRound(round);
    }

    if (roundsChanged) {
      log.info("Some rounds were scheduled, reload rounds.");
      reloadRounds();
    }
  }

  public boolean createGamesForLoadedRound (Round round)
  {
    if (round.getSize() > 0) {
      log.info("Round already scheduled : " + round.getRoundName());
      return false;
    }
    else if (!round.isStarted()) {
      log.info("Round not ready : " + round.getRoundName());
      return false;
    }
    log.info("Round available : " + round.getRoundName());

    // Get array of gametypes
    int[] gameTypes = {round.getSize1(),
        round.getSize2(), round.getSize3()};
    int[] multipliers = {round.getMultiplier1(),
        round.getMultiplier2(), round.getMultiplier3()};

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      List<Broker> brokers = new ArrayList<Broker>();
      for (Broker broker : round.getBrokerMap().values()) {
        brokers.add(broker);
      }

      if (brokers.size() == 0) {
        log.info("Round " + round.getRoundName()
            + " has no brokers registered, setting to complete");
        round.setStateToComplete();
        session.update(round);
        transaction.commit();
        return true;
      }

      for (int i = 0; i < (gameTypes.length); i++) {
        for (int j = 0; j < multipliers[i]; j++) {
          doTheKailash(session, round, gameTypes[i], i, j, brokers);
        }
      }

      round.setStateToInProgress();
      session.update(round);

      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    finally {
      session.close();
    }

    return true;
  }

  private void doTheKailash (Session session, Round round, int gameType,
                             int gameNumber, int multiplier,
                             List<Broker> brokers)
  {
    log.info(String.format("Doing the Kailash with gameType = %s ; "
        + "maxBrokers = %s", gameType, brokers.size()));
    String brokersString = "";
    for (Broker b : brokers) {
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
          round.getRoundName(),
          (gameNumber + 1), gameType, j + multiplier * games.size());
      Game game = Game.createGame(round, gameName);
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
    log.info("SchedulerTimer Looking for Wedged Bootstraps");

    for (Game game : Game.getNotCompleteGamesList()) {
      if (!game.isBooting() || game.getReadyTime() == null) {
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

  private void checkWedgedSims ()
  {
    log.info("SchedulerTimer Looking for Wedged Sims");

    for (Game game : Game.getNotCompleteGamesList()) {
      if (!game.isRunning() || game.getReadyTime() == null) {
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

  public boolean isRunningRoundsEmpty ()
  {
    return runningRounds == null || runningRounds.size() == 0;
  }

  public boolean isRunning ()
  {
    return schedulerTimer != null;
  }

  public List<Round> getRunningRounds ()
  {
    return runningRounds;
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