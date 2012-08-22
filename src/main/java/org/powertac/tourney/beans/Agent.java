/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 8/6/12
 * Time: 10:29 AM
 */

package org.powertac.tourney.beans;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An Agent is an instance of a broker, competing in one game, and one game only
 *
 */

@Entity
@Table(name = "agents", catalog = "tourney", uniqueConstraints = {
    @UniqueConstraint(columnNames = "agentId")})
public class Agent {
  public static enum STATE {
    pending, in_progress, complete
  }

  private int agentId;
  private int gameId;
  private int brokerId;
  private String brokerQueue;
  private String status;
  private double balance;

  public Agent ()
  {
  }

  public Agent (ResultSet rs) throws SQLException
  {
    setAgentId(rs.getInt("agentId"));
    setGameId(rs.getInt("gameId"));
    setBrokerId(rs.getInt("brokerId"));
    setBrokerQueue(rs.getString("brokerQueue"));
    setStatus(rs.getString("status"));
    setBalance(rs.getDouble("balance"));
  }

  //<editor-fold desc="Getters and Setters">
  public int getAgentId() {
    return agentId;
  }
  public void setAgentId(int agentId) {
    this.agentId = agentId;
  }

  public int getGameId() {
    return gameId;
  }
  public void setGameId(int gameId) {
    this.gameId = gameId;
  }

  public int getBrokerId() {
    return brokerId;
  }
  public void setBrokerId(int brokerId) {
    this.brokerId = brokerId;
  }

  public String getBrokerQueue() {
    return brokerQueue;
  }
  public void setBrokerQueue(String brokerQueue) {
    this.brokerQueue = brokerQueue;
  }

  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  public double getBalance() {
    return balance;
  }
  public void setBalance(double balance) {
    this.balance = balance;
  }
  //</editor-fold>
}