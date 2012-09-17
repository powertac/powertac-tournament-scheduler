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
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;

@ManagedBean
@Entity
@Table(name="brokers", catalog="tourney", uniqueConstraints={
    @UniqueConstraint(columnNames="brokerId")})
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
  private int selectedTourney;

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

  public boolean update ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.update(this);
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

  public boolean delete ()
  {
    // TODO Check if allowed to delete broker? Running games etc?

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Broker broker = (Broker) session
          .createQuery(Constants.HQL.GET_BROKER_BY_ID)
          .setInteger("brokerId", brokerId).uniqueResult();

      // Delete all agent belonging to this broker
      for (Agent agent: broker.agentMap.values()) {
        session.delete(agent);
        session.flush();
      }

      // Delete all registrations to this broker
      for (Tournament tournament: broker.getTournamentMap().values()) {
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
      if (tournament.typeEquals(Tournament.TYPE.SINGLE_GAME)) {
        for (Game game: tournament.getGameMap().values()) {
          Agent agent = new Agent();
          agent.setGame(game);
          agent.setBroker(this);
          agent.setBrokerQueue(Utils.createQueueName());
          agent.setStatus(Agent.STATE.pending.toString());
          agent.setBalance(-1);
          session.save(agent);
          log.info(String.format("Registering broker: %s with game: %s",
              brokerId, game.getGameId()));
        }
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

  // Check if not more than maxBrokers are running (only multi_game tourneys)
  public boolean agentsAvailable()
  {
    Scheduler scheduler = Scheduler.getScheduler();
    Tournament runningTournament = scheduler.getRunningTournament();

    // When no tournament loaded (thus only SINGLE_GAMES will be run),
    // assume enough agents ready
    if (runningTournament == null) {
      return true;
    }

    int count = 0;
    for (Agent agent: agentMap.values()) {
      if (agent.getGame().isRunning()) {
        count ++;
      }
    }

    return count < runningTournament.getMaxAgents();
  }

  @Transient
  public String getRegisteredString ()
  {
    String result = "";

    for (Tournament tournament: tournamentMap.values()) {
      if (tournament.stateEquals(Tournament.STATE.complete)) {
        continue;
      }
      result += tournament.getTournamentName() + ", ";
    }
    if (!result.isEmpty()) {
      result = result.substring(0, result.length()-2);
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
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return brokers;
  }

  @Transient
  public List<Tournament> getAvailableTournaments ()
  {
    List<Tournament> registrableTournaments = new ArrayList<Tournament>();

    TournamentProperties properties = TournamentProperties.getProperties();
    long loginDeadline =
        Integer.parseInt(properties.getProperty("loginDeadline", "3600000"));
    long nowStamp = Utils.offsetDate().getTime();

    Outer: for (Tournament tourney: Tournament.getNotCompleteTournamentList()) {
      // Check if maxNofBrokers reached
      if (tourney.getBrokerMap().size() >= tourney.getMaxBrokers()) {
        continue;
      }
      // Check if after deadline
      long diff = tourney.getStartTime().getTime() - nowStamp;
      if (diff < loginDeadline) {
        continue;
      }
      // Check if already registered
      for (Tournament t : tournamentMap.values()) {
        // Check if already registered
        if (t.getTournamentId() == tourney.getTournamentId()) {
          continue Outer;
        }
      }
      // Check if not closed
      if (tourney.isClosed()) {
        continue;
      }

      // No reason not to be able to register
      registrableTournaments.add(tourney);
    }

    return registrableTournaments;
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
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
    return broker;
  }

  @OneToMany
  @JoinColumn(name="brokerId")
  @MapKey(name="gameId")
  public Map<Integer, Agent> getAgentMap() {
    return agentMap;
  }
  public void setAgentMap(Map<Integer, Agent> agentMap) {
    this.agentMap = agentMap;
  }

  @ManyToMany
  @JoinTable(name="registrations",
      joinColumns=
      @JoinColumn(name="brokerId", referencedColumnName="brokerId"),
      inverseJoinColumns=
      @JoinColumn(name="tourneyId", referencedColumnName="tourneyId")
  )
  @MapKey(name="tournamentId")
  public Map<Integer, Tournament> getTournamentMap() {
    return tournamentMap;
  }
  public void setTournamentMap(Map<Integer, Tournament> tournamentMap) {
    this.tournamentMap = tournamentMap;
  }

  //<editor-fold desc="Bean Setters and Getters">
  @Id
  @GeneratedValue(strategy=IDENTITY)
  @Column(name="brokerId", unique=true, nullable=false)
  public Integer getBrokerId () {
    return brokerId;
  }
  public void setBrokerId (Integer brokerId) {
    this.brokerId = brokerId;
  }

  @ManyToOne
  @JoinColumn(name="userId")
  public User getUser() {
    return user;
  }
  public void setUser(User user) {
    this.user = user;
  }

  @Column(name="brokerName", nullable=false)
  public String getBrokerName (){
    return brokerName;
  }
  public void setBrokerName (String brokerName) {
    this.brokerName = brokerName;
  }

  @Column(name="brokerAuth", unique=true, nullable=false)
  public String getBrokerAuth () {
    return brokerAuth;
  }
  public void setBrokerAuth (String brokerAuth) {
    this.brokerAuth = brokerAuth;
  }

  @Column(name="brokerShort", nullable=false)
  public String getShortDescription () {
    return shortDescription;
  }
  public void setShortDescription (String shortDescription) {
    this.shortDescription = shortDescription;
  }
  //</editor-fold>

  //<editor-fold desc="Web Setters and Getters">
  @Transient
  public boolean isEdit () {
    return edit;
  }
  public void setEdit (boolean edit) {
    this.edit = edit;
  }

  @Transient
  public String getNewName () {
    return newName;
  }
  public void setNewName (String newName) {
    this.newName = newName;
  }

  @Transient
  public String getNewAuth () {
    return newAuth;
  }
  public void setNewAuth (String newAuth) {
    this.newAuth = newAuth;
  }

  @Transient
  public String getNewShort () {
    return newShort;
  }
  public void setNewShort (String newShort) {
    this.newShort = newShort;
  }

  @Transient
  public int getSelectedTourney () {
    return selectedTourney;
  }
  public void setSelectedTourney (int selectedTourney) {
    this.selectedTourney = selectedTourney;
  }
  //</editor-fold>
}
