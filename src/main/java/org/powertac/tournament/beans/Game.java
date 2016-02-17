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
import org.powertac.tournament.states.GameState;

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
  private GameState state = GameState.boot_pending;
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
    StringBuilder result = new StringBuilder();
    String prefix = "";

    for (Agent agent : agentMap.values()) {
      result.append(prefix).append(agent.getBroker().getBrokerName());
      prefix = ", ";
    }

    return result.toString();
  }

  @Transient
  @SuppressWarnings("unchecked")
  public String getBrokerIdsInGameString ()
  {
    List<Agent> agents = new ArrayList(agentMap.values());
    Collections.sort(agents, new Utils.agentIdComparator());

    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (Agent agent : agents) {
      result.append(prefix).append(agent.getBroker().getBrokerId());
      prefix = ", ";
    }

    return result.toString();
  }

  /*
  public void handleStatus (Session session, String status) throws Exception
  {
    GameState newState = GameState.valueOf(status);

    if (newState.equals(state)) {
      return;
    }

    state = newState;
    log.info(String.format("Update game: %s to %s", gameId, status));

    switch (newState) {
      case boot_in_progress:
        // Remove bootfile, it shouldn't exist anyway
        removeBootFile();
        break;

      case boot_complete:
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);

        // Reset values for aborted games
        for (Agent agent : agentMap.values()) {
          agent.setState(AgentState.pending);
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
          agent.setState(AgentState.complete);
          session.update(agent);
        }
        log.info("Setting Agents to Complete for game: " + gameId);
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);
        // If all games of round are complete, set round complete
        new RoundScheduler(round).gameCompleted(gameId);
        MemStore.removeGameInfo(gameId);
        break;

      case game_failed:
        log.warn("GAME " + gameId + " FAILED!");
        for (Agent agent : agentMap.values()) {
          agent.setState(AgentState.complete);
          session.update(agent);
        }
        log.info("Setting Agents to Complete for game: " + gameId);
        Machine.delayedMachineUpdate(machine, 10);
        machine = null;
        log.debug("Freeing Machine for game: " + gameId);
        MemStore.removeGameInfo(gameId);
        break;
    }
    session.update(this);
  }
  */

  /*
   * This is called when the REST interface receives a heartbeat message (GET)
   * or a end-of-game message (POST)
   */
  /*
  public String handleStandings (Session session, String standings,
                                 boolean isEndOfGame) throws Exception
  {
    log.debug("We received standings for game " + gameId);

    if (isEndOfGame) {
      log.debug("Status of the game is " + state);

      if (!state.isRunning()) {
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
  */

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
  public int computeGameLength ()
  {
    String p = round.getRoundName().toLowerCase()
        .contains("test") ? "test" : "competition";
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

  public static String createGameName (String roundName, int type, int counter)
  {
    return String.format("%s_%d_%d", roundName, type, counter);
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

  public void saveStandings ()
  {
    try {
      int bootLength = properties.getPropertyInt("bootLength");
      gameLength = MemStore.getGameLengths().get(gameId) - bootLength;
      lastTick = Integer.parseInt(MemStore.getGameHeartbeats().get(gameId)[0])
          - bootLength;
    }
    catch (Exception ignored) {
    }
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

  public static Game createGame (Round round, String gameName)
  {
    Game game = new Game();
    game.setGameName(gameName);
    game.setRound(round);
    game.setState(GameState.boot_pending);
    game.setStartTime(round.getStartTime());
    game.setLocation(randomLocation(round));
    game.setSimStartTime(randomSimStartTime(game.getLocation()));
    game.setServerQueue(Utils.createQueueName());
    game.setVisualizerQueue(Utils.createQueueName());

    return game;
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
  public GameState getState ()
  {
    return state;
  }

  public void setState (GameState state)
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
