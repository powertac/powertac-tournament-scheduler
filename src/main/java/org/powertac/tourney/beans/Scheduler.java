package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.powertac.tourney.services.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;


@Service("scheduler")
public class Scheduler implements InitializingBean
{
  private static Logger log = Logger.getLogger("TMLogger");
  public static final String key = "scheduler";

  @Autowired
  private TournamentProperties properties;

  private Timer watchDogTimer = null;
  private Tournament runningTournament = null;

  /* TODO
     We need to keep score of the startTime of boot,
     If the boot takes to long, cancel + reschedule it
     We could the readyTime field for this (needs a rename though)
    */

  public Scheduler ()
  {
    super();
  }

  @Override
  public void afterPropertiesSet () throws Exception
  {
    lazyStart();
  }

  public void reloadTournament ()
  {
    Database db = new Database();
    try {
      db.startTrans();

      Tournament t = db.getTournamentByType(Tournament.TYPE.MULTI_GAME);
      if (t == null) {
        log.info("No tournament to reload");
        return;
      }
      log.info("Reloading Tournament: " + t.getTournamentName());

      runningTournament = t;
    }
    catch (Exception e) {
      log.error("Error retrieving tourney");
      e.printStackTrace();
    }
    finally {
      db.abortTrans();
    }
  }

  public void unloadTournament ()
  {
    runningTournament = null;
  }

  private void lazyStart ()
  {
    Timer t = new Timer();
    TimerTask tt = new TimerTask() {
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

    TimerTask watchDog = new TimerTask() {
      @Override
      public void run ()
      {
        try {
          checkMachines();
          checkTournament();
          checkForSims();
          checkForBoots();
        }
        catch (Exception e) {
          log.error("Severe error in WatchDogTimer!");
          e.printStackTrace();
        }
      }
    };

    long watchDogInt =
      Integer.parseInt(properties
          .getProperty("scheduler.watchDogInterval", "120000"));

    watchDogTimer = new Timer();
    watchDogTimer.schedule(watchDog, new Date(), watchDogInt);
  }

  private void stopWatchDog ()
  {
    if (watchDogTimer != null) {
      watchDogTimer.cancel();
      watchDogTimer = null;
      log.info("Stopping WatchDog...");
    }
    else {
      log.warn("WatchDogTimer Already Stopped");
    }
  }

  public void restartWatchDog ()
  {
    stopWatchDog();
    startWatchDog();
  }

  /**
   * Check the status of the Jenkins slaves against the local status
   */
  private void checkMachines ()
  {
    log.info("WatchDogTimer Checking Machine States..");

    Database db = new Database();
    try {
      db.startTrans();

      String url = properties.getProperty("jenkins.location")
          + "computer/api/xml";
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder docB = dbf.newDocumentBuilder();
      Document doc = docB.parse(new URL(url).openStream());
      NodeList nList = doc.getElementsByTagName("computer");

      for (int temp = 0; temp < nList.getLength(); temp++) {
        try{
          Node nNode = nList.item(temp);
          if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            Element eElement = (Element) nNode;

            String displayName = eElement.getElementsByTagName("displayName")
                .item(0).getChildNodes().item(0).getNodeValue();
            String offline = eElement.getElementsByTagName("offline")
                .item(0).getChildNodes().item(0).getNodeValue();
            String idle = eElement.getElementsByTagName("idle")
                .item(0).getChildNodes().item(0).getNodeValue();

            // We don't check the status of the master
            log.debug("Checking machine " + displayName);
            if (displayName.equals("master")) {
              continue;
            }

            Machine machine = db.getMachineByName(displayName);
            if (machine == null) {
              log.warn("Machine " + displayName + " doesn't exist in the TM");
              continue;
            }

            if (machine.isAvailable() && offline.equals("true")) {
              db.setMachineAvailable(machine.getMachineId(), false);
              log.warn(String.format("Machine %s is set available, but "
                  + "Jenkins reports offline", displayName));
            }

            if (machine.stateEquals(Machine.STATE.idle) && idle.equals("false")) {
              db.setMachineStatus(machine.getMachineId(), Machine.STATE.running);
              log.warn(String.format("Machine %s has status 'idle', but "
                  + "Jenkins reports 'not idle'", displayName));
            }
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
      db.commitTrans();
    }
    catch (Exception ignored) {
      db.abortTrans();
    }
  }

  /**
   * Check if it's time to schedule the tournament
   */
  private void checkTournament ()
  {
    if (isNullTourney()) {
      log.info("No multigame tournament available");
      return;
    }

    if (runningTournament.getStartTime().after(new Date())) {
      log.info("Too early to start tournament: " +
          runningTournament.getTournamentName());
      return;
    }

    log.info("Multigame tournament available "
        + runningTournament.getTournamentName());

    // Get array of gametypes, and number of participants
    int tourneyId = runningTournament.getTournamentId();
    int[] gameTypes = {runningTournament.getSize1(),
                       runningTournament.getSize2(),
                       runningTournament.getSize3() };

    // Sort and remove duplicates
    Arrays.sort(gameTypes);
    if (gameTypes[2] == gameTypes[1]) {
      gameTypes[2] = 0;
    }
    if (gameTypes[1] == gameTypes[0]) {
      gameTypes[1] = 0;
    }

    Database db = new Database();
    try {
      db.startTrans();

      List<Game> games = db.getGamesInTourney(tourneyId);
      if (games.size() > 0) {
        db.abortTrans();
        log.info("Tournament already scheduled");
        return;
      }

      List<Broker> brokers = db.getBrokersInTournament(tourneyId);
      if (brokers.size() == 0) {
        db.updateTournamentStatus(tourneyId, Tournament.STATE.complete);
        db.abortTrans();
        log.error("Tournament has no brokers registered, setting to complete");
        return;
      }

      for (int i=gameTypes.length-1; i > -1; i--) {
        doTheKailash(db, gameTypes[i], brokers);
      }

      db.commitTrans();
    }
    catch (Exception e) {
      e.printStackTrace();
      db.abortTrans();
    }
  }

  private void doTheKailash (Database db, int gameType, List<Broker> brokers)
      throws SQLException
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
    if (gameType<1 || brokers.size()<1) {
      return;
    }

    // Get binary string representations of games
    List<String> games = new ArrayList<String>();
    for (int i=0; i<(int) Math.pow(2, brokers.size()); i++) {
      // Write as binary + pad with leading zeros
      String gameString = Integer.toBinaryString(i);
      while (gameString.length() < brokers.size()) {
        gameString = '0' + gameString;
      }

      // Count number of 1's, representing participating players
      int count = 0;
      for (int j=0; j<gameString.length(); j++) {
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
    for (int j=0; j<games.size(); j++) {
      String gameString = games.get(j);

      String gameName = String.format("%s_%s_%s",
          runningTournament.getTournamentName(), gameType, j);
      int gameId = db.addGame(gameName, runningTournament.getTournamentId(),
          gameType, new Date());
      CreateProperties.genProperties(db, gameId,
          runningTournament.getLocationsList(),
          runningTournament.getDateFrom(), runningTournament.getDateTo());

      log.info("Added game " + gameId);
      log.info("Added properties for game " + gameId);

      for (int i=0; i<gameString.length(); i++) {
        if (gameString.charAt(i) == '1') {
          db.addBrokerToGame(gameId, brokers.get(i).getBrokerId());
          log.info(String.format("Added broker %s to game %s",
              brokers.get(i).getBrokerId(), gameId));
        }
      }
    }
  }

  /***
   * Check if games need to be booted, only pick the first one
   * bootRunning is set to true if by RunBootstrap, if it actually runs
   */
  private void checkForBoots ()
  {
    log.info("WatchDogTimer Looking for Bootstraps To Start..");

    Database db = new Database();
    try {
      db.startTrans();
      List<Game> games = db.getBootableGames();
      log.info(String.format("WatchDogTimer reports %s boots are ready to "
          + "start", games.size()));

      if (games.size() > 0) {
        Game g = games.get(0);
        Tournament t = db.getTournamentByGameId(g.getGameId());

        log.info(String.format("Boot: %s will be started...", g.getGameId()));

        RunBootstrap runBootstrap = new RunBootstrap(g.getGameId(), t.getPomId());
        new Thread(runBootstrap).start();
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  private void checkForSims ()
  {
    log.info("WatchDogTimer Looking for Runnable Games");

    // Check Database for startable games
    Database db = new Database();
    try {
      db.startTrans();

      if (db.checkFreeMachine() == null) {
        log.info("WatchDog No free machines, not looking for Runnable Games");
        db.commitTrans();
        return;
      }

      List<Game> games;
      if (runningTournament == null) {
				games = db.getRunnableSingleGames();
        log.info("WatchDog CheckForSims for SINGLE_GAME tournament games");
      }
      else {
        games = db.getRunnableMultiGames(runningTournament.getTournamentId());
        log.info("WatchDog CheckForSims for MULTI_GAME tournament games");
      }
      log.info(String.format("WatchDogTimer reports %s game(s) are ready to "
          + "start", games.size()));

      for (Game g: games) {
        Tournament t = db.getTournamentByGameId(g.getGameId());
        log.info(String.format("Game: %s will be started...", g.getGameId()));

        RunGame runGame = new RunGame(g.getGameId(), t.getPomId());
        new Thread(runGame).start();
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
  }

  public boolean isNullTourney ()
  {
    return runningTournament == null;
  }

  public static String getKey ()
  {
    return key;
  }

  public boolean isRunning ()
  {
    return watchDogTimer != null;
  }

  public Tournament getRunningTournament ()
  {
    return runningTournament;
  }

  @PreDestroy
  private void cleanUp () throws Exception
  {
    log.info("Spring Container is destroyed! Scheduler clean up");

    stopWatchDog();
  }
}