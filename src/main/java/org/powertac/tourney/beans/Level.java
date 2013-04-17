package org.powertac.tourney.beans;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "levels")
public class Level
{
  private int levelId;
  private String levelName;
  private int competitionId;
  private int levelNr;
  private int nofTournaments;
  private int nofWinners;
  private Date startTime;

  private Map<Integer, Tournament> tournamentMap = new HashMap<Integer, Tournament>();

  public Level ()
  {
  }

  @Transient
  public int getMaxBrokers ()
  {
    int total = 0;
    for (Tournament tournament: tournamentMap.values()) {
      total += tournament.getMaxBrokers();
    }
    return total;
  }

  @Transient
  public int getNofBrokers ()
  {
    int nofBrokers = 0;
    for (Tournament tournament: tournamentMap.values()) {
      nofBrokers += tournament.getBrokerMap().size();
    }
    return nofBrokers;
  }

  @OneToMany
  @JoinColumn(name = "levelId")
  @MapKey(name = "tournamentId")
  public Map<Integer, Tournament> getTournamentMap ()
  {
    return tournamentMap;
  }
  public void setTournamentMap (Map<Integer, Tournament> tournamentMap)
  {
    this.tournamentMap = tournamentMap;
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

  @Column(name = "competitionId")
  public int getCompetitionId ()
  {
    return competitionId;
  }
  public void setCompetitionId (int competitionId)
  {
    this.competitionId = competitionId;
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

  @Column(name = "nofTournaments")
  public int getNofTournaments ()
  {
    return nofTournaments;
  }
  public void setNofTournaments (int nofTournaments)
  {
    this.nofTournaments = nofTournaments;
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
