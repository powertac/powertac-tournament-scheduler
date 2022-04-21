package org.powertac.tournament.beans;

import org.hibernate.Criteria;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;
import org.powertac.tournament.states.RoundState;

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
import java.io.Serializable;
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
public class Round implements Serializable
{
  private int roundId;
  private String roundName;
  private Level level;
  private Date startTime;
  private Date dateFrom;
  private Date dateTo;
  private int maxBrokers;
  private int maxAgents;
  private RoundState state;
  private int size1;
  private int size2;
  private int size3;
  private int multiplier1;
  private int multiplier2;
  private int multiplier3;
  private String locations;

  private Map<Integer, Game> gameMap = new HashMap<>();
  private Map<Integer, Broker> brokerMap = new HashMap<>();

  public Round ()
  {
    state = RoundState.pending;
  }

  public String delete ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round = (Round) session
          .createQuery(Constants.HQL.GET_ROUND_BY_ID)
          .setParameter("roundId", roundId).uniqueResult();

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
      result.append(size3).append(":").append(multiplier3).append(" ");
    }
    return result.toString();
  }

  @Transient
  public int getPomId ()
  {
    return level.getTournament().getPomId();
  }

  @Transient
  public Map<Broker, Result> getResultMap ()
  {
    Map<Broker, Result> resultMap = new HashMap<>();
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
        Result brokerResult = resultMap.get(broker);
        if (brokerResult == null) {
          brokerResult = new Result(broker.getBrokerName());
          resultMap.put(broker, brokerResult);
        }

        brokerResult.getArray0()[gameSizeIndex] += agent.getBalance();
        meanArr[gameSizeIndex] += agent.getBalance();
      }
    }

    // Get mean per game size
    for (int i = 0; i < 3; i++) {
      meanArr[i] = resultMap.size() > 0 ? meanArr[i] / resultMap.size() : 0;
    }

    // Get SD per game size
    for (Broker broker : resultMap.keySet()) {
      Result brokerResult = resultMap.get(broker);
      for (int i = 0; i < 3; i++) {
        sdevArr[i] += Math.pow(meanArr[i] - brokerResult.getArray0()[i], 2);
      }
    }

    for (int i = 0; i < 3; i++) {
      sdevArr[i] = Math.sqrt(sdevArr[i] / brokerMap.size());
    }

    for (Broker broker : resultMap.keySet()) {
      double[] notNorm = resultMap.get(broker).getArray0();
      double[] norm = resultMap.get(broker).getArray1();
      double[] totals = resultMap.get(broker).getArray2();

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
      resultMap.put(null, new Result(types, meanArr, sdevArr));
    }

    return resultMap;
  }

  public List<Broker> rankList ()
  {
    // Get result, remove hijacked 'null' broker
    final Map<Broker, Result> resultMap = getResultMap();
    resultMap.remove(null);

    // Compare brokers on normalized total
    class CustomComparator implements Comparator<Broker>
    {
      @Override
      public int compare (Broker b1, Broker b2)
      {
        Result result1 = resultMap.get(b1);
        Result result2 = resultMap.get(b2);
        return ((Double) result2.array2[1]).compareTo(result1.array2[1]);
      }
    }

    List<Broker> winnerList = new ArrayList<>(resultMap.keySet());
    Collections.sort(winnerList, new CustomComparator());
    return winnerList;
  }

  public static Round getRoundFromId (int roundId)
  {
    Round round = null;
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_ROUND_BY_ID);
      query.setParameter("roundId", roundId);
      round = (Round) query.uniqueResult();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    finally {
      session.close();
    }

    return round;
  }

  //<editor-fold desc="Convenience methods">
  @Transient
  public boolean isStarted ()
  {
    return startTime.before(Utils.offsetDate());
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
    List<String> locationList = new ArrayList<>();
    for (String location : locations.split(",")) {
      locationList.add(location.trim());
    }
    return locationList;
  }

  @SuppressWarnings("unchecked")
  public static List<Round> getNotCompleteRoundList ()
  {
    List<Round> rounds = new ArrayList<>();

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
  public RoundState getState ()
  {
    return state;
  }

  public void setState (RoundState state)
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
    // Not-normalized results for the broker
    private double[] array0 = new double[3];
    // Normalized results for the broker
    private double[] array1 = new double[3];
    // Totals of normalized and not-normalized, 3th value used by 'null-broker'
    private double[] array2 = new double[3];

    private Result (String name)
    {
      this.name = name;
    }

    private Result (Integer[] types, double[] meanArr, double[] sdevArr)
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
