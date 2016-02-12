package org.powertac.tournament.beans;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.CSV;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Scheduler;
import org.powertac.tournament.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "rounds")
public class Round
{
  private int roundId;
  private String roundName;
  private Level level;
  private Date startTime;
  private Date dateFrom;
  private Date dateTo;
  private int maxBrokers;
  private int maxAgents;
  private STATE state;
  private int size1;
  private int size2;
  private int size3;
  private int multiplier1;
  private int multiplier2;
  private int multiplier3;
  private String locations;

  private Map<Integer, Game> gameMap = new HashMap<Integer, Game>();
  private Map<Integer, Broker> brokerMap = new HashMap<Integer, Broker>();

  private enum STATE
  {
    pending, in_progress, complete
  }

  public Round ()
  {
    state = STATE.pending;
  }

  public String delete ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round = (Round) session
          .createQuery(Constants.HQL.GET_ROUND_BY_ID)
          .setInteger("roundId", roundId).uniqueResult();

      Level level = round.getLevel();
      level.setNofRounds(level.getNofRounds() - 1);

      // Disallow removal when games booting or running
      for (Game game : round.gameMap.values()) {
        if (game.getState().isBooting() || game.getState().isRunning()) {
          transaction.rollback();
          return String.format("Game %s can not be removed, state = %s",
              game.getGameName(), game.getState());
        }
      }

      @SuppressWarnings("unchecked")
      List<RoundBroker> roundBrokers = (List<RoundBroker>) session
          .createCriteria(RoundBroker.class)
          .add(Restrictions.eq("round", round)).list();
      for (RoundBroker roundBroker : roundBrokers) {
        session.delete(roundBroker);
      }
      session.flush();

      for (Game game : round.gameMap.values()) {
        game.delete(session);
      }
      session.flush();

