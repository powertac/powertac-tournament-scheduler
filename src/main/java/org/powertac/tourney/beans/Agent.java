/**
 * Created by IntelliJ IDEA.
 * User: Govert Buijs
 * Date: 8/6/12
 * Time: 10:29 AM
 */

package org.powertac.tourney.beans;

import org.powertac.tourney.services.Utils;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * An Agent is an instance of a broker, competing in one game, and one game only
 */

@Entity
@Table(name = "agents", catalog = "tourney", uniqueConstraints = {
    @UniqueConstraint(columnNames = "agentId")})
public class Agent {
  private int agentId;
  private Game game;
  private int gameId;
  private Broker broker;
  private int brokerId;
  private String brokerQueue;
  private String status;
  private double balance;

  public static enum STATE {
    pending, in_progress, complete
  }

  public Agent() {
  }

  public static Agent createAgent(Broker broker, Game game) {
    Agent agent = new Agent();
    agent.setGame(game);
    agent.setBroker(broker);
    agent.setBrokerQueue(Utils.createQueueName());
    agent.setStatus(Agent.STATE.pending.toString());
    agent.setBalance(0);

    return agent;
  }

  //<editor-fold desc="Getters and Setters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "agentId", unique = true, nullable = false)
  public int getAgentId() {
    return agentId;
  }

  public void setAgentId(int agentId) {
    this.agentId = agentId;
  }

  @ManyToOne
  @JoinColumn(name = "gameId")
  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
  }

  @Column(name = "gameId", updatable = false, insertable = false)
  public int getGameId() {
    return gameId;
  }

  public void setGameId(int gameId) {
    this.gameId = gameId;
  }

  @ManyToOne
  @JoinColumn(name = "brokerId")
  public Broker getBroker() {
    return broker;
  }

  public void setBroker(Broker broker) {
    this.broker = broker;
  }

  @Column(name = "brokerId", updatable = false, insertable = false)
  public int getBrokerId() {
    return brokerId;
  }

  public void setBrokerId(int brokerId) {
    this.brokerId = brokerId;
  }

  @Column(name = "brokerQueue")
  public String getBrokerQueue() {
    return brokerQueue;
  }

  public void setBrokerQueue(String brokerQueue) {
    this.brokerQueue = brokerQueue;
  }

  @Column(name = "status", nullable = false)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Column(name = "balance", nullable = false)
  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }
  //</editor-fold>
}