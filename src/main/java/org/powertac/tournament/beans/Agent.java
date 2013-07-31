package org.powertac.tournament.beans;

import org.powertac.tournament.services.Utils;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;


/**
 * An Agent is an instance of a broker, competing in one game, and one game only
 */

@Entity
@Table(name = "agents")
public class Agent
{
  private int agentId;
  private Game game;
  private int gameId;
  private Broker broker;
  private int brokerId;
  private String brokerQueue;
  private STATE state;
  private double balance;

  private static enum STATE
  {
    pending, in_progress, complete
  }

  public Agent ()
  {
  }

  public static Agent createAgent (Broker broker, Game game)
  {
    Agent agent = new Agent();
    agent.setGame(game);
    agent.setBroker(broker);
    agent.setBrokerQueue(Utils.createQueueName());
    agent.setState(Agent.STATE.pending);
    agent.setBalance(0);

    return agent;
  }

  //<editor-fold desc="State methods">
  @Transient
  public boolean isPending ()
  {
    return this.state.equals(STATE.pending);
  }

  @Transient
  public boolean isInProgress ()
  {
    return this.state.equals(STATE.in_progress);
  }

  public void setStatePending ()
  {
    setState(STATE.pending);
  }

  public void setStateInProgress ()
  {
    setState(STATE.in_progress);
  }

  public void setStateComplete ()
  {
    setState(STATE.complete);
  }
  //</editor-fold>

  //<editor-fold desc="Getters and Setters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "agentId", unique = true, nullable = false)
  public int getAgentId ()
  {
    return agentId;
  }
  public void setAgentId (int agentId)
  {
    this.agentId = agentId;
  }

  @ManyToOne
  @JoinColumn(name = "gameId")
  public Game getGame ()
  {
    return game;
  }
  public void setGame (Game game)
  {
    this.game = game;
  }

  @Column(name = "gameId", updatable = false, insertable = false)
  public int getGameId ()
  {
    return gameId;
  }
  public void setGameId (int gameId)
  {
    this.gameId = gameId;
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

  @Column(name = "brokerId", updatable = false, insertable = false)
  public int getBrokerId ()
  {
    return brokerId;
  }
  public void setBrokerId (int brokerId)
  {
    this.brokerId = brokerId;
  }

  @Column(name = "brokerQueue")
  public String getBrokerQueue ()
  {
    return brokerQueue;
  }
  public void setBrokerQueue (String brokerQueue)
  {
    this.brokerQueue = brokerQueue;
  }

  @Column(name = "state", nullable = false)
  @Enumerated(EnumType.STRING)
  public STATE getState ()
  {
    return state;
  }
  public void setState (STATE state)
  {
    this.state = state;
  }

  @Column(name = "balance", nullable = false)
  public double getBalance ()
  {
    return balance;
  }
  public void setBalance (double balance)
  {
    this.balance = balance;
  }
  //</editor-fold>
}