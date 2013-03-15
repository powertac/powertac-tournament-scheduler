package org.powertac.tourney.beans;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.CSV;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "tournaments")
public class Tournament
{
  private int tourneyId;
  private String tournamentName;
  private CompetitionRound round;
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
  private TYPE type;
  private int pomId;
  private String locations;
  private boolean closed;

  private Map<Integer, Game> gameMap = new HashMap<Integer, Game>();
  private Map<Integer, Broker> brokerMap = new HashMap<Integer, Broker>();

  private static enum STATE
  {
    pending, in_progress, complete
  }

  public static enum TYPE
  {
    SINGLE_GAME, MULTI_GAME
  }

  public Tournament ()
  {
  }

  public String delete ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = (Tournament) session
          .createQuery(Constants.HQL.GET_TOURNAMENT_BY_ID)
          .setInteger("tournamentId", tourneyId).uniqueResult();

      // Disallow removal when games booting or running
      for (Game game: tournament.gameMap.values()) {
        if (game.isBooting() || game.isRunning()) {
          transaction.rollback();
          return String.format("Game %s can not be removed, state = %s",
              game.getGameName(), game.getState());
        }
      }

      @SuppressWarnings("unchecked")
      List<Registration> registrations = (List<Registration>) session
          .createCriteria(Registration.class)
          .add(Restrictions.eq("tournament", tournament)).list();
      for (Registration registration: registrations) {
        session.delete(registration);
      }
      session.flush();

      for (Game game: tournament.gameMap.values()) {
        game.delete(session);
      }
      session.flush();

