package org.powertac.tournament.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Scheduler;
import org.powertac.tournament.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "brokers")
public class Broker
{
  private static Logger log = Utils.getLogger();

  private Integer brokerId;
  private String brokerName;
  private User user;
  private String brokerAuth;
  private String shortDescription;

  private Map<Integer, Agent> agentMap = new HashMap<Integer, Agent>();
  private Map<Integer, Round> roundMap = new HashMap<Integer, Round>();
  private Map<Integer, Tournament> tournamentMap = new HashMap<Integer, Tournament>();

  // For edit mode, web interface
  private boolean edit;

  // For registration, web interface
  private int registerRoundId;
  private int unregisterRoundId;
  private int registerTournamentId;
  private int unregisterTournamentId;

  public Broker ()
  {
  }

  public boolean save ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(this);
      transaction.commit();
      return true;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    }
    finally {
      session.close();
    }
  }

  public String update ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.update(this);
      transaction.commit();
      return null;
    }
    catch (ConstraintViolationException cve) {
      transaction.rollback();
      return cve.getMessage();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Error updating broker";
    }
    finally {
      session.close();
    }
  }

  public boolean delete ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Broker broker = (Broker) session
          .createQuery(Constants.HQL.GET_BROKER_BY_ID)
          .setInteger("brokerId", brokerId).uniqueResult();

      // Delete all agent belonging to this broker
      for (Agent agent : broker.agentMap.values()) {
        // Don't allow deleting brokers with agents in running games
        if (agent.getGame().isRunning()) {
          transaction.rollback();
          return false;
        }

        session.delete(agent);
        session.flush();
      }

      // Delete all registrations to this broker
      for (Round round : broker.getRoundMap().values()) {
        RoundBroker roundBroker = (RoundBroker) session
            .createCriteria(RoundBroker.class)
            .add(Restrictions.eq("round", round))
            .add(Restrictions.eq("broker", broker)).uniqueResult();
        session.delete(roundBroker);
        session.flush();
      }

      session.delete(broker);
      transaction.commit();
      return true;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    }
    finally {
      session.close();
    }
  }

  public boolean registerForRound (int roundId)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      registerForRound(session, roundId);
      transaction.commit();
      return true;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    }
    finally {
      session.close();
    }
  }

  public void registerForRound (Session session, int roundId)
  {
    Round round = (Round) session.get(Round.class, roundId);
    RoundBroker roundBroker = new RoundBroker();
    roundBroker.setBroker(this);
    roundBroker.setRound(round);
    session.save(roundBroker);
    log.info(String.format("Registering broker: %s with round: %s",
        brokerId, round.getRoundId()));
  }

  public boolean unregisterFromRound (int roundId)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round =
          (Round) session.get(Round.class, roundId);

      // Can't unregister from games that already started (or should have)
      if (round.isStarted()) {
        transaction.rollback();
        return false;
      }

      RoundBroker roundBroker = (RoundBroker) session
          .createCriteria(RoundBroker.class)
          .add(Restrictions.eq("round", round))
          .add(Restrictions.eq("broker", this)).uniqueResult();
      session.delete(roundBroker);

      transaction.commit();
      return true;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    }
    finally {
      session.close();
    }
  }

  public boolean registerForTournament (int tournamentId)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      registerForTournament(session, tournamentId);
      transaction.commit();
      return true;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    }
    finally {
      session.close();
    }
  }

  public void registerForTournament (Session session, int tournamentId)
  {
    Tournament tournament =
        (Tournament) session.get(Tournament.class, tournamentId);
    TournamentBroker tournamentBroker = new TournamentBroker();
    tournamentBroker.setBroker(this);
    tournamentBroker.setTournament(tournament);
    session.save(tournamentBroker);
    log.info(String.format("Registering broker: %s with tournament: %s",
        brokerId, tournament.getTournamentId()));
  }

  public boolean unRegisterFromTournament (int tournamentId)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament =
          (Tournament) session.get(Tournament.class, tournamentId);

      // Can't unregister if rounds already started
      if (tournament.isStarted()) {
        transaction.rollback();
        return false;
      }

      // Delete link between broker and tournament
      TournamentBroker tournamentBroker = (TournamentBroker) session
          .createCriteria(TournamentBroker.class)
          .add(Restrictions.eq("tournament", tournament))
          .add(Restrictions.eq("broker", this)).uniqueResult();
      if (tournamentBroker != null) {
        session.delete(tournamentBroker);
      }

      List<Integer> deleteAgents = new ArrayList<Integer>();
      for (Level level : tournament.getLevelMap().values()) {
        for (Round round : level.getRoundMap().values()) {
          // Delete link between broker and round
          RoundBroker roundBroker = (RoundBroker) session
              .createCriteria(RoundBroker.class)
              .add(Restrictions.eq("round", round))
              .add(Restrictions.eq("broker", this)).uniqueResult();

          if (roundBroker != null) {
            session.delete(roundBroker);
          }

          // Delete link between brokers agent and game
          for (Game game : round.getGameMap().values()) {
            for (Agent agent : game.getAgentMap().values()) {
              if (agent.getBrokerId() == brokerId) {
                deleteAgents.add(agent.getAgentId());
              }
            }
          }
        }
      }
      for (Integer agentId : deleteAgents) {
        Agent agent = (Agent) session.load(Agent.class, agentId);
        session.delete(agent);
        session.flush();
      }

      transaction.commit();
      return true;
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    }
    finally {
      session.close();
    }
  }

  // Check if there are still agents available
  public boolean hasAgentsAvailable (Round round)
  {
    Scheduler scheduler = Scheduler.getScheduler();
    List<Integer> runningRoundIds = scheduler.getRunningRoundIds();

    // Shouldn't happen (no games should start when no round loaded)
    // Or there are running rounds, but this one is not (should also not happen)
    if (runningRoundIds.size() == 0 ||
        !Scheduler.getScheduler().getRunningRoundIds().contains(round.getRoundId())) {
      return false;
    }

    // Round is running, now check for free agents
    int freeAgents = round.getLevel().getTournament().getMaxAgents();
    for (Agent agent : agentMap.values()) {
      Game game = agent.getGame();
      if (game.isRunning() &&
          round.getTournamentId() == game.getRound().getTournamentId()) {
        // every agent that is in a game that is running and from the same
        // tournament reduces the amount of availabele agents.
        freeAgents--;
      }
    }

    return freeAgents > 0;
  }

  @Transient
  public String getTournamentsString (boolean useId)
  {
    Set<String> tournaments = new HashSet<String>();

    for (Tournament tournament : tournamentMap.values()) {
      if (!tournament.isComplete() && useId) {
        tournaments.add(String.valueOf(tournament.getTournamentId()));
      }
      else if (!tournament.isComplete() && !useId) {
        tournaments.add(tournament.getTournamentName());
      }
    }

    return tournaments.toString().replace("[", "").replace("]", "");
  }

  @Transient
  public String getRoundsString (boolean useId)
  {
    Set<String> rounds = new HashSet<String>();

    for (Round round : roundMap.values()) {
      if (!round.isComplete() && useId) {
        rounds.add(String.valueOf(round.getRoundId()));
      }
      else if (!round.isComplete() && !useId) {
        rounds.add(round.getRoundName());
      }
    }

    return rounds.toString().replace("[", "").replace("]", "");
  }

  @Transient
  @SuppressWarnings("unchecked")
  public String getRunningGamesString ()
  {
    Set<Integer> gameIds = new HashSet<Integer>();

    for (Agent agent : agentMap.values()) {
      if (agent.isInProgress()) {
        gameIds.add(agent.getGameId());
      }
    }

    return gameIds.toString().replace("[", "").replace("]", "");
  }

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Broker> getBrokerList ()
  {
    List<Broker> brokers = new ArrayList<Broker>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_BROKERS);
      brokers = (List<Broker>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return brokers;
  }

  @Transient
  public List<Round> getAvailableRounds (List<Round> roundList)
  {
    List<Round> registrableRounds = new ArrayList<Round>();

    for (Round round : roundList) {
      // Check if maxNofBrokers reached
      if (round.getBrokerMap().size() >= round.getMaxBrokers()) {
        continue;
      }
      // Check if round hasn't started
      if (!round.isPending()) {
        continue;
      }
      // Check if already registered
      if (roundMap.get(round.getRoundId()) != null) {
        continue;
      }

      // Check if broker is registered for this tournament
      if (tournamentMap.get(round.getTournamentId()) == null) {
        continue;
      }

      // No reason not to be able to register
      registrableRounds.add(round);
    }

    return registrableRounds;
  }

  @Transient
  public List<Round> getRegisteredRounds ()
  {
    List<Round> registeredRounds = new ArrayList<Round>();

    for (Round round : roundMap.values()) {
      if (round.isComplete()) {
        continue;
      }
      if (round.isStarted()) {
        continue;
      }

      registeredRounds.add(round);
    }

    return registeredRounds;
  }

  @Transient
  public List<Tournament> getAvailableTournaments (List<Tournament> tournamentList)
  {
    List<Tournament> registrableTournaments = new ArrayList<Tournament>();

    for (Tournament tournament : tournamentList) {
      // Can't register if already started
      if (tournament.isStarted()) {
        continue;
      }

      // Check if maxNofBrokers reached
      if (tournament.getNofBrokers() >= tournament.getMaxBrokers()) {
        continue;
      }

      // Check if already registered
      if (tournamentMap.get(tournament.getTournamentId()) != null) {
        continue;
      }

      // No reason not to be able to register
      registrableTournaments.add(tournament);
    }

    return registrableTournaments;
  }

  @Transient
  public List<Tournament> getRegisteredTournaments ()
  {
    List<Tournament> registeredTournaments = new ArrayList<Tournament>();

    for (Tournament tournament : tournamentMap.values()) {
      if (tournament.isComplete()) {
        continue;
      }

      if (tournament.isStarted()) {
        continue;
      }

      registeredTournaments.add(tournament);
    }

    return registeredTournaments;
  }

  public static Broker getBrokerByName (String brokerName)
  {
    Broker broker = null;
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_BROKER_BY_NAME);
      query.setString("brokerName", brokerName);
      broker = (Broker) query.uniqueResult();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
    return broker;
  }

  @OneToMany
  @JoinColumn(name = "brokerId")
  @MapKey(name = "gameId")
  public Map<Integer, Agent> getAgentMap ()
  {
    return agentMap;
  }

  public void setAgentMap (Map<Integer, Agent> agentMap)
  {
    this.agentMap = agentMap;
  }

  @ManyToMany
  @JoinTable(name = "round_brokers",
      joinColumns =
      @JoinColumn(name = "brokerId", referencedColumnName = "brokerId"),
      inverseJoinColumns =
      @JoinColumn(name = "roundId", referencedColumnName = "roundId")
  )
  @MapKey(name = "roundId")
  public Map<Integer, Round> getRoundMap ()
  {
    return roundMap;
  }

  public void setRoundMap (Map<Integer, Round> roundMap)
  {
    this.roundMap = roundMap;
  }

  @ManyToMany
  @JoinTable(name = "tournament_brokers",
      joinColumns =
      @JoinColumn(name = "brokerId", referencedColumnName = "brokerId"),
      inverseJoinColumns =
      @JoinColumn(name = "tournamentId", referencedColumnName = "tournamentId")
  )
  @MapKey(name = "tournamentId")
  public Map<Integer, Tournament> getTournamentMap ()
  {
    return tournamentMap;
  }

  public void setTournamentMap (Map<Integer, Tournament> tournamentMap)
  {
    this.tournamentMap = tournamentMap;
  }
  //</editor-fold>

  //<editor-fold desc="Bean Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "brokerId", unique = true, nullable = false)
  public Integer getBrokerId ()
  {
    return brokerId;
  }

  public void setBrokerId (Integer brokerId)
  {
    this.brokerId = brokerId;
  }

  @ManyToOne
  @JoinColumn(name = "userId")
  public User getUser ()
  {
    return user;
  }

  public void setUser (User user)
  {
    this.user = user;
  }

  @Column(name = "brokerName", nullable = false)
  public String getBrokerName ()
  {
    return brokerName;
  }

  public void setBrokerName (String brokerName)
  {
    this.brokerName = brokerName;
  }

  @Column(name = "brokerAuth", unique = true, nullable = false)
  public String getBrokerAuth ()
  {
    return brokerAuth;
  }

  public void setBrokerAuth (String brokerAuth)
  {
    this.brokerAuth = brokerAuth;
  }

  @Column(name = "brokerShort", nullable = false)
  public String getShortDescription ()
  {
    return shortDescription;
  }

  public void setShortDescription (String shortDescription)
  {
    this.shortDescription = shortDescription;
  }
  //</editor-fold>

  //<editor-fold desc="Web Setters and Getters">
  @Transient
  public boolean isEdit ()
  {
    return edit;
  }

  public void setEdit (boolean edit)
  {
    this.edit = edit;
  }

  @Transient
  public int getRegisterRoundId ()
  {
    return registerRoundId;
  }

  public void setRegisterRoundId (int selectedTourneyRegister)
  {
    this.registerRoundId = selectedTourneyRegister;
  }

  @Transient
  public int getUnregisterRoundId ()
  {
    return unregisterRoundId;
  }

  public void setUnregisterRoundId (int unregisterRoundId)
  {
    this.unregisterRoundId = unregisterRoundId;
  }

  @Transient
  public int getRegisterTournamentId ()
  {
    return registerTournamentId;
  }

  public void setRegisterTournamentId (int registerTournamentId)
  {
    this.registerTournamentId = registerTournamentId;
  }

  @Transient
  public int getUnregisterTournamentId ()
  {
    return unregisterTournamentId;
  }

  public void setUnregisterTournamentId (int selectedUnregisterTournamentId)
  {
    this.unregisterTournamentId = selectedUnregisterTournamentId;
  }
  //</editor-fold>
}
