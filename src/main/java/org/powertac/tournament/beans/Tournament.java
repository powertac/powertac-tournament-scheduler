package org.powertac.tournament.beans;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.states.TournamentState;

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
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "tournaments")
public class Tournament
{
  private int tournamentId;
  private String tournamentName;
  private TournamentState state;
  private int pomId;
  private int maxAgents;

  private Map<Integer, Level> levelMap = new HashMap<>();
  private Map<Integer, Broker> brokerMap = new HashMap<>();

  public Tournament ()
  {
    state = TournamentState.open;
    maxAgents = 2; // default value for maximum number of concurrent agents
  }

  @Transient
  public Level getCurrentLevel ()
  {
    return levelMap.get(state.getCurrentLevelNr());
  }

  @Transient
  public Level getPreviousLevel ()
  {
    int currentLevelNr = state.getCurrentLevelNr();

    if (currentLevelNr == 0) {
      return null;
    }

    return levelMap.get(state.getCurrentLevelNr() - 1);
  }

  @Transient
  public Level getNextLevel ()
  {
    int currentLevelNr = state.getCurrentLevelNr();

    if (currentLevelNr == levelMap.size()) {
      return null;
    }

    return levelMap.get(state.getCurrentLevelNr() + 1);
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
    for (Round round : levelMap.get(0).getRoundMap().values()) {
      nofBrokers += round.getBrokerMap().size();
    }
    return nofBrokers;
  }

  @Transient
  public boolean isStarted ()
  {
    Level firstLevel = levelMap.get(0);

    for (Round round : firstLevel.getRoundMap().values()) {
      if (round.isStarted()) {
        return true;
      }
    }
    return false;
  }

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Tournament> getNotCompleteTournamentList ()
  {
    List<Tournament> tournaments = new ArrayList<>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournaments = (List<Tournament>) session
          .createQuery(Constants.HQL.GET_TOURNAMENT_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    }
    catch (Exception e) {
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
  public TournamentState getState ()
  {
    return state;
  }

  public void setState (TournamentState state)
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
  public int getMaxAgents ()
  {
    return maxAgents;
  }

  public void setMaxAgents (int maxAgents)
  {
    this.maxAgents = maxAgents;
  }
  //</editor-fold>
}