package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "competitions")
public class Competition
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int competitionId;
  private String competitionName;
  private STATE state;
  private int pomId;

  private Map<Integer, CompetitionRound> roundMap =
      new HashMap<Integer, CompetitionRound>();

  public static enum STATE
  {
    open,         // This is the initial state. Accepts registrations.
    closed,       // No more registrations. Adjustments still allowed.
    scheduled0,   // Tournaments for round 1 created, no more editing.
    completed0,   // Tournaments for round 1 completed.
    scheduled1,
    completed1,
    scheduled2,
    completed2,
    scheduled3,
    completed3,
    complete;     // All the tournaments are done ( == completed3).

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

  public Competition ()
  {
    state = STATE.open;
  }

  public void scheduleTournaments (Session session)
  {
    CompetitionRound round = getCurrentRound();

    log.info("Scheduling tournaments for round " + round.getRoundNr());

    for (int i=0; i < round.getNofTournaments(); i++) {
      String tournamentName = competitionName +"_"+ round.getRoundName();
      if (round.getNofTournaments() != 1) {
        tournamentName += "_" + i;
      }
      scheduleTournament(session, tournamentName, round);
    }
  }

  private void scheduleTournament (Session session, String name,
                                   CompetitionRound round)
  {
    log.info("Creating tournament : " + name);

    int maxBrokers = 100; // Let all pass first round
    if (round.getRoundNr() > 0) {
      CompetitionRound previousRound = getPreviousRound();
      maxBrokers =
          Math.min(previousRound.getNofBrokers(), previousRound.getNofWinners())
              / round.getNofTournaments();
    }

    int size1 = round.getRoundNr() == 0 ? 1 : maxBrokers;
    int size2 = round.getRoundNr() == 0 ? 1 : Math.max(maxBrokers / 2, 2);
    int size3 = round.getRoundNr() == 0 ? 1 : Math.min(maxBrokers, 2);
    Date startDate = round.getStartTime();
    if (startDate.compareTo(Utils.offsetDate()) < 0) {
      startDate = Utils.offsetDate(1);
    }

    Tournament tournament = new Tournament();
    tournament.setTournamentName(name);
    tournament.setRound(round);
    tournament.setType(Tournament.TYPE.MULTI_GAME);
    tournament.setMaxBrokers(maxBrokers);
    tournament.setMaxAgents(2);
    tournament.setSize1(size1);
    tournament.setSize2(size2);
    tournament.setSize3(size3);
    tournament.setMultiplier1(1);
    tournament.setMultiplier2(1);
    tournament.setMultiplier3(1);
    tournament.setStartTime(startDate);
    Location location = new Location();
    tournament.setDateFrom(location.getDateFrom());
    tournament.setDateTo(location.getDateTo());
    tournament.setLocations(location.getLocation() + ",");
    tournament.setPomId(pomId);
    tournament.setClosed(round.getRoundNr() != 0);
    tournament.setStateToPending();
    session.save(tournament);

    round.getTournamentMap().put(tournament.getTournamentId(), tournament);

    log.debug("Tournament created : " + tournament.getTournamentId());
  }

  private void scheduleBrokers (Session session)
  {
    CompetitionRound previousRound = getPreviousRound();
    CompetitionRound round = getCurrentRound();

    log.info("Scheduling brokers for round " + round.getRoundNr());

    // Loop through brokers, list them in result order
    SortedSet<Map.Entry<Integer, Double>> brokerSet =
        new TreeSet<Map.Entry<Integer, Double>>(
            new Comparator<Map.Entry<Integer, Double>>() {
              @Override
              public int compare(Map.Entry<Integer, Double> e1,
                                 Map.Entry<Integer, Double> e2) {
                return e2.getValue().compareTo(e1.getValue());
              }
            });
    SortedMap<Integer, Double> brokerMap = new TreeMap<Integer, Double>();
    List<Map.Entry<Integer, Double>> brokerList =
        new ArrayList<Map.Entry<Integer, Double>>();
    List<Map.Entry<Integer, Double>> brokerListTemp =
        new ArrayList<Map.Entry<Integer, Double>>();

    int winnersPerTournament =
        previousRound.getNofWinners() / round.getNofTournaments();

    // Loop through tournaments, pick top winners
    for (Tournament tournament: previousRound.getTournamentMap().values()) {
      for (Game game: tournament.getGameMap().values()) {
        for (Agent agent: game.getAgentMap().values()) {
          if (!brokerMap.containsKey(agent.getBroker().getBrokerId())) {
            brokerMap.put(agent.getBroker().getBrokerId(), agent.getBalance());
          } else {
            brokerMap.put(agent.getBroker().getBrokerId(),
                brokerMap.get(agent.getBroker().getBrokerId()) + agent.getBalance());
          }
        }
      }

      // Select winners for this tournament
      brokerSet.clear();
      brokerSet.addAll(brokerMap.entrySet());
      brokerMap.clear();
      brokerListTemp.clear();
      brokerListTemp.addAll(brokerSet);
      winnersPerTournament = Math.min(winnersPerTournament, brokerListTemp.size());
      brokerList.addAll( brokerListTemp.subList(0, winnersPerTournament) );
    }

    log.debug("Winners from previous round : " + brokerList);

    // Randomly shuffle picked brokers into tournaments via registering
    Random randomGenerator = new Random();
    int winnerCount = Math.min(previousRound.getNofWinners(), brokerList.size());
    int brokersPerTournament = winnerCount / round.getNofTournaments();

    log.debug("winnerCount /  brokersPerTournament " +
        winnerCount + " / " + brokersPerTournament);

    for (Tournament tournament: round.getTournamentMap().values()) {
      log.debug("Tournament : " + tournament.getTournamentName());
      for (int i = 0; i < brokersPerTournament; i++) {
        Map.Entry<Integer, Double> entry =
            brokerList.remove(randomGenerator.nextInt(brokerList.size()));
        log.debug(i + " = " + entry.getKey() +" / "+ entry.getValue());
        Broker broker = (Broker) session.get(Broker.class, entry.getKey());
        broker.register(session, tournament.getTournamentId());
      }
    }
  }

  @Transient
  private CompetitionRound getCurrentRound ()
  {
    return roundMap.get(getCurrentRoundNr());
  }

  @Transient
  private CompetitionRound getPreviousRound ()
  {
    return roundMap.get(getCurrentRoundNr() - 1);
  }

  //<editor-fold desc="State methods">
  public boolean scheduleNextRound (Session session)
  {
    STATE oldState = state;

    if (state == STATE.closed) {
      state = STATE.scheduled0;
      // Tournaments are already scheduled during competition creation
      // Brokers are already scheduled via registering for the competition
    }
    else if (state == STATE.completed0) {
      state = STATE.scheduled1;
      scheduleTournaments(session);
      scheduleBrokers(session);
    }
    else if (state == STATE.completed1) {
      state = STATE.scheduled2;
      scheduleTournaments(session);
      scheduleBrokers(session);
    }
    else if (state == STATE.completed2) {
      state = STATE.scheduled3;
      scheduleTournaments(session);
      scheduleBrokers(session);
    }
    else {
      log.error("ScheduleNextRound : This shouldn't happen!!!!!");
      return false;
    }
    log.info(String.format("Changing state from %s to %s", oldState, getState()));
    return true;
  }

  public boolean completeRound ()
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
      state = STATE.complete;
    }
    else {
      log.error("CloseRound : This shouldn't happen!!!!!");
      return false;
    }
    log.info(String.format("Changing state from %s to %s", oldState, getState()));
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
    CompetitionRound round = getCurrentRound();
    for (Tournament tournament: round.getTournamentMap().values()) {
      if (!tournament.isComplete()) {
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
  public int getCurrentRoundNr () {
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
  //</editor-fold>

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Competition> getNotCompleteCompetitionList ()
  {
    List<Competition> competitions = new ArrayList<Competition>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      competitions = (List<Competition>) session
          .createQuery(Constants.HQL.GET_COMPETITION_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return competitions;
  }

  @OneToMany
  @JoinColumn(name = "competitionId")
  @MapKey(name = "roundNr")
  public Map<Integer, CompetitionRound> getRoundMap ()
  {
    return roundMap;
  }
  public void setRoundMap (Map<Integer, CompetitionRound> roundMap)
  {
    this.roundMap = roundMap;
  }
  //</editor-fold>

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "competitionId", unique = true, nullable = false)
  public int getCompetitionId ()
  {
    return competitionId;
  }
  public void setCompetitionId (int competitionId)
  {
    this.competitionId = competitionId;
  }

  @Column(name = "competitionName", nullable = false)
  public String getCompetitionName ()
  {
    return competitionName;
  }
  public void setCompetitionName (String competitionName)
  {
    this.competitionName = competitionName;
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
  //</editor-fold>
}