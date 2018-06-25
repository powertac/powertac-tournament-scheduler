package org.powertac.tournament.beans;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "tournament_brokers", uniqueConstraints =
@UniqueConstraint(columnNames = {"tournamentId", "brokerId"}))
public class TournamentBroker implements Serializable
{
  private int tournamentBrokerId;
  private Tournament tournament;
  private Broker broker;

  public TournamentBroker ()
  {
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

  @ManyToOne
  @JoinColumn(name = "brokerId")
  public Broker getBroker ()
  {
    return broker;
  }

  public void setBroker (Broker broker)
  {
    this.broker = broker;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "tournamentBrokerId", unique = true, nullable = false)
  public int getTournamentBrokerId ()
  {
    return tournamentBrokerId;
  }

  public void setTournamentBrokerId (int tournamentBrokerId)
  {
    this.tournamentBrokerId = tournamentBrokerId;
  }
}
