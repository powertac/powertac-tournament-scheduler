package org.powertac.tournament.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "tournaments")
public class Tournament
{
  private static Logger log = Utils.getLogger();

  private int tournamentId;
  private String tournamentName;
  private STATE state;
  private int pomId;
  private int maxAgents;

  private Map<Integer, Level> levelMap = new HashMap<Integer, Level>();
  private Map<Integer, Broker> brokerMap = new HashMap<Integer, Broker>();

  private static enum STATE
  {
    open,         // This is the initial state. Accepts registrations.
    closed,       // No more registrations. Adjustments still allowed.
    scheduled0,   // Rounds for level 1 created, no more editing.
    completed0,   // Rounds for level 1 completed.
    scheduled1,
    completed1,
    scheduled2,
    completed2,
    scheduled3,
    completed3,
    complete;     // All the levels are done.

    public static final EnumSet<STATE> editingAllowed = EnumSet.of(
        open,
        closed,
        completed0,
        completed1,
        completed2);

    public static final EnumSet<STATE> schedulingAllowed = EnumSet.of(
        closed,
        completed0,
        completed1,
        completed2);

    public static final EnumSet<STATE> completingAllowed = EnumSet.of(
        scheduled0,
        scheduled1,
        scheduled2,
        scheduled3);
  }

  public Tournament ()
  {
    state = STATE.open;
    maxAgents = 2; // default value for maximum number of concurrent agents
  }

  public void scheduleLevel (Session session)
  {
    Level level = getCurrentLevel();

    log.info("Scheduling rounds for level " + level.getLevelNr());

    for (int i=0; i < level.getNofRounds(); i++) {
      String roundName = tournamentName +"_"+ level.getLevelName();
      if (level.getNofRounds() != 1) {
        roundName += "_" + i;
      }
      scheduleRound(session, roundName, level);
    }
  }

  private void scheduleRound (Session session, String name, Level level)
  {
    log.info("Creating round : " + name);

    int nofBrokers = Math.max(100, level.getNofWinners());
    if (level.getLevelNr() != 0) {
      nofBrokers = Math.min(getPreviousLevel().getNofBrokers(),
                            getPreviousLevel().getNofWinners());
    }
    int maxBrokers = (int) Math.ceil(nofBrokers / level.getNofRounds());

    int size1 = level.getLevelNr() == 0 ? 1 : maxBrokers;
    int size2 = level.getLevelNr() == 0 ? 1 : Math.max(maxBrokers / 2, 2);
    int size3 = level.getLevelNr() == 0 ? 1 : Math.min(maxBrokers, 2);

    Date startDate = level.getStartTime();
    if (startDate.compareTo(Utils.offsetDate()) < 0) {
      startDate = Utils.offsetDate(1);
    }

    Round round = new Round();
    round.setRoundName(name);
    round.setLevel(level);
    round.setMaxBrokers(maxBrokers);
    round.setMaxAgents(level.getTournament().maxAgents);
    round.setSize1(size1);
    round.setSize2(size2);
    round.setSize3(size3);
    round.setMultiplier1(1);
    round.setMultiplier2(1);
    round.setMultiplier3(1);
    round.setStartTime(startDate);
    Location location = new Location();
    round.setDateFrom(location.getDateFrom());
    round.setDateTo(location.getDateTo());
    round.setLocations(location.getLocation() + ",");
    round.setPomId(pomId);
    session.save(round);

    level.getRoundMap().put(round.getRoundId(), round);

    log.debug("Round created : " + round.getRoundId());
  }

  private void scheduleBrokers (Session session)
  {
    Level previousLevel = getPreviousLevel();
    Level level = getCurrentLevel();

    log.info("Scheduling brokers for level " + level.getLevelNr());

    // Loop through rounds, pick top winners
    int winnersPerRound =
        previousLevel.getNofWinners() / previousLevel.getNofRounds();
    List<Broker> winners = new ArrayList<Broker>();
    for (Round round : previousLevel.getRoundMap().values()) {
      List<Broker> roundWinners = round.rankList();
      winners.addAll(roundWinners.subList(0,
          Math.min(winnersPerRound, roundWinners.size())));
    }

    log.debug("Winners from previous level : " + winners);

    // Randomly shuffle picked brokers into rounds via registering
    Random randomGenerator = new Random();
    int winnerCount = Math.min(previousLevel.getNofWinners(), winners.size());
    int brokersPerRound = winnerCount / level.getNofRounds();

    log.debug("winnerCount /  brokersPerRound " +
        winnerCount + " / " + brokersPerRound);

    for (Round round : level.getRoundMap().values()) {
      log.debug("Round : " + round.getRoundName());
      for (int i = 0; i < brokersPerRound; i++) {
        Broker broker = winners.remove(randomGenerator.nextInt(winners.size()));
        broker.registerForRound(session, round.getRoundId());
      }
    }
  }

  @Transient
  private Level getCurrentLevel ()
  {
    return levelMap.get(getCurrentLevelNr());
  }

  @Transient
  private Level getPreviousLevel ()
  {
    int currentLevelNr = getCurrentLevelNr();

    if (currentLevelNr == 0) {
      return null;
    }

    return levelMap.get(getCurrentLevelNr() - 1);
  }

  @Transient
  private Level getNextLevel ()
  {
    int currentLevelNr = getCurrentLevelNr();

    if (currentLevelNr == levelMap.size()) {
      return null;
    }

    return levelMap.get(getCurrentLevelNr() + 1);
  }