      session.delete(round);
      transaction.commit();
      return "";
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Error deleting round";
    }
    finally {
      session.close();
    }
  }

  /**
   * If a game is complete, check if it was the last one to complete
   * If so, set round state to complete
   */
  public void gameCompleted (int finishedGameId)
  {
    boolean allDone = true;

    for (Game game : gameMap.values()) {
      // The state of the finished game isn't in the db yet.
      if (game.getGameId() == finishedGameId) {
        continue;
      }
      allDone &= game.getState().isComplete();
    }

    if (allDone) {
      state = STATE.complete;
      Scheduler scheduler = Scheduler.getScheduler();
      scheduler.unloadRound(roundId);
    }

    // Always generate new CSVs
    CSV.createRoundCsv(this);
  }

  public static String getStateComplete ()
  {
    return STATE.complete.toString();
  }

  @Transient
  public int getTournamentId ()
  {
    return level.getTournament().getTournamentId();
  }

  @Transient
  public int getSize ()
  {
    return gameMap.size();
  }

  @Transient
  public int getNofBrokers ()
  {
    return brokerMap.size();
  }

  @Transient
  public String getParamString1 ()
  {
    return brokerMap.size() + " / " + maxBrokers + " : " + maxAgents;
  }

  @Transient
  public String getParamString2 ()
  {
    StringBuilder result = new StringBuilder();
    if (size1 > 0 && multiplier1 > 0) {
      result.append(size1).append(":").append(multiplier1).append(" ");
    }
    if (size2 > 0 && multiplier2 > 0) {
      result.append(size2).append(":").append(multiplier2).append(" ");
    }
    if (size3 > 0 && multiplier3 > 0) {
      result.append(size2).append(":").append(multiplier3).append(" ");
    }
    return result.toString();
  }

  @Transient
  public int getPomId ()
  {
    return level.getTournament().getPomId();
  }

  //<editor-fold desc="Winner determination">
  public Map<Broker, double[]> determineWinner ()
  {
    // Col 0  = result gameType 1
    // Col 1  = result gameType 2
    // Col 2  = result gameType 3
    // Col 3  = total not-normalized
    // Col 4  = average gameType 1
    // Col 5  = average gameType 2
    // Col 6  = average gameType 3
    // Col 7  = SD gameType 1
    // Col 8  = SD gameType 2
    // Col 9  = SD gameType 3
    // Col 10 = normalized result gameType 1
    // Col 11 = normalized result gameType 2
    // Col 12 = normalized result gameType 3
    // Col 13 = total normalized

    Map<Broker, double[]> resultMap = new HashMap<Broker, double[]>();
    double[] averages = new double[3];
    double[] SD = new double[3];

    // Get the not-normalized results into the map
    for (Game game : gameMap.values()) {
      if (!game.getState().isComplete()) {
        continue;
      }

      int gameTypeIndex = game.getGameTypeIndex();

      for (Agent agent : game.getAgentMap().values()) {
        if (!resultMap.containsKey(agent.getBroker())) {
          resultMap.put(agent.getBroker(), new double[14]);
        }

        double[] results = resultMap.get(agent.getBroker());
        results[gameTypeIndex] += agent.getBalance();
        averages[gameTypeIndex] += agent.getBalance();
        results[3] = results[0] + results[1] + results[2];
        resultMap.put(agent.getBroker(), results);
      }
    }

    averages[0] = averages[0] / resultMap.size();
    averages[1] = averages[1] / resultMap.size();
    averages[2] = averages[2] / resultMap.size();

    // Put averages in map, calculate SD
    for (Broker broker : resultMap.keySet()) {
      double[] results = resultMap.get(broker);
      results[4] = averages[0];
      results[5] = averages[1];
      results[6] = averages[2];
      resultMap.put(broker, results);

      SD[0] += Math.pow((averages[0] - results[0]), 2);
      SD[1] += Math.pow((averages[1] - results[1]), 2);
      SD[2] += Math.pow((averages[2] - results[2]), 2);
    }

    SD[0] = Math.sqrt(SD[0] / resultMap.size());
    SD[1] = Math.sqrt(SD[1] / resultMap.size());
    SD[2] = Math.sqrt(SD[2] / resultMap.size());

    // Put SDs in map, calculate normalized results and total
    for (Broker broker : resultMap.keySet()) {
      double[] results = resultMap.get(broker);
      results[7] = SD[0];
      results[8] = SD[1];
      results[9] = SD[2];

      if (results[7] > 0) {
        results[10] = (results[0] - results[4]) / results[7];
      }
      else {
        results[10] = results[0] - results[4];
      }
      if (results[8] > 0) {
        results[11] = (results[1] - results[5]) / results[8];
      }
      else {
        results[11] = results[1] - results[5];
      }
      if (results[9] > 0) {
        results[12] = (results[2] - results[6]) / results[9];
      }
      else {
        results[12] = results[2] - results[6];
      }

      results[13] = results[10] + results[11] + results[12];

      resultMap.put(broker, results);
    }

    return resultMap;
  }

  @Transient
  public Map<Broker, Result> getResultMap ()
  {
    Map<Broker, Result> resultMapRename = new HashMap<>();
    boolean dataAvailable = false;

    // Get the types in the last array
    Integer[] types = new Integer[]{
        multiplier1 != 0 ? size1 : 0,
        multiplier2 != 0 ? size2 : 0,
        multiplier3 != 0 ? size3 : 0};
    double[] meanArr = new double[3];
    double[] sdevArr = new double[3];

    // Get the not-normalized results
    for (Game game : gameMap.values()) {
      if (!game.getState().isComplete()) {
        continue;
      }
      dataAvailable = true;

      int gameSize = game.getAgentMap().size();
      int gameSizeIndex = Arrays.asList(types).indexOf(gameSize);

      for (Agent agent : game.getAgentMap().values()) {
        Broker broker = agent.getBroker();
        Result brokerResult = resultMapRename.get(broker);
        if (brokerResult == null) {
          brokerResult = new Result(broker.getBrokerName());
          resultMapRename.put(broker, brokerResult);
        }

        brokerResult.getArray0()[gameSizeIndex] += agent.getBalance();
        meanArr[gameSizeIndex] += agent.getBalance();
      }
    }

    // Get mean per game size
    for (int i = 0; i < 3; i++) {
      meanArr[i] = types[i] > 0 ? meanArr[i] / types[i] : 0;
    }

    // Get SD per game size
    for (Broker broker : resultMapRename.keySet()) {
      Result brokerResult = resultMapRename.get(broker);
      for (int i = 0; i < 3; i++) {
        sdevArr[i] += Math.pow(meanArr[i] - brokerResult.getArray0()[i], 2);
      }
    }

    for (int i = 0; i < 3; i++) {
      sdevArr[i] = Math.sqrt(sdevArr[i] / brokerMap.size());
    }

    for (Broker broker : resultMapRename.keySet()) {
      double[] notNorm = resultMapRename.get(broker).getArray0();
      double[] norm = resultMapRename.get(broker).getArray1();
      double[] totals = resultMapRename.get(broker).getArray2();

      // Calculate normalized
      for (int i = 0; i < 3; i++) {
        norm[i] = (notNorm[i] - meanArr[i]) / (sdevArr[i] > 0 ? sdevArr[i] : 1);
      }

      // Totalize
      totals[0] = notNorm[0] + notNorm[1] + notNorm[2];
      totals[1] = norm[0] + norm[1] + norm[2];
    }

    // Hijack broker 'null' for round results
    if (dataAvailable) {
      resultMapRename.put(null, new Result(types, meanArr, sdevArr));
    }

    return resultMapRename;
  }

  public List<Broker> rankList ()
  {
    final Map<Broker, double[]> winnersMap = determineWinner();

    class CustomComparator implements Comparator<Broker>
    {
      @Override
      public int compare (Broker b1, Broker b2)
      {
        return ((Double) winnersMap.get(b2)[13]).compareTo(winnersMap.get(b1)[13]);
      }
    }

    List<Broker> result = new ArrayList<Broker>(winnersMap.keySet());
    Collections.sort(result, new CustomComparator());
    return result;
  }
  //</editor-fold>

  //<editor-fold desc="State methods">
  @Transient
  public boolean isStarted ()
  {
    return startTime.before(Utils.offsetDate());
  }

  @Transient
  public boolean isPending ()
  {
    return state.equals(STATE.pending);
  }

  @Transient
  public boolean isComplete ()
  {
    return state.equals(STATE.complete);
  }

  public String startTimeUTC ()
  {
    return Utils.dateToStringMedium(startTime);
  }

  public String dateFromUTC ()
  {
    return Utils.dateToStringFull(dateFrom);
  }

  public String dateToUTC ()
  {
    return Utils.dateToStringFull(dateTo);
  }

  public void setStateToInProgress ()
  {
    this.setState(STATE.in_progress);
  }

  public void setStateToComplete ()
  {
    this.setState(STATE.complete);
  }
  //</editor-fold>

  //<editor-fold desc="Collections">
  @OneToMany
  @JoinColumn(name = "roundId")
  @MapKey(name = "gameId")
  public Map<Integer, Game> getGameMap ()
  {
    return gameMap;
  }

  public void setGameMap (Map<Integer, Game> gameMap)
  {
    this.gameMap = gameMap;
  }

  @ManyToMany
  @JoinTable(name = "round_brokers",
      joinColumns =
      @JoinColumn(name = "roundId", referencedColumnName = "roundId"),
      inverseJoinColumns =
      @JoinColumn(name = "brokerId", referencedColumnName = "brokerId")
  )
  @MapKey(name = "brokerId")
  public Map<Integer, Broker> getBrokerMap ()
  {
    return brokerMap;
  }

  public void setBrokerMap (Map<Integer, Broker> brokerMap)
  {
    this.brokerMap = brokerMap;
  }

  @Transient
  public List<String> getLocationsList ()
  {
    List<String> locationList = new ArrayList<String>();
    for (String location : locations.split(",")) {
      locationList.add(location.trim());
    }
    return locationList;
  }

  @SuppressWarnings("unchecked")
  public static List<Round> getNotCompleteRoundList ()
  {
    List<Round> rounds = new ArrayList<Round>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      rounds = (List<Round>) session
          .createQuery(Constants.HQL.GET_ROUNDS_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

      for (Round round : rounds) {
        for (Game game : round.getGameMap().values()) {
          game.getSize();
        }
      }

      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return rounds;
  }

  // Extract means and StdDevs
  public double[] getMeanSigmaArray (Map<Broker, double[]> resultMap)
  {
    if (resultMap.size() > 0) {
      Map.Entry<Broker, double[]> entry = resultMap.entrySet().iterator().next();
      double[] temp = Arrays.copyOfRange(entry.getValue(), 4, 10);
      if (!Arrays.equals(temp, new double[6])) {
        return temp;
      }
    }

    return null;
  }
  //</editor-fold>

  //<editor-fold desc="Getters and setters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "roundId", unique = true, nullable = false)
  public int getRoundId ()
  {
    return roundId;
  }

  public void setRoundId (int roundId)
  {
    this.roundId = roundId;
  }

  @Column(name = "roundName", nullable = false)
  public String getRoundName ()
  {
    return roundName;
  }

  public void setRoundName (String roundName)
  {
    this.roundName = roundName;
  }

  @ManyToOne
  @JoinColumn(name = "levelId")
  public Level getLevel ()
  {
    return level;
  }

  public void setLevel (Level level)
  {
    this.level = level;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "startTime", nullable = false, length = 10)
  public Date getStartTime ()
  {
    return startTime;
  }

  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "dateFrom", nullable = false)
  public Date getDateFrom ()
  {
    return dateFrom;
  }

  public void setDateFrom (Date dateFrom)
  {
    this.dateFrom = dateFrom;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "dateTo", nullable = false)
  public Date getDateTo ()
  {
    return dateTo;
  }

  public void setDateTo (Date dateTo)
  {
    this.dateTo = dateTo;
  }

  @Column(name = "maxBrokers", nullable = false)
  public int getMaxBrokers ()
  {
    return maxBrokers;
  }

  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  @Column(name = "maxAgents", nullable = false)
  public int getMaxAgents ()
  {
    return maxAgents;
  }

  public void setMaxAgents (int maxAgents)
  {
    this.maxAgents = maxAgents;
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

  @Column(name = "gameSize1", nullable = false)
  public int getSize1 ()
  {
    return size1;
  }

  public void setSize1 (int size1)
  {
    this.size1 = size1;
  }

  @Column(name = "gameSize2", nullable = false)
  public int getSize2 ()
  {
    return size2;
  }

  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  @Column(name = "gameSize3", nullable = false)
  public int getSize3 ()
  {
    return size3;
  }

  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  @Column(name = "multiplier1", nullable = false)
  public int getMultiplier1 ()
  {
    return multiplier1;
  }

  public void setMultiplier1 (int multiplier1)
  {
    this.multiplier1 = multiplier1;
  }

  @Column(name = "multiplier2", nullable = false)
  public int getMultiplier2 ()
  {
    return multiplier2;
  }

  public void setMultiplier2 (int multiplier2)
  {
    this.multiplier2 = multiplier2;
  }

  @Column(name = "multiplier3", nullable = false)
  public int getMultiplier3 ()
  {
    return multiplier3;
  }

  public void setMultiplier3 (int multiplier3)
  {
    this.multiplier3 = multiplier3;
  }

  @Column(name = "locations", nullable = false)
  public String getLocations ()
  {
    return locations;
  }

  public void setLocations (String locations)
  {
    this.locations = locations;
  }
  //</editor-fold>

  // Data object for broker results in a round
  public class Result
  {
    private String name;
    private double[] array0 = new double[3];
    private double[] array1 = new double[3];
    private double[] array2 = new double[3];

    public Result (String name)
    {
      this.name = name;
    }

    public Result (Integer[] types, double[] meanArr, double[] sdevArr)
    {
      array0 = meanArr;
      array1 = sdevArr;
      array2 = Arrays.stream(types).mapToDouble(x -> x).toArray();
    }

    public String getName ()
    {
      return name;
    }

    public double[] getArray0 ()
    {
      return array0;
    }

    public double[] getArray1 ()
    {
      return array1;
    }

    public double[] getArray2 ()
    {
      return array2;
    }
  }
}
