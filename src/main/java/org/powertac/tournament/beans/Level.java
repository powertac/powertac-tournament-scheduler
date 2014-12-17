package org.powertac.tournament.beans;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "levels")
public class Level
{
  private int levelId;
  private String levelName;
  private Tournament tournament;
  private int levelNr;
  private int nofRounds;
  private int nofWinners;
  private Date startTime;

  private Map<Integer, Round> roundMap = new HashMap<Integer, Round>();

  public Level ()
  {
  }

  @Transient
  public int getMaxBrokers ()
  {
    int total = 0;
    for (Round round : roundMap.values()) {
      total += round.getMaxBrokers();
    }
    return total;
  }

  @Transient
  public int getNofBrokers ()
  {
    int nofBrokers = 0;
    for (Round round : roundMap.values()) {
      nofBrokers += round.getNofBrokers();
    }
    return nofBrokers;
  }

  @Transient
  public boolean isStarted ()
  {
    for (Round round: roundMap.values()) {
      if (round.isStarted()) {
        return true;
      }
    }
    return false;
  }

  @OneToMany
  @JoinColumn(name = "levelId")
  @MapKey(name = "roundId")
  public Map<Integer, Round> getRoundMap ()
  {
    return roundMap;
  }
  public void setRoundMap (Map<Integer, Round> roundMap)
  {
    this.roundMap = roundMap;
  }

  @SuppressWarnings("unchecked")
  public static List<Level> getNotCompleteLevelList ()
  {
    List<Level> levels = new ArrayList<Level>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      levels = (List<Level>) session
          .createQuery(Constants.HQL.GET_LEVELS_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return levels;
  }

  public String startTimeUTC ()
  {
    return Utils.dateToStringMedium(startTime);
  }

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "levelId", unique = true, nullable = false)
  public int getLevelId ()
  {
    return levelId;
  }
  public void setLevelId (int levelId)
  {
    this.levelId = levelId;
  }

  @Column(name = "levelName")
  public String getLevelName ()
  {
    return levelName;
  }
  public void setLevelName (String levelName)
  {
    this.levelName = levelName;
  }

  @ManyToOne
  @JoinColumn(name = "tournamentId")
  public Tournament getTournament ()
  {
    return tournament;
  }
  public void setTournament (Tournament tournament)
  {
    this.tournament = tournament;
  }

  @Column(name = "levelNr")
  public int getLevelNr ()
  {
    return levelNr;
  }
  public void setLevelNr (int levelNr)
  {
    this.levelNr = levelNr;
  }

  @Column(name = "nofRounds")
  public int getNofRounds ()
  {
    return nofRounds;
  }
  public void setNofRounds (int nofRounds)
  {
    this.nofRounds = nofRounds;
  }

  @Column(name = "nofWinners")
  public int getNofWinners ()
  {
    return nofWinners;
  }
  public void setNofWinners (int nofWinners)
  {
    this.nofWinners = nofWinners;
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
  //</editor-fold>
}
