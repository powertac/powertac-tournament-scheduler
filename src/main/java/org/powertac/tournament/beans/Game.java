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

  private Map<Integer, Agent> agentMap = new HashMap<>();

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

  public static Game createGame (Round round, int counter)
  {
    String gameName = String.format("%s_%d",
        round.getLevel().getTournament().getTournamentName(), counter);

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

  public static Game getGameFromId (int gameId)
  {
    Game game = null;
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      game = getGame(session, gameId);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    finally {
      session.close();
    }

    return game;
  }

  public static Game getGame (Session session, int gameId)
  {
    Query query = session.createQuery(Constants.HQL.GET_GAME_BY_ID);
    query.setInteger("gameId", gameId);
    return (Game) query.uniqueResult();
  }

  @Transient
  public String getLogURL ()
  {
    String baseUrl = properties.getProperty("actionIndex.logUrl");
    if (baseUrl.isEmpty()) {
      return String.format("download?game=%d", gameId);
    }

    return String.format(baseUrl, gameName);
  }

  @Transient
  public String getBootURL ()
  {
    String baseUrl = properties.getProperty("actionIndex.bootUrl");
    if (baseUrl.isEmpty()) {
      return String.format("download?boot=%d", gameId);
    }

    return String.format(baseUrl, gameName);
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
