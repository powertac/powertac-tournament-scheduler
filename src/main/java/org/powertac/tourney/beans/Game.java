package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.MemStore;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Utils;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "games")
public class Game implements Serializable
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Integer gameId = 0;
  private String gameName;
  private Tournament tournament;
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

  TournamentProperties properties = TournamentProperties.getProperties();

  private Map<Integer, Agent> agentMap = new HashMap<Integer, Agent>();

  public static enum STATE
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
    for (Agent agent: agentMap.values()) {
      session.delete(agent);
      session.flush();
    }
    session.delete(this);
  }

  @Transient
  public String getBrokersInGameString ()
  {
    String result = "";

    for (Agent agent: agentMap.values()) {
      result += agent.getBroker().getBrokerName() + ", ";
    }

    if (!result.isEmpty()) {
      result = result.substring(0, result.length() - 2);
    }

    return result;
  }

  @Transient
  public String getBrokerIdsInGameString ()
  {
    String result = "";

    for (Agent agent: agentMap.values()) {
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
        log.debug("Freeing Machines for game: " + gameId);

        // Reset values when a game is aborted
        for (Agent agent: getAgentMap().values()) {
          agent.setStatePending();
          agent.setBalance(0);
          session.update(agent);
        }
        setReadyTime(null);
        MemStore.removeGameHeartbeat(gameId);
        break;

      case boot_failed:
        log.warn("BOOT " + gameId + " FAILED!");
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machines for game: " + gameId);
        break;

      case game_ready:
        tournament.setStateToInProgress();
        readyTime = Utils.offsetDate();
        break;

      case game_in_progress:
        tournament.setStateToInProgress();
        break;

      case game_complete:
        for (Agent agent: agentMap.values()) {
          agent.setStateComplete();
          session.update(agent);
        }
        log.info("Setting Agents to Complete for game: " + gameId);
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machines for game: " + gameId);
        // If all games of tournament are complete, set tournament complete
        tournament.processGameFinished(gameId);
        break;

      case game_failed:
        log.warn("GAME " + gameId + " FAILED!");
        for (Agent agent: agentMap.values()) {
          agent.setStateComplete();
          session.update(agent);
        }
        log.info("Setting Agents to Complete for game: " + gameId);
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machines for game: " + gameId);
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
    for (String result: standings.split(",")) {
      Double balance = Double.parseDouble(result.split(":")[1]);
      String name = result.split(":")[0];
      if (name.equals("default broker")) {
        continue;
      }
      results.put(name, balance);
    }

    for (Agent agent: agentMap.values()) {
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
    return Utils.dateFormat(startTime);
  }

  public String readyTimeUTC ()
  {
    return Utils.dateFormat(readyTime);
  }

  public boolean stateEquals (STATE state)
  {
    return this.state.equals(state);
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

  @Transient
  public int getGameTypeIndex ()
  {
    if (tournament.isSingle()) {
      return 0;
    }

    String[] parts = gameName.split("_");
    return Integer.parseInt(parts[parts.length - 3]) - 1;
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
      gameLength = MemStore.gameLengths.get(gameId) + 360;
      lastTick = Integer.parseInt(MemStore.gameHeartbeats.get(gameId)[0]);
    } catch (Exception ignored) {
    }
  }

  public static Game createGame (Tournament tournament, String gameName)
  {
    Game game = new Game();
    game.setGameName(gameName);
    game.setTournament(tournament);
    game.setState(STATE.boot_pending);
    game.setStartTime(tournament.getStartTime());
    game.setLocation(randomLocation(tournament));
    game.setSimStartTime(randomSimStartTime(tournament));
    game.setServerQueue(Utils.createQueueName());
    game.setVisualizerQueue(Utils.createQueueName());

    return game;
  }

  private static String randomLocation (Tournament tournament)
  {
    double randLocation = Math.random() * tournament.getLocationsList().size();
    return tournament.getLocationsList().get((int) Math.floor(randLocation));
  }

  private static String randomSimStartTime (Tournament tournament)
  {
    // TODO We need to check if dates are valid for location
    // Only usefull with multiple weather locations with different periods

    Date starting = new Date();

    // Number of msecs in a year divided by 4
    double gameLength = (3.1556926 * Math.pow(10, 10)) / 4;

    // Max amount of time between the fromTime to the toTime to start a game
    long msLength = (long) gameLength;

    if (tournament.getDateTo().getTime() - tournament.getDateFrom().getTime() < msLength) {
      // Use fromTime in all games in the tournament as the start time
      starting = tournament.getDateFrom();
    } else {
      long start = tournament.getDateFrom().getTime();
      long end = tournament.getDateTo().getTime() - msLength;
      long startTime = (long) (Math.random() * (end - start) + start);

      starting.setTime(startTime);
    }

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    return format.format(starting);
  }

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Game> getBootableSingleGames (Session session)
  {
    return (List<Game>) session
        .createQuery(Constants.HQL.GET_GAMES_SINGLE_BOOT_PENDING)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
  }

  @SuppressWarnings("unchecked")
  public static List<Game> getBootableMultiGames (Session session, Tournament tournament)
  {
    return (List<Game>) session
        .createQuery(Constants.HQL.GET_GAMES_MULTI_BOOT_PENDING)
        .setInteger("tournamentId", tournament.getTournamentId())
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
  }

  @SuppressWarnings("unchecked")
  public static List<Game> getStartableSingleGames (Session session)
  {
    return (List<Game>) session
        .createQuery(Constants.HQL.GET_GAMES_SINGLE_BOOT_COMPLETE)
        .setTimestamp("startTime", Utils.offsetDate())
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
  }

  @SuppressWarnings("unchecked")
  public static List<Game> getStartableMultiGames (Session session, Tournament tournament)
  {
    return (List<Game>) session
        .createQuery(Constants.HQL.GET_GAMES_MULTI_BOOT_COMPLETE)
        .setInteger("tournamentId", tournament.getTournamentId())
        .setTimestamp("startTime", Utils.offsetDate())
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
  }

  @SuppressWarnings("unchecked")
  public static List<Game> getNotCompleteGamesList ()
  {
    List<Game> games = new ArrayList<Game>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_GAMES_NOT_COMPLETE);
      games = (List<Game>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
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

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_GAMES_COMPLETE);
      games = (List<Game>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return games;
  }

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
  @JoinColumn(name = "tourneyId")
  public Tournament getTournament ()
  {
    return tournament;
  }
  public void setTournament (Tournament tournament)
  {
    this.tournament = tournament;
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
  //</editor-fold>
}
