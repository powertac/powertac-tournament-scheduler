package org.powertac.tournament.beans;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "round_brokers", uniqueConstraints =
    @UniqueConstraint(columnNames = {"roundId", "brokerId"}))
public class RoundBroker
{
  private int roundBrokerId;
  private Round round;
  private Broker broker;

  public RoundBroker ()
  {
  }

  @ManyToOne
  @JoinColumn(name = "roundId")
  public Round getRound ()
  {
    return round;
  }

  public void setRound (Round round)
  {
    this.round = round;
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
  @Column(name = "roundBrokerId", unique = true, nullable = false)
  public int getRoundBrokerId ()
  {
    return roundBrokerId;
  }

  public void setRoundBrokerId (int roundBrokerId)
  {
    this.roundBrokerId = roundBrokerId;
  }
}
