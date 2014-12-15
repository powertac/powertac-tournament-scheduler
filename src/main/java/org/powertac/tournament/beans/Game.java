package org.powertac.tournament.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.TournamentProperties;
import org.powertac.tournament.services.Utils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "games")
public class Game implements Serializable
{
  private static Logger log = Utils.getLogger();

  private static TournamentProperties properties =
      TournamentProperties.getProperties();

  private Integer gameId = 0;
  private String gameName;
  private Round round;
  private Machine machine = null;
  private STATE state = STATE.boot_pending;
  private Date startTime;
  private Date readyTime;
  private String serverQueue = "";
  private String visualizerQueue = "";
  private String location = "";
  private String simStartTime;
  private Integer gameLength = 0;
  private Integer lastTick = 0;
  private Integer urgency;

  private Map<Integer, Agent> agentMap = new HashMap<Integer, Agent>();

  private Random random = new Random();

  private static enum STATE
  {
    boot_pending, boot_in_progress, boot_complete, boot_failed,
    game_pending, game_ready, game_in_progress, game_complete, game_failed;

    public static final EnumSet<STATE> hasBootstrap = EnumSet.of(
        boot_complete,
        game_pending,
        game_ready,
        game_in_progress,
        game_complete);

    public static final EnumSet<STATE> isRunning = EnumSet.of(
        game_pending,
        game_ready,
        game_in_progress);
  }

  /*
  - Boot
    Games are initially set to boot_pending.
    When the job is sent to Jenkins, the TM sets it to in_progress.
    When done the Jenkins script sets it to complete or failed, depending on
    the resulting boot file. When the TM isn't able to send the job to
    Jenkins, the game is set to failed as well.

  - Game
    When the job is sent to Jenkins, the TM sets it to game_pending.
    When the sim is ready, the sim sets the game to game_ready.
    (This is done before the game is actually started.
    That's why we delay the login of the visualizers.)
    It also sets readyTime, to give the visualizer some time to log in before
    the brokers log in. Brokers are allowed to log in when game_ready and
    readyTime + 2 minutes (so the viz is logged in).
    When all the brokers are logged in (or login timeout occurs), the sim sets
    the game to in_progress.

    When the sim stops, the Jenkins script sets the game to complete.
    game_failed occurs when the script encounters problems downloading the POM-
    or boot-file, or when RunGame has problems sending the job to jenkins.
  */

  public Game ()
  {
  }

  public void delete (Session session)
  {
    // Delete all agent belonging to this broker
    for (Agent agent : agentMap.values()) {
      session.delete(agent);
      session.flush();
    }
    session.delete(this);
  }

  @Transient
  public String getBrokersInGameString ()
  {
    String result = "";
    for (Agent agent : agentMap.values()) {
      result += agent.getBroker().getBrokerName() + ", ";
    }

    if (!result.isEmpty()) {
      result = result.substring(0, result.length() - 2);
    }

    return result;
  }

  @Transient
  @SuppressWarnings("unchecked")
  public String getBrokerIdsInGameString ()
  {
    List<Agent> agents = new ArrayList(agentMap.values());
    Collections.sort(agents, new Utils.agentIdComparator());

    String result = "";
    for (Agent agent : agents) {
      result += agent.getBroker().getBrokerId() + ", ";
    }

    if (!result.isEmpty()) {
      result = result.substring(0, result.length() - 2);
    }

    return result;
  }

  public void handleStatus (Session session, String status) throws Exception
  {
    STATE state;
    state = STATE.valueOf(status);

    if (stateEquals(state)) {
      return;
    }

    this.state = STATE.valueOf(status);
    log.info(String.format("Update game: %s to %s", gameId, status));

    switch (state) {
      case boot_in_progress:
        // Remove bootfile, it shouldn't exist anyway
        removeBootFile();
        break;

      case boot_complete:
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);

        // Reset values for aborted games
        for (Agent agent : getAgentMap().values()) {
          agent.setStatePending();
          agent.setBalance(0);
          session.update(agent);
        }
        setReadyTime(null);
        MemStore.removeGameInfo(gameId);
        break;

      case boot_failed:
        log.warn("BOOT " + gameId + " FAILED!");
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);
        break;

