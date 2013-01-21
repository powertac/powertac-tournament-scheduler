package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private Map<Integer, Tournament> tournamentMap = new HashMap<Integer, Tournament>();

  // For edit mode, web interface
  private boolean edit;
  private String newName;
  private String newAuth;
  private String newShort;
  // For registration, web interface
  private int selectedTourneyRegister;
  private int selectedTourneyUnregister;

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

  public boolean update ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.update(this);
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

  public boolean delete ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
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
      for (Tournament tournament : broker.getTournamentMap().values()) {
        Registration registration = (Registration) session
            .createCriteria(Registration.class)
            .add(Restrictions.eq("tournament", tournament))
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

  public boolean register (int tourneyId)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament =
          (Tournament) session.get(Tournament.class, tourneyId);
      Registration registration = new Registration();
      registration.setBroker(this);
      registration.setTournament(tournament);
      session.save(registration);
      log.info(String.format("Registering broker: %s with tournament: %s",
          brokerId, tournament.getTournamentId()));

      // Only for single game, the scheduler handles multigame tourneys
      if (tournament.isSingle()) {
        for (Game game : tournament.getGameMap().values()) {
          Agent agent = Agent.createAgent(this, game);
          session.save(agent);
          log.info(String.format("Registering broker: %s with game: %s",
              brokerId, game.getGameId()));
        }
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

  public boolean unregister (int tourneyId)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament =
          (Tournament) session.get(Tournament.class, tourneyId);

      // Can't unregister from games that already started (or should have)
      if (tournament.isStarted()) {
        transaction.rollback();
        return false;
      }

      Registration registration = (Registration) session
          .createCriteria(Registration.class)
          .add(Restrictions.eq("tournament", tournament))
          .add(Restrictions.eq("broker", this)).uniqueResult();
      session.delete(registration);

      List<Integer> deleteAgents = new ArrayList<Integer>();
      for (Agent agent : agentMap.values()) {
        if (agent.getGame().getTournament().getTournamentId() == tourneyId) {
          deleteAgents.add(agent.getAgentId());
        }
      }

      for (Integer agentId : deleteAgents) {
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

  // Check if not more than maxBrokers are running (only multi_game tourneys)
  public boolean agentsAvailable ()
  {
    Scheduler scheduler = Scheduler.getScheduler();
    Tournament runningTournament = scheduler.getRunningTournament();

    // When no tournament loaded (thus only SINGLE_GAMES will be run),
    // assume enough agents ready
    if (runningTournament == null) {
      return true;
    }

    int count = 0;
    for (Agent agent : agentMap.values()) {
      if (agent.getGame().isRunning()) {
        count++;
      }
    }

    return count < runningTournament.getMaxAgents();
  }

  @Transient
  public String getRegisteredString ()
  {
    String result = "";

    for (Tournament tournament : tournamentMap.values()) {
      if (!tournament.isComplete()) {
        result += tournament.getTournamentName() + ", ";
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
  public List<Tournament> getAvailableTournaments (Boolean checkClosed)
  {
    List<Tournament> registrableTournaments = new ArrayList<Tournament>();

    TournamentProperties properties = TournamentProperties.getProperties();
    long loginDeadline =
        Integer.parseInt(properties.getProperty("loginDeadline", "3600000"));
    long nowStamp = Utils.offsetDate().getTime();

    Outer:
    for (Tournament tournament : Tournament.getNotCompleteTournamentList()) {
      // Check if maxNofBrokers reached
      if (tournament.getBrokerMap().size() >= tournament.getMaxBrokers()) {
        continue;
      }
      // Check if after deadline
      long diff = tournament.getStartTime().getTime() - nowStamp;
      if (diff < loginDeadline) {
        continue;
      }
      // Check if already registered
      for (Tournament t : tournamentMap.values()) {
        // Check if already registered
        if (t.getTournamentId() == tournament.getTournamentId()) {
          continue Outer;
        }
      }
      // Check if not closed
      if (checkClosed && tournament.isClosed()) {
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
      @JoinColumn(name = "tourneyId", referencedColumnName = "tourneyId")
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
  public int getSelectedTourneyRegister ()
  {
    return selectedTourneyRegister;
  }

  public void setSelectedTourneyRegister (int selectedTourneyRegister)
  {
    this.selectedTourneyRegister = selectedTourneyRegister;
  }

  @Transient
  public int getSelectedTourneyUnregister ()
  {
    return selectedTourneyUnregister;
  }

  public void setSelectedTourneyUnregister (int selectedTourneyUnregister)
  {
    this.selectedTourneyUnregister = selectedTourneyUnregister;
  }
  //</editor-fold>
}
