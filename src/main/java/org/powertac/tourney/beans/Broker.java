package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.MemStore;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;

@ManagedBean
@Entity
@Table(name = "brokers")
public class Broker
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Integer brokerId;
  private String brokerName;
  private User user;
  private String brokerAuth;
  private String shortDescription;

  private Map<Integer, Agent> agentMap = new HashMap<Integer, Agent>();
  private Map<Integer, Round> roundMap = new HashMap<Integer, Round>();

  // For edit mode, web interface
  private boolean edit;
  private String newName;
  private String newAuth;
  private String newShort;
  // For registration, web interface
  private int selectedRoundRegister;
  private int selectedRoundUnregister;

  public Broker ()
  {
  }

  public boolean save ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(this);
      transaction.commit();
      return true;
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    } finally {
      session.close();
    }
  }

  public String update ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.update(this);
      transaction.commit();
      return null;
    } catch (ConstraintViolationException cve) {
      transaction.rollback();
      return cve.getMessage();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Error updating broker";
    } finally {
      session.close();
    }
  }

  public boolean delete ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Broker broker = (Broker) session
          .createQuery(Constants.HQL.GET_BROKER_BY_ID)
          .setInteger("brokerId", brokerId).uniqueResult();

      // Delete all agent belonging to this broker
      for (Agent agent: broker.agentMap.values()) {
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
        Registration registration = (Registration) session
            .createCriteria(Registration.class)
            .add(Restrictions.eq("getRound", round))
            .add(Restrictions.eq("broker", broker)).uniqueResult();
        session.delete(registration);
        session.flush();
      }

      session.delete(broker);
      transaction.commit();
      return true;
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    } finally {
      session.close();
    }
  }

  public boolean register (int roundId)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      register(session, roundId);
      transaction.commit();
      return true;
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    } finally {
      session.close();
    }
  }

  public void register (Session session, int roundId)
  {
    Round round =
        (Round) session.get(Round.class, roundId);
    Registration registration = new Registration();
    registration.setBroker(this);
    registration.setRound(round);
    session.save(registration);
    log.info(String.format("Registering broker: %s with round: %s",
        brokerId, round.getRoundId()));

    // Only for single game, the scheduler handles multigame tourneys
    if (round.isSingle()) {
      for (Game game: round.getGameMap().values()) {
        Agent agent = Agent.createAgent(this, game);
        session.save(agent);
        log.info(String.format("Registering broker: %s with game: %s",
            brokerId, game.getGameId()));
      }
    }
  }

  public boolean unregister (int roundId)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Round round =
          (Round) session.get(Round.class, roundId);

      // Can't unregister from games that already started (or should have)
      if (round.isStarted()) {
        transaction.rollback();
        return false;
      }

      Registration registration = (Registration) session
          .createCriteria(Registration.class)
          .add(Restrictions.eq("getRound", round))
          .add(Restrictions.eq("broker", this)).uniqueResult();
      session.delete(registration);

      List<Integer> deleteAgents = new ArrayList<Integer>();
      for (Agent agent: agentMap.values()) {
        if (agent.getGame().getRound().getRoundId() == roundId) {
          deleteAgents.add(agent.getAgentId());
        }
      }

      for (Integer agentId: deleteAgents) {
        Agent agent = (Agent) session.load(Agent.class, agentId);
        session.delete(agent);
        session.flush();
      }

      transaction.commit();
      return true;
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return false;
    } finally {
      session.close();
    }
  }

  // Check if not more than maxBrokers are running
  public boolean hasAgentsAvailable ()
  {
    Scheduler scheduler = Scheduler.getScheduler();
    Round runningRound = scheduler.getRunningRound();

    // When running SINGLE_GAMES rounds, always assume true
    if (runningRound == null) {
      return true;
    }

    int freeAgents = runningRound.getMaxAgents();
    for (Agent agent: agentMap.values()) {
      if (agent.getGame().isRunning()) {
        freeAgents--;
      }
    }

    return freeAgents > 0;
  }

  @Transient
  public String getRegisteredString ()
  {
    String result = "";

    for (Round round : roundMap.values()) {
      if (!round.isComplete()) {
        result += round.getRoundName() + ", ";
      }
    }
    if (!result.isEmpty()) {
      result = result.substring(0, result.length() - 2);
    }

    return result;
  }

  @Transient
  @SuppressWarnings("unchecked")
  public String getRunningString ()
  {
    List<Agent> agents = new ArrayList(agentMap.values());
    Collections.sort(agents, new Utils.agentIdComparator());

    String result = "";
    for (Agent agent: agents) {
      if (agent.isInProgress()) {
        result += agent.getGameId() + ", ";
      }
    }

    if (!result.isEmpty()) {
      result = result.substring(0, result.length() - 2);
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public static List<Broker> getBrokerList ()
  {
    List<Broker> brokers = new ArrayList<Broker>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_BROKERS);
      brokers = (List<Broker>) query.
          setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return brokers;
  }

  @Transient
  public List<Round> getAvailableRounds (Boolean accountPage)
  {
    List<Round> registrableRounds = new ArrayList<Round>();

    TournamentProperties properties = TournamentProperties.getProperties();
    long loginDeadline = properties.getPropertyInt("loginDeadline", "3600000");
    long nowStamp = Utils.offsetDate().getTime();

    Outer:
    for (Round round : Round.getNotCompleteRoundList()) {
      // Check if maxNofBrokers reached
      if (round.getBrokerMap().size() >= round.getMaxBrokers()) {
        continue;
      }
      // Check if after deadline
      long diff = round.getStartTime().getTime() - nowStamp;
      if (diff < loginDeadline) {
        continue;
      }
      // Check if already registered
      for (Round t: roundMap.values()) {
        // Check if already registered
        if (t.getRoundId() == round.getRoundId()) {
          continue Outer;
        }
      }
      // Check if not closed
      if (accountPage && round.isClosed()) {
        continue;
      }

      // Check if not part of a competition
      if (accountPage && round.getLevel() != null) {
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

  public static Broker getBrokerByName (String brokerName)
  {
    Broker broker = null;
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_BROKER_BY_NAME);
      query.setString("brokerName", brokerName);
      broker = (Broker) query.uniqueResult();
      transaction.commit();
    } catch (Exception e) {
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
  @JoinTable(name = "registrations",
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

  // This creates a map with brokerId <--> # of free agents
  @SuppressWarnings("unchecked")
  public static Map<Integer, Integer> getBrokerAvailability (Session session,
                                                             int maxAgents)
  {
    Map<Integer, Integer> result = new HashMap<Integer, Integer>();

    List<Broker> brokers = (List<Broker>) session
        .createQuery(Constants.HQL.GET_BROKERS).list();

    for (Broker broker: brokers) {
      int brokerId = broker.getBrokerId();
      if (!MemStore.getBrokerState(brokerId)) {
        result.put(brokerId, 0);
        continue;
      }

      result.put(brokerId, maxAgents);
      for (Agent agent: broker.getAgentMap().values()) {
        if (agent.getGame().isRunning()) {
          result.put(brokerId, result.get(brokerId) - 1);
        }
      }
    }

    return result;
  }

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
  public String getNewName ()
  {
    return newName;
  }
  public void setNewName (String newName)
  {
    this.newName = newName;
  }

  @Transient
  public String getNewAuth ()
  {
    return newAuth;
  }
  public void setNewAuth (String newAuth)
  {
    this.newAuth = newAuth;
  }

  @Transient
  public String getNewShort ()
  {
    return newShort;
  }
  public void setNewShort (String newShort)
  {
    this.newShort = newShort;
  }

  @Transient
  public int getSelectedRoundRegister ()
  {
    return selectedRoundRegister;
  }
  public void setSelectedRoundRegister (int selectedTourneyRegister)
  {
    this.selectedRoundRegister = selectedTourneyRegister;
  }

  @Transient
  public int getSelectedRoundUnregister ()
  {
    return selectedRoundUnregister;
  }
  public void setSelectedRoundUnregister (int selectedRoundUnregister)
  {
    this.selectedRoundUnregister = selectedRoundUnregister;
  }
  //</editor-fold>
}
