package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.powertac.tourney.scheduling.AgentLet;
import org.powertac.tourney.scheduling.MainScheduler;
import org.powertac.tourney.scheduling.Server;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.RunBootstrap;
import org.powertac.tourney.services.RunGame;
import org.powertac.tourney.services.TournamentProperties;
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
public class Scheduler
{
  private static Logger log = Logger.getLogger("TMLogger");
  public static final String key = "scheduler";

  private TournamentProperties tournamentProperties;

  public static boolean bootRunning = false;

  private Timer watchDogTimer = null;
  private MainScheduler scheduler;
  private Tournament runningTournament;

  private HashMap<Server, AgentLet[]> games = new HashMap<Server, AgentLet[]>();
  private HashMap<Integer, Integer> AgentIdToBrokerId = new HashMap<Integer, Integer>();
  private HashMap<Integer, Integer> ServerIdToMachineId = new HashMap<Integer, Integer>();

  /* TODO
     We need to keep score of the startTime of boot,
     If the boot takes to long, cancel + reschedule it
     We could the readyTime field for this (needs a rename though)
    */

  public Scheduler ()
  {
    tournamentProperties = TournamentProperties.getProperties();
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
        db.commitTrans();
        return;
      }
      log.info("Reloading Tournament: " + t.getTournamentName());

      runningTournament = t;
      List<Machine> machines = db.getMachines();
      List<Database.Server> servers = db.getServers();
      for (int i = 0; i < servers.size(); i++) {
        ServerIdToMachineId.put(servers.get(i).getServerNumber(),
                                machines.get(i).getMachineId());
      }

      // Initially no one is registered so set brokerId's to -1
      for (Database.Agent agent: db.getAgents()) {
        AgentIdToBrokerId.put(agent.getInternalAgentID(), -1);
      }

      int noofagents = t.getMaxBrokers();
      int noofcopies = t.getMaxBrokerInstances();
      int noofservers = machines.size();
      int[] gtypes = { t.getSize1(), t.getSize2(), t.getSize3() };
      int[] mxs = { t.getNumberSize1(), t.getNumberSize2(), t.getNumberSize3() };

      try {
        scheduler = new MainScheduler(noofagents, noofservers);
        scheduler.initServerPanel(noofservers);
        scheduler.initializeAgentsDB(noofagents, noofcopies);
        scheduler.initGameCube(gtypes, mxs);
        scheduler.resetCube();
        runningTournament = t;
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      db.commitTrans();
    }
    catch (Exception e) {
      log.error("Error retrieving tourney");
      e.printStackTrace();
      db.closeConnection();
    }
  }

  // Resets the internal scheduling tables;
  public synchronized void resetServer (int machineId)
  {
    // Find the serverId from a machineId
    int serverNumber = -1;
    for (Integer i: ServerIdToMachineId.keySet()) {
      if (ServerIdToMachineId.get(i) == machineId) {
        serverNumber = i;
        break;
      }
    }

    if (serverNumber == -1) {
      return;
    }

    try {
      scheduler.resetServers(serverNumber);
      log.info("Servers and Agents freed");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void tickScheduler ()
  {
    if (runningTournament == null) {
      log.info("No multigame tournament available");
      return;
    }

    if ((runningTournament.getStartTime() == null) ||
        (runningTournament.getStartTime().before(new Date()))) {
      log.info(String.format("Multigame tournament available (%s), ticking "
          + "scheduler..", runningTournament.getTournamentName()));
    }
    else {
      log.info("Too early to start tournament: " +
          runningTournament.getTournamentName());
      return;
    }

    try {
      Database db = new Database();
      db.startTrans();
      List<Game> gamesInTourney =
        db.getGamesInTourney(runningTournament.getTournamentId());
      List<Broker> brokersInTourney =
        db.getBrokersInTournament(runningTournament.getTournamentId());
      int i = 0;
      log.info(String.format("Brokers in Tournament: %s  TourneyId: %s",
          brokersInTourney.size(), runningTournament.getTournamentId()));

      for (int agentId: AgentIdToBrokerId.keySet()) {
        if (i >= brokersInTourney.size()) {
          break;
        }
        AgentIdToBrokerId.put(agentId, brokersInTourney.get(i++).getBrokerId());
      }
      db.commitTrans();

      List<Game> finalGames = new ArrayList<Game>();
      for (Game g : gamesInTourney) {
        if (!g.getHasBootstrap()
            || g.stateEquals(Game.STATE.game_pending)
            || g.stateEquals(Game.STATE.game_ready)
            || g.stateEquals(Game.STATE.game_in_progress)
            || g.stateEquals(Game.STATE.game_complete)) {
          // gamesInTourney.remove(g);
        } else {
          finalGames.add(g);
        }
      }
      gamesInTourney = finalGames;

      if (gamesInTourney.size() == 0) {
        log.info("Tournament is either complete or not " +
            "enough bootstraps are available");
        return;
      }
      else {
        log.info("Games with boots available " + gamesInTourney.size());
      }

      if (!scheduler.equilibrium()) {
        if (games.isEmpty()) {
          log.info("Acquiring new schedule...");
          games = scheduler.Schedule();
        }
        log.info(String.format("WatchDogTimer reports %s tournament game(s) "
            + "are ready to start", games.size()));

        List<Server> servers = new ArrayList<Server>(games.keySet());
        for (Server s: servers) {
          if (gamesInTourney.size() == 0) {
            break;
          }
          AgentLet[] agentSet = games.get(s);

          log.info(String.format("Server %s playing", s.getServerNumber()));

          for (AgentLet a: agentSet) {
            log.info("Agent " + a.getAgentType());
          }

          String result = "";
          for (Integer key: ServerIdToMachineId.keySet()) {
            result += key + ",";
          }
          log.info("Key Set in serversToMachines: " + result);
          Integer machineId = ServerIdToMachineId.get(s.getServerNumber());

          List<Integer> brokerSet = new ArrayList<Integer>();
          for (AgentLet a: agentSet) {
            brokerSet.add(AgentIdToBrokerId.get(a.getAgentType()));
          }
          log.info("BrokerSet Size " + brokerSet.size());

          log.info("Games ready");
          Game somegame = gamesInTourney.get(0);
          gamesInTourney.remove(somegame);

          log.info(String.format("Game: %s will be started...",
              somegame.getGameId()));

          Database db1 = new Database();
          db1.startTrans();
          Machine m = db1.getMachineById(machineId);
          String brokers = "";
          for (Integer b: brokerSet) {
            Broker tmp = db1.getBroker(b);
            log.info(String.format("Adding broker %s to game %s",
                tmp.getBrokerId(), somegame.getGameId()));
            db1.addBrokerToGame(somegame.getGameId(), tmp);
            brokers += tmp.getBrokerName() + ",";
          }
          db1.commitTrans();

          int lastIndex = brokers.length();
          brokers = brokers.substring(0, lastIndex - 1);

          log.info(String.format("Tourney Game %s Brokers: %s",
              somegame.getGameId(), brokers));

          RunGame runGame = new RunGame(somegame.getGameId(),
                                        runningTournament.getPomId(),
                                        m, brokers);
          new Thread(runGame).start();

          games.remove(s);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
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

    TimerTask watchDog = new TimerTask() {
      @Override
      public void run ()
      {
        try {
          checkMachines();

          tickScheduler();
          checkForBoots();
          checkForSims();
        }
        catch (Exception e) {
          log.error("Severe error in WatchDogTimer!");
          e.printStackTrace();
        }
      }
    };

    long watchDogInt =
      Integer.parseInt(tournamentProperties
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

  private void checkMachines() {
    log.info("WatchDogTimer Checking Machine States..");

    Database db = new Database();
    try {
      db.startTrans();

      // TODO Get Jenkins URL from config
      String url = tournamentProperties.getProperty("jenkinsLocation")
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

            // We don't the status of the master
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
              log.warn(String.format("Machine %s is set available, but "
                  + "Jenkins reports offline", displayName));
              db.setMachineAvailable(machine.getMachineId(), false);
            }

            if (machine.stateEquals(Machine.STATE.idle) && idle.equals("false")) {
              log.warn(String.format("Machine %s has status 'idle', but "
                  + "Jenkins reports 'not idle'", displayName));
              db.setMachineStatus(machine.getMachineId(), Machine.STATE.running);
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

  /***
   * Check if games need to be booted, only pick the first one
   * bootRunning is set to true if by RunBootstrap, if it actually runs
   */
  private void checkForBoots ()
  {
    if (bootRunning) {
      log.info("WatchDogTimer Reports a boot is running");
      return;
    }
    else {
      log.info("WatchDogTimer Looking for Bootstraps To Start..");
    }

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
    log.info("WatchDogTimer Looking for Games To Start..");

    // Check Database for startable games
    Database db = new Database();
    try {
      db.startTrans();
      List<Game> games;

      if (runningTournament == null) {
				log.info("WatchDog CheckForSims for SINGLE_GAME tournament games");
				games = db.getRunnableSingleGames();
      }
      else {
				log.info("WatchDog CheckForSims for MULTI_GAME tournament games");
        games = db.getRunnableGames(runningTournament.getTournamentId());
      }
      log.info(String.format("WatchDogTimer reports %s game(s) are ready to "
          + "start", games.size()));

      for (Game g: games) {
        Tournament t = db.getTournamentByGameId(g.getGameId());
        log.info(String.format("Game: %s will be started...", g.getGameId()));

        RunGame runGame = new RunGame(g.getGameId(), t.getPomId());
        new Thread(runGame).start();

        try {
          Thread.sleep(5000);
        } catch (Exception ignored) {}
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

  @PreDestroy
  private void cleanUp () throws Exception
  {
    log.info("Spring Container is destroyed! Scheduler clean up");

    stopWatchDog();
  }
}