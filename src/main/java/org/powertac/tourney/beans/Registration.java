package org.powertac.tourney.beans;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "registrations", uniqueConstraints =
    @UniqueConstraint(columnNames = {"tournamentId", "brokerId"}))
public class Registration
{
  private int registrationId;
  private Tournament tournament;
  private Broker broker;

  public Registration ()
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
  @Column(name = "registrationId", unique = true, nullable = false)
  public int getRegistrationId ()
  {
    return registrationId;
  }

  public void setRegistrationId (int registrationId)
  {
    this.registrationId = registrationId;
  }
}