      case game_ready:
        readyTime = Utils.offsetDate();
        break;

      case game_in_progress:
        break;

      case game_complete:
        for (Agent agent : agentMap.values()) {
          agent.setStateComplete();
          session.update(agent);
        }
        log.info("Setting Agents to Complete for game: " + gameId);
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);
        // If all games of round are complete, set round complete
        round.gameCompleted(gameId);
        break;

      case game_failed:
        log.warn("GAME " + gameId + " FAILED!");
        for (Agent agent : agentMap.values()) {
          agent.setStateComplete();
          session.update(agent);
        }
        log.info("Setting Agents to Complete for game: " + gameId);
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);
        break;
    }
    session.update(this);
  }

  /*
   * This is called when the REST interface receives a heartbeat message (GET)
   * or a end-of-game message (POST)
   */
  public String handleStandings (Session session, String standings,
                                 boolean isEndOfGame) throws Exception
  {
    log.debug("We received standings for game " + gameId);

    if (isEndOfGame) {
      log.debug("Status of the game is " + state);

      if (!isRunning()) {
        session.getTransaction().rollback();
        log.warn("Game is not running, aborting!");
        return "error";
      }

      saveStandings();
    }

    HashMap<String, Double> results = new HashMap<String, Double>();
    for (String result : standings.split(",")) {
      Double balance = Double.parseDouble(result.split(":")[1]);
      String name = result.split(":")[0];
      if (name.equals("default broker")) {
        continue;
      }
      results.put(name, balance);
    }

    for (Agent agent : agentMap.values()) {
      Double balance = results.get(agent.getBroker().getBrokerName());
      if (balance == null || balance == Double.NaN) {
        continue;
      }
      agent.setBalance(balance);
      session.update(agent);
    }

    session.getTransaction().commit();
    return "success";
  }

  public void removeBootFile ()
  {
    String bootLocation = properties.getProperty("bootLocation") +
        gameId + "-boot.xml";
    File f = new File(bootLocation);

    if (!f.exists()) {
      return;
    }

    if (!f.canWrite()) {
      log.error("Write protected: " + bootLocation);
    }

    if (!f.delete()) {
      log.error("Failed to delete : " + bootLocation);
    }
  }

  public String startTimeUTC ()
  {
    return Utils.dateToStringFull(startTime);
  }

  public String readyTimeUTC ()
  {
    return Utils.dateToStringFull(readyTime);
  }

  @Transient
  public int getSize ()
  {
    return agentMap.size();
  }

  // Computes a random game length as outlined in the game specification
  public int computeGameLength (String name)
  {
    String p = name.toLowerCase().contains("test") ? "test" : "competition";
    int minLength = properties.getPropertyInt(p + ".minimumTimeslotCount");
    int expLength = properties.getPropertyInt(p + ".expectedTimeslotCount");

    if (expLength == minLength) {
      return minLength;
    }
    else {
      double roll = random.nextDouble();
      // compute k = ln(1-roll)/ln(1-p) where p = 1/(exp-min)
      double k = (Math.log(1.0 - roll) /
          Math.log(1.0 - 1.0 / (expLength - minLength + 1)));
      return minLength + (int) Math.floor(k);
    }
  }

  //<editor-fold desc="State methods">
  private boolean stateEquals (STATE state)
  {
    return this.state.equals(state);
  }

  @Transient
  public boolean isBooting ()
  {
    return state.equals(STATE.boot_in_progress);
  }

  @Transient
  public boolean isRunning ()
  {
    return STATE.isRunning.contains(state);
  }

  public boolean hasBootstrap ()
  {
    return STATE.hasBootstrap.contains(state);
  }

  public void setStateBootPending ()
  {
    this.state = STATE.boot_pending;
  }

  public void setStateBootInProgress ()
  {
    this.state = STATE.boot_in_progress;
  }

  public void setStateBootComplete ()
  {
    this.state = STATE.boot_complete;
  }

  public void setStateBootFailed ()
  {
    this.state = STATE.boot_failed;
  }

  public void setStateGamePending ()
  {
    this.state = STATE.game_pending;
  }

  public void setStateGameFailed ()
  {
    this.state = STATE.game_failed;
  }

  @Transient
  public boolean isComplete ()
  {
    return stateEquals(STATE.game_complete);
  }

  @Transient
  public boolean isReady ()
  {
    return stateEquals(STATE.game_ready);
  }

  @Transient
  public boolean isBootFailed ()
  {
    return stateEquals(STATE.boot_failed);
  }

  @Transient
  public boolean isGameFailed ()
  {
    return stateEquals(STATE.game_failed);
  }

  @Transient
  public boolean isFailed ()
  {
    return isBootFailed() || isGameFailed();
  }

  public static String getStateBootPending ()
  {
    return STATE.boot_pending.toString();
  }

  public static String getStateBootInProgress ()
  {
    return STATE.boot_in_progress.toString();
  }

  public static String getStateBootComplete ()
  {
    return STATE.boot_complete.toString();
  }

  public static String getStateGamePending ()
  {
    return STATE.game_pending.toString();
  }

  public static String getStateGameReady ()
  {
    return STATE.game_ready.toString();
  }

  public static String getStateGameComplete ()
  {
    return STATE.game_complete.toString();
  }
  //</editor-fold>

  // These 2 methods (createGameName and getGameTypeIndex) are tightly coupled
  @Transient
  public int getGameTypeIndex ()
  {
    String[] parts = gameName.split("_");
    return Integer.parseInt(parts[parts.length - 3]) - 1;
  }

  public static String createGameName (String roundName, int gameNumber,
                                       int gameType, int counter, int offset)
  {
    return String.format("%s_%s_%s_%s",
        roundName, (gameNumber + 1), gameType, counter + offset);
  }

  public String jenkinsMachineUrl ()
  {
    if (machine == null) {
      return "";
    }

    return String.format("%scomputer/%s/",
        properties.getProperty("jenkins.location"),
        machine.getMachineName());
  }

  private void saveStandings ()
  {
    try {
      gameLength = MemStore.getGameLengths().get(gameId);
      lastTick = Integer.parseInt(MemStore.getGameHeartbeats().get(gameId)[0]);
    }
    catch (Exception ignored) {
    }
  }

  public static Game createGame (Round round, String gameName)
  {
    Game game = new Game();
    game.setGameName(gameName);
    game.setRound(round);
    game.setState(STATE.boot_pending);
    game.setStartTime(round.getStartTime());
    game.setLocation(randomLocation(round));
    game.setSimStartTime(randomSimStartTime(game.getLocation()));
    game.setServerQueue(Utils.createQueueName());
    game.setVisualizerQueue(Utils.createQueueName());

    return game;
  }

  private static String randomLocation (Round round)
  {
    double randLocation = Math.random() * round.getLocationsList().size();
    return round.getLocationsList().get((int) Math.floor(randLocation));
  }

  private static String randomSimStartTime (String locationString)
  {
    Location location = Location.getLocationByName(locationString);
    long dateTo = location.getDateTo().getTime();
    long dateFrom = location.getDateFrom().getTime();

    // Number of msecs in a year divided by 4
    double gameLength = (3.1556926 * Math.pow(10, 10)) / 4;

    // Max amount of time between the fromTime to the toTime to start a game
    long msLength = (long) gameLength;

    Date starting = new Date();
    if ((dateTo - dateFrom) < msLength) {
      // Use fromTime in all games in the round as the start time
      starting.setTime(dateFrom);
    }
    else {
      long end = dateTo - msLength;
      long startTime = (long) (Math.random() * (end - dateFrom) + dateFrom);
      starting.setTime(startTime);
    }

    return Utils.dateToStringSmall(starting);
  }

  //<editor-fold desc="Collections">
  @OneToMany
  @JoinColumn(name = "gameId")
  @MapKey(name = "brokerId")
  public Map<Integer, Agent> getAgentMap ()
  {
    return agentMap;
  }

  public void setAgentMap (Map<Integer, Agent> agentMap)
  {
    this.agentMap = agentMap;
  }

  @SuppressWarnings("unchecked")
  public static List<Game> getNotCompleteGamesList ()
  {
    List<Game> games = new ArrayList<Game>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_GAMES_NOT_COMPLETE);
      games = (List<Game>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return games;
  }

  @SuppressWarnings("unchecked")
  public static List<Game> getCompleteGamesList ()
  {
    List<Game> games = new ArrayList<Game>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_GAMES_COMPLETE);
      games = (List<Game>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return games;
  }
  //</editor-fold>

  //<editor-fold desc="Setter and getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "gameId", unique = true, nullable = false)
  public Integer getGameId ()
  {
    return gameId;
  }

  public void setGameId (Integer gameId)
  {
    this.gameId = gameId;
  }

  @Column(name = "gameName")
  public String getGameName ()
  {
    return gameName;
  }

  public void setGameName (String gameName)
  {
    this.gameName = gameName;
  }

  @ManyToOne
  @JoinColumn(name = "roundId")
  public Round getRound ()
  {
    return round;
  }

  public void setRound (Round round)
  {
    this.round = round;
  }

  @ManyToOne
  @JoinColumn(name = "machineId")
  public Machine getMachine ()
  {
    return machine;
  }

  public void setMachine (Machine machine)
  {
    this.machine = machine;
  }

  @Column(name = "state", nullable = false)
  @Enumerated(EnumType.STRING)
  public STATE getState ()
  {
    return state;
  }

  public void setState (STATE state)
  {
    this.state = state;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "startTime")
  public Date getStartTime ()
  {
    return startTime;
  }

  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "readyTime")
  public Date getReadyTime ()
  {
    return readyTime;
  }

  public void setReadyTime (Date readyTime)
  {
    this.readyTime = readyTime;
  }

  @Column(name = "visualizerQueue")
  public String getVisualizerQueue ()
  {
    return visualizerQueue;
  }

  public void setVisualizerQueue (String name)
  {
    this.visualizerQueue = name;
  }

  @Column(name = "serverQueue")
  public String getServerQueue ()
  {
    return serverQueue;
  }

  public void setServerQueue (String name)
  {
    this.serverQueue = name;
  }

  @Column(name = "location")
  public String getLocation ()
  {
    return location;
  }

  public void setLocation (String location)
  {
    this.location = location;
  }

  @Column(name = "simStartDate")
  public String getSimStartTime ()
  {
    return simStartTime;
  }

  public void setSimStartTime (String simStartTime)
  {
    this.simStartTime = simStartTime;
  }

  @Column(name = "gameLength")
  public Integer getGameLength ()
  {
    return gameLength;
  }

  public void setGameLength (Integer gameLength)
  {
    this.gameLength = gameLength;
  }

  @Column(name = "lastTick")
  public Integer getLastTick ()
  {
    return lastTick;
  }

  public void setLastTick (Integer lastTick)
  {
    this.lastTick = lastTick;
  }

  @Transient
  public Integer getUrgency ()
  {
    return urgency;
  }

  public void setUrgency (int urgency)
  {
    this.urgency = urgency;
  }
  //</editor-fold>

  // Used by timeline
  @Override
  public String toString ()
  {
    return gameId + " : " + getBrokerIdsInGameString();
  }
}
