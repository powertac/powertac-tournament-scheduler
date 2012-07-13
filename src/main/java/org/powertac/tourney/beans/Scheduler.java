package org.powertac.tourney.beans;

import org.powertac.tourney.scheduling.AgentLet;
import org.powertac.tourney.scheduling.MainScheduler;
import org.powertac.tourney.scheduling.Server;
import org.powertac.tourney.services.*;
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

import static org.powertac.tourney.services.Utils.log;


@Service("scheduler")
public class Scheduler
{
  private TournamentProperties tournamentProperties;

  public static final String key = "scheduler";
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
    */

  public Scheduler ()
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
        log("[INFO] No tournament to reload");
        db.commitTrans();
        return;
      }

      log("[INFO] Reloading Tournament: {0}", t.getTournamentName());

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
      log("Error retrieving tourney");
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
      log("[INFO] Servers and Agents freed");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void tickScheduler ()
  {
    if (runningTournament == null) {
      log("[INFO] No multigame tournament available");
      return;
    }

    if ((runningTournament.getStartTime() == null) ||
        (runningTournament.getStartTime().before(new Date()))) {
      log("[INFO] Multigame tournament available ({0}), ticking scheduler..",
          runningTournament.getTournamentName());
    }
    else {
      log("[INFO] Too early to start tournament: {0}",
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
      log("[INFO] Brokers in Tournament: {0}  TourneyId: {1}",
          brokersInTourney.size(), runningTournament.getTournamentId());

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
            || g.stateEquals(Game.STATE.game_in_progress)
            || g.stateEquals(Game.STATE.game_complete)) {
          // gamesInTourney.remove(g);
        } else {
          finalGames.add(g);
        }
      }
      gamesInTourney = finalGames;

      if (gamesInTourney.size() == 0) {
        log("[INFO] Tournament is either complete or not " +
            "enough bootstraps are available");
        return;
      }
      else {
        log("[INFO] Games with boots available " + gamesInTourney.size());
      }

      if (!scheduler.equilibrium()) {
        if (games.isEmpty()) {
          log("[INFO] Acquiring new schedule...");
          games = scheduler.Schedule();
        }
        log("[INFO] WatchDogTimer reports {0} tournament game(s) are ready to "+
            "start", games.size());

        List<Server> servers = new ArrayList<Server>(games.keySet());
        for (Server s: servers) {
          if (gamesInTourney.size() == 0) {
            break;
          }
          AgentLet[] agentSet = games.get(s);

          log("[INFO] Server {0} playing", s.getServerNumber());

          for (AgentLet a: agentSet) {
            log("[INFO] Agent " + a.getAgentType());
          }

          String result = "";
          for (Integer key: ServerIdToMachineId.keySet()) {
            result += key + ",";
          }
          log("[INFO] Key Set in serversToMachines: " + result);
          Integer machineId = ServerIdToMachineId.get(s.getServerNumber());

          List<Integer> brokerSet = new ArrayList<Integer>();
          for (AgentLet a: agentSet) {
            brokerSet.add(AgentIdToBrokerId.get(a.getAgentType()));
          }
          log("[INFO] BrokerSet Size " + brokerSet.size());

          log("[INFO] Games ready");
          Game somegame = gamesInTourney.get(0);
          gamesInTourney.remove(somegame);

          log("[INFO] {0} : Game: {1} will be started...",
              Utils.dateFormatUTC(new Date()), somegame.getGameId());

          Database db1 = new Database();
          db1.startTrans();
          Machine m = db1.getMachineById(machineId);
          String brokers = "";
          for (Integer b: brokerSet) {
            Broker tmp = db1.getBroker(b);
            log("[INFO] Adding broker {0} to game {1}",
                tmp.getBrokerId(), somegame.getGameId());
            db1.addBrokerToGame(somegame.getGameId(), tmp);
            brokers += tmp.getBrokerName() + ",";
          }
          db1.commitTrans();

          int lastIndex = brokers.length();
          brokers = brokers.substring(0, lastIndex - 1);

          log("[INFO] Tourney Game {0} Brokers: {1}", somegame.getGameId(), brokers);

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
        tournamentProperties = (TournamentProperties) SpringApplicationContext
            .getBean("tournamentProperties");
        startWatchDog();
      }
    };
    t.schedule(tt, 3000);
  }

  private synchronized void startWatchDog ()
  {
    if (watchDogTimer != null) {
      log("[WARN] Watchdog already running");
      return;
    }

    watchDogTimer = new Timer();
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
          log("[ERROR] Severe error in WatchDogTimer!");
          e.printStackTrace();
        }
      }
    };

    long watchDogInt =
      Integer.parseInt(tournamentProperties
              .getProperty("scheduler.watchDogInterval", "120000"));

    watchDogTimer.schedule(watchDog, new Date(), watchDogInt);
  }

  private void stopWatchDog ()
  {
    String date = Utils.dateFormatUTC(new Date());

    if (watchDogTimer != null) {
      watchDogTimer.cancel();
      watchDogTimer = null;
      log("[INFO] {0} : Stopping WatchDog...", date);
    }
    else {
      log("[WARN] {0} : WatchDogTimer Already Stopped", date);
    }
  }

  public void restartWatchDog ()
  {
    stopWatchDog();
    startWatchDog();
  }

  private void checkMachines() {
    log("[INFO] {0} : WatchDogTimer Checking Machine States..",
        Utils.dateFormatUTC(new Date()));

    Database db = new Database();
    try {
      db.startTrans();

      // TODO Get Jenkins URL from config
      String url = "http://localhost:8080/jenkins/computer/api/xml";
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
            if (displayName.equals("master")) {
              continue;
            }

            Machine machine = db.getMachineByName(displayName);
            if (machine == null) {
              log("[WARN] Machine {0} doesn't exist in the TM", displayName);
              continue;
            }

            if (machine.isAvailable() && offline.equals("true")) {
              log("[WARN] Machine {0} is set available, but Jenkins reports"
                  + " offline", displayName);
              db.setMachineAvailable(machine.getMachineId(), false);
            }

            if (machine.stateEquals(Machine.STATE.idle) && idle.equals("false")) {
              log("[WARN] Machine {0} has status 'idle', but Jenkins reports "
                  + "'not idle'", displayName);
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
    String date = Utils.dateFormatUTC(new Date());
    if (bootRunning) {
      log("[INFO] {0} : WatchDogTimer Reports a boot is running", date);
      return;
    }
    else {
      log("[INFO] {0} : WatchDogTimer Looking for Bootstraps To Start..", date);
    }

    Database db = new Database();
    try {
      db.startTrans();
      List<Game> games = db.getBootableGames();
      log("[INFO] WatchDogTimer reports {0} boots are ready to start",
          games.size());

      if (games.size() > 0) {
        Game g = games.get(0);
        Tournament t = db.getTournamentByGameId(g.getGameId());

        log("[INFO] {0} : Boot: {1} will be started...",
            Utils.dateFormatUTC(new Date()), g.getGameId());

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
    log("[INFO] {0} : WatchDogTimer Looking for Games To Start..",
        Utils.dateFormatUTC(new Date()));

    // Check Database for startable games
    Database db = new Database();
    try {
      db.startTrans();
      List<Game> games;

      if (runningTournament == null) {
        log("[INFO] WatchDog CheckForSims ignoring multi-game tournament games");
        games = db.getRunnableGames();
      }
      else {
        log("[INFO] WatchDog CheckForSims ignoring single-game tournament games");
        games = db.getRunnableGames(runningTournament.getTournamentId());
      }
      log("[INFO] WatchDogTimer reports {0} game(s) are ready to start",
          games.size());

      for (Game g: games) {
        Tournament t = db.getTournamentByGameId(g.getGameId());
        log("[INFO] {0} : Game: {1} will be started...",
            Utils.dateFormatUTC(new Date()), g.getGameId());

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
    return this.runningTournament == null;
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
    log("[INFO] Spring Container is destroyed! Scheduler clean up");

    stopWatchDog();
  }
}