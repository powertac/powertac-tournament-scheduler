package org.powertac.tourney.beans;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "rounds")
public class CompetitionRound
{
  private int roundId;
  private String roundName;
  private int competitionId;
  private int roundNr;
  private int nofTournaments;
  private int nofWinners;
  private Date startTime;

  private Map<Integer, Tournament> tournamentMap = new HashMap<Integer, Tournament>();

  public CompetitionRound ()
  {
  }

  @OneToMany
  @JoinColumn(name = "roundId")
  @MapKey(name = "tournamentId")
  public Map<Integer, Tournament> getTournamentMap ()
  {
    return tournamentMap;
  }
  public void setTournamentMap (Map<Integer, Tournament> tournamentMap)
  {
    this.tournamentMap = tournamentMap;
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

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "roundId", unique = true, nullable = false)
  public int getRoundId ()
  {
    return roundId;
  }
  public void setRoundId (int roundId)
  {
    this.roundId = roundId;
  }

  @Column(name = "roundName")
  public String getRoundName ()
  {
    return roundName;
  }
  public void setRoundName (String roundName)
  {
    this.roundName = roundName;
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

  @Column(name = "roundNr")
  public int getRoundNr ()
  {
    return roundNr;
  }
  public void setRoundNr (int roundNr)
  {
    this.roundNr = roundNr;
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