      session.delete(tournament);
      transaction.commit();
      return "";
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Error deleting tournament";
    } finally {
      session.close();
    }
  }

  /**
   * If a game is complete, check if it was the last one to complete
   * If so, set tournament state to complete
   */
  public void processGameFinished (int finishedGameId)
  {
    boolean allDone = true;

    for (Game game: gameMap.values()) {
      // The state of the finished game isn't in the db yet.
      if (game.getGameId() == finishedGameId) {
        continue;
      }
      if (!game.isComplete()) {
        allDone = false;
      }
    }

    if (allDone) {
      state = STATE.complete;

      Scheduler scheduler = Scheduler.getScheduler();
      if (scheduler.getRunningTournament() != null &&
          scheduler.getRunningTournament().getTournamentId() == tourneyId) {
        scheduler.unloadTournament();
      }
    }

    // Always generate new CSVs
    CSV.createCsv(this);
  }

  public static String getStateComplete ()
  {
    return STATE.complete.toString();
  }

  //<editor-fold desc="Winner determination">
  public Map<Broker, Double[]> determineWinner ()
  {
    if (isMulti()) {
      return determineWinnerMulti();
    } else {
      return determineWinnerSingle();
    }
  }

  private Map<Broker, Double[]> determineWinnerSingle ()
  {
    Map<Broker, Double[]> resultMap = new HashMap<Broker, Double[]>();

    for (Game game: getGameMap().values()) {
      for (Agent agent: game.getAgentMap().values()) {
        resultMap.put(agent.getBroker(), new Double[]{agent.getBalance()});
      }
    }
    return resultMap;
  }

  private Map<Broker, Double[]> determineWinnerMulti ()
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

    Map<Broker, Double[]> resultMap = new HashMap<Broker, Double[]>();
    Double[] averages = new Double[]{0.0, 0.0, 0.0};
    Double[] SD = new Double[]{0.0, 0.0, 0.0};

    // Get the not-normalized results into the map
    for (Game game: gameMap.values()) {
      int gameTypeIndex = game.getGameTypeIndex();

      for (Agent agent: game.getAgentMap().values()) {
        if (!resultMap.containsKey(agent.getBroker())) {
          resultMap.put(agent.getBroker(), new Double[]{
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});
        }

        Double[] results = resultMap.get(agent.getBroker());
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
    for (Broker broker: resultMap.keySet()) {
      Double[] results = resultMap.get(broker);
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
    for (Broker broker: resultMap.keySet()) {
      Double[] results = resultMap.get(broker);
      results[7] = SD[0];
      results[8] = SD[1];
      results[9] = SD[2];

      if (results[7] > 0) {
        results[10] = (results[0] - results[4]) / results[7];
      } else {
        results[10] = results[0] - results[4];
      }
      if (results[8] > 0) {
        results[11] = (results[1] - results[5]) / results[8];
      } else {
        results[11] = results[1] - results[5];
      }
      if (results[9] > 0) {
        results[12] = (results[2] - results[6]) / results[9];
      } else {
        results[12] = results[2] - results[6];
      }

      results[13] = results[10] + results[11] + results[12];

      resultMap.put(broker, results);
    }

    return resultMap;
  }

  public List<Broker> rankList ()
  {
    final Map<Broker, Double[]> winnersMap = determineWinner();

    class CustomComparator implements Comparator<Broker> {
      @Override
      public int compare(Broker b1, Broker b2) {
        return winnersMap.get(b2)[13].compareTo(winnersMap.get(b1)[13]);
      }
    }

    List<Broker> result = new ArrayList<Broker>(winnersMap.keySet());
    Collections.sort(result, new CustomComparator());
    return result;
  }
  //</editor-fold>

  //<editor-fold desc="Helper methods">
  @Transient
  public boolean isStarted ()
  {
    return startTime.before(Utils.offsetDate());
  }

  @Transient
  public boolean isMulti ()
  {
    return type.equals(TYPE.MULTI_GAME);
  }

  @Transient
  public boolean isSingle ()
  {
    return type.equals(TYPE.SINGLE_GAME);
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
    return Utils.dateToStringFull(startTime);
  }

  public String dateFromUTC ()
  {
    return Utils.dateToStringFull(dateFrom);
  }

  public String dateToUTC ()
  {
    return Utils.dateToStringFull(dateTo);
  }

  public void setStateToPending ()
  {
    this.setState(STATE.pending);
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
  @JoinColumn(name = "tourneyId")
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
  @JoinTable(name = "registrations",
      joinColumns =
      @JoinColumn(name = "tourneyId", referencedColumnName = "tourneyId"),
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
    for (String location: locations.split(",")) {
      locationList.add(location.trim());
    }
    return locationList;
  }

  @SuppressWarnings("unchecked")
  public static List<Tournament> getNotCompleteTournamentList ()
  {
    List<Tournament> tournaments = new ArrayList<Tournament>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournaments = (List<Tournament>) session
          .createQuery(Constants.HQL.GET_TOURNAMENTS_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

      for (Tournament tournament: tournaments) {
        for (Game game: tournament.getGameMap().values()) {
          game.getAgentMap().size();
        }
      }

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return tournaments;
  }

  public List<Double> getAvgsAndSDs (Map<Broker, Double[]> resultMap)
  {
    List<Double> result = new ArrayList<Double>();

    if (resultMap.size() > 0 && isMulti()) {
      Map.Entry<Broker, Double[]> entry = resultMap.entrySet().iterator().next();
      result.addAll(Arrays.asList(entry.getValue()).subList(4, 10));
    }

    return result;
  }
  //</editor-fold>

  //<editor-fold desc="Getters and setters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "tourneyId", unique = true, nullable = false)
  public int getTournamentId ()
  {
    return tourneyId;
  }
  public void setTournamentId (int tourneyId)
  {
    this.tourneyId = tourneyId;
  }

  @Column(name = "tourneyName", nullable = false)
  public String getTournamentName ()
  {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  @ManyToOne
  @JoinColumn(name = "roundId", nullable = true)
  public CompetitionRound getRound ()
  {
    return round;
  }
  public void setRound (CompetitionRound round)
  {
    this.round = round;
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

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  public TYPE getType ()
  {
    return type;
  }
  public void setType (TYPE type)
  {
    this.type = type;
  }

  @Column(name = "pomId", nullable = false)
  public int getPomId ()
  {
    return pomId;
  }
  public void setPomId (int pomId)
  {
    this.pomId = pomId;
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

  @Column(name = "closed", nullable = false)
  public boolean isClosed ()
  {
    return closed;
  }
  public void setClosed (boolean closed)
  {
    this.closed = closed;
  }
  //</editor-fold>
}
