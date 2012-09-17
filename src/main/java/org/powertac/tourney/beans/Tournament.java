package org.powertac.tourney.beans;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name="tournaments", catalog="tourney", uniqueConstraints = {
            @UniqueConstraint(columnNames="tourneyId")})
public class Tournament
{
  private int tourneyId;
  private String tournamentName;
  private Date startTime;
  private Date dateFrom;
  private Date dateTo;
  private int maxBrokers;
  private int maxAgents;
  private String status;
  private int size1;
  private int size2;
  private int size3;
  private String type;
  private int pomId;
  private String locations;
  private boolean closed;

  // ALTER TABLE  `tournaments` ADD  `closed` BOOLEAN NOT NULL

  private Map<Integer, Game> gameMap = new HashMap<Integer, Game>();
  private Map<Integer, Broker> brokerMap = new HashMap<Integer, Broker>();

  public static enum STATE {
    pending, in_progress, complete
  }

  public static enum TYPE {
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
      Tournament tournament = (Tournament)session
          .createQuery(Constants.HQL.GET_TOURNAMENT_BY_ID)
          .setInteger("tournamentId", tourneyId).uniqueResult();

      // Disallow removal when games booting or running
      for (Game game: tournament.gameMap.values()) {
        if (game.isBooting() || game.isRunning()) {
          transaction.rollback();
          return String.format("Game %s can not be removed, state = %s",
              game.getGameName(), game.getStatus());
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
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Error deleting tournament";
    }
    finally {
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
      if (!game.stateEquals(Game.STATE.game_complete)) {
        allDone = false;
      }
    }

    if (allDone) {
      status = STATE.complete.toString();

      Scheduler scheduler = Scheduler.getScheduler();
      if (scheduler.getRunningTournament() != null &&
          scheduler.getRunningTournament().getTournamentId() == tourneyId) {
        scheduler.unloadTournament();
      }
    }
  }

  @Transient
  public boolean isStarted ()
  {
    return startTime.before(Utils.offsetDate());
  }

  public boolean typeEquals(TYPE type)
  {
    return this.type.equals(type.toString());
  }
  public boolean stateEquals(STATE state)
  {
    return this.status.equals(state.toString());
  }

  public String startTimeUTC ()
  {
    return Utils.dateFormat(startTime);
  }
  public String dateFromUTC()
  {
    return Utils.dateFormat(dateFrom);
  }
  public String dateToUTC()
  {
    return Utils.dateFormat(dateTo);
  }

  //<editor-fold desc="Collections">
  @OneToMany
  @JoinColumn(name="tourneyId")
  @MapKey(name="gameId")
  public Map<Integer, Game> getGameMap() {
    return gameMap;
  }
  public void setGameMap(Map<Integer, Game> gameMap) {
    this.gameMap = gameMap;
  }

  @ManyToMany
  @JoinTable(name="registrations",
      joinColumns=
      @JoinColumn(name="tourneyId", referencedColumnName="tourneyId"),
      inverseJoinColumns=
      @JoinColumn(name="brokerId", referencedColumnName="brokerId")
  )
  @MapKey(name="brokerId")
  public Map<Integer, Broker> getBrokerMap() {
    return brokerMap;
  }
  public void setBrokerMap(Map<Integer, Broker> brokerMap) {
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
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return tournaments;
  }
  //</editor-fold>

  //<editor-fold desc="Getters and setters">
  @Id
  @GeneratedValue(strategy=IDENTITY)
  @Column(name="tourneyId", unique=true, nullable=false)
  public int getTournamentId ()
  {
    return tourneyId;
  }
  public void setTournamentId (int tourneyId)
  {
    this.tourneyId = tourneyId;
  }

  @Column(name="tourneyName", nullable=false)
  public String getTournamentName ()
  {
    return tournamentName;
  }
  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="startTime", nullable=false, length=10)
  public Date getStartTime ()
  {
    return startTime;
  }
  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="dateFrom", nullable=false)
  public Date getDateFrom() {
    return dateFrom;
  }
  public void setDateFrom(Date dateFrom) {
    this.dateFrom = dateFrom;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="dateTo", nullable=false)
  public Date getDateTo() {
    return dateTo;
  }
  public void setDateTo(Date dateTo) {
    this.dateTo = dateTo;
  }

  @Column(name="maxBrokers", nullable=false)
  public int getMaxBrokers ()
  {
    return maxBrokers;
  }
  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  @Column(name="maxAgents", nullable=false)
  public int getMaxAgents()
  {
    return maxAgents;
  }
  public void setMaxAgents(int maxAgents)
  {
    this.maxAgents = maxAgents;
  }

  @Column(name="status", nullable=false)
  public String getStatus ()
  {
    return status;
  }
  public void setStatus (String status)
  {
    this.status = status;
  }

  @Column(name="gameSize1", nullable=false)
  public int getSize1 ()
  {
    return size1;
  }
  public void setSize1 (int size1)
  {
    this.size1 = size1;
  }

  @Column(name="gameSize2", nullable=false)
  public int getSize2 ()
  {
    return size2;
  }
  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  @Column(name="gameSize3", nullable=false)
  public int getSize3 ()
  {
    return size3;
  }
  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  @Column(name="type", nullable=false)
  public String getType ()
  {
    return type;
  }
  public void setType (String type)
  {
    this.type = type;
  }

  @Column(name="pomId", nullable=false)
  public int getPomId ()
  {
    return pomId;
  }
  public void setPomId (int pomId)
  {
    this.pomId = pomId;
  }

  @Column(name="locations", nullable=false)
  public String getLocations ()
  {
    return locations;
  }
  public void setLocations (String locations)
  {
    this.locations = locations;
  }

  @Column(name="closed", nullable=false)
  public boolean isClosed() {
    return closed;
  }
  public void setClosed(boolean closed) {
    this.closed = closed;
  }
  //</editor-fold>
}