  @Transient
  public int getMaxBrokers ()
  {
    int maxBrokers = Math.max(100, levelMap.get(0).getNofWinners());
    if (levelMap.get(0).getLevelNr() != 0) {
      maxBrokers = Math.min(getPreviousLevel().getNofBrokers(),
          getPreviousLevel().getNofWinners());
    }
    return maxBrokers;
  }

  @Transient
  public int getNofBrokers ()
  {
    int nofBrokers = 0;
    for (Round round: levelMap.get(0).getRoundMap().values()) {
      nofBrokers += round.getBrokerMap().size();
    }
    return nofBrokers;
  }

  //<editor-fold desc="State methods">
  @Transient
  public boolean isStarted ()
  {
    Level firstLevel = levelMap.get(0);
    return firstLevel.isStarted();
  }

  public boolean scheduleNextLevel (Session session)
  {
    STATE oldState = state;

    if (state == STATE.closed) {
      state = STATE.scheduled0;
      // Rounds are already scheduled during tournament creation
      // Brokers are already scheduled via registering for the tournament
    }
    else if (state == STATE.completed0) {
      state = STATE.scheduled1;
      scheduleLevel(session);
      scheduleBrokers(session);
    }
    else if (state == STATE.completed1) {
      state = STATE.scheduled2;
      scheduleLevel(session);
      scheduleBrokers(session);
    }
    else if (state == STATE.completed2) {
      state = STATE.scheduled3;
      scheduleLevel(session);
      scheduleBrokers(session);
    }
    else {
      log.error("ScheduleNextlevel : This shouldn't happen!");
      return false;
    }
    log.info(String.format("Changing state from %s to %s", oldState, state));
    return true;
  }

  public boolean completeLevel ()
  {
    STATE oldState = state;

    if (state == STATE.scheduled0) {
      state = STATE.completed0;
    }
    else if (state == STATE.scheduled1) {
      state = STATE.completed1;
    }
    else if (state == STATE.scheduled2) {
      state = STATE.completed2;
    }
    else if (state == STATE.scheduled3) {
      state = STATE.completed3;
    }
    else {
      log.error("CloseLevel : This shouldn't happen!");
      return false;
    }

    Level nextLevel = getNextLevel();
    if (nextLevel == null ||
        nextLevel.getNofRounds() == 0 || nextLevel.getNofWinners() == 0) {
      state = STATE.complete;
    }

    log.info(String.format("Changing state from %s to %s", oldState, state));
    return true;
  }

  public void setStateToClosed ()
  {
    this.setState(STATE.closed);
  }

  public boolean editingAllowed ()
  {
    return STATE.editingAllowed.contains(state);
  }

  public boolean closingAllowed ()
  {
    return state.equals(STATE.open);
  }

  public boolean schedulingAllowed ()
  {
    return STATE.schedulingAllowed.contains(state);
  }

  public boolean completingAllowed ()
  {
    if (!STATE.completingAllowed.contains(state)) {
      return false;
    }

    // Loop over tourneys, check all complete
    Level level = getCurrentLevel();
    for (Round round : level.getRoundMap().values()) {
      if (!round.isComplete()) {
        return false;
      }
    }

    return true;
  }

  @Transient
  public boolean isOpen ()
  {
    return state.equals(STATE.open);
  }

  @Transient
  public boolean isComplete ()
  {
    return state.equals(STATE.complete);
  }

  @Transient
  public int getCurrentLevelNr () {
    if (state.compareTo(STATE.scheduled1) < 0) {
      return 0;
    }
    else if (state.compareTo(STATE.scheduled2) < 0) {
      return 1;
    }
    else if (state.compareTo(STATE.scheduled3) < 0) {
      return 2;
    }

    return 3;
  }

  public static String getStateComplete ()
  {
    return STATE.complete.toString();
  }
  //</editor-fold>

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Tournament> getNotCompleteTournamentList ()
  {
    List<Tournament> tournaments = new ArrayList<Tournament>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournaments = (List<Tournament>) session
          .createQuery(Constants.HQL.GET_TOURNAMENT_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return tournaments;
  }

  @OneToMany
  @JoinColumn(name = "tournamentId")
  @MapKey(name = "levelNr")
  public Map<Integer, Level> getLevelMap ()
  {
    return levelMap;
  }
  public void setLevelMap (Map<Integer, Level> levelMap)
  {
    this.levelMap = levelMap;
  }

  @ManyToMany
  @JoinTable(name = "tournament_brokers",
      joinColumns =
      @JoinColumn(name = "tournamentId", referencedColumnName = "tournamentId"),
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
  //</editor-fold>

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "tournamentId", unique = true, nullable = false)
  public int getTournamentId ()
  {
    return tournamentId;
  }
  public void setTournamentId (int tournamentId)
  {
    this.tournamentId = tournamentId;
  }

  @Column(name = "tournamentName", nullable = false)
  public String getTournamentName ()
  {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
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

  @Column(name = "pomId", nullable = false)
  public int getPomId ()
  {
    return pomId;
  }
  public void setPomId (int pomId)
  {
    this.pomId = pomId;
  }

  @Column(name = "maxAgents", nullable = false)
  public int getMaxAgents()
  {
    return maxAgents;
  }
  public void setMaxAgents(int maxAgents)
  {
    this.maxAgents = maxAgents;
  }
  //</editor-fold>
}