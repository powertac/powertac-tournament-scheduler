package org.powertac.tourney.beans;

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.ResultSet;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

//Create hibernate mapping with annotations
@Entity
@Table(name = "machines", catalog = "tourney", uniqueConstraints = {
            @UniqueConstraint(columnNames = "machineId")})
public class Machine
{
  private String name;
  private String url;
  private boolean available;
  private boolean inProgress;
  private String status;
  private int machineId;
  private int gameId;
  private String vizUrl;
  private String vizQueue;

  public Machine ()
  {
  }

  public Machine (ResultSet rsMachines)
  {
    try {
      this.setMachineId(rsMachines.getInt("machineId"));
      this.setStatus(rsMachines.getString("status"));
      this.setUrl(rsMachines.getString("machineUrl"));
      this.setVizUrl(rsMachines.getString("visualizerUrl"));
      this.setVizQueue(rsMachines.getString("visualizerQueue"));
      this.setName(rsMachines.getString("machineName"));
      this.setAvailable(rsMachines.getBoolean("available"));
    }
    catch (Exception e) {
      System.out.println("[ERROR] Error creating tournament from result set");
    }
  }

  @Column(name = "machineName", unique = false, nullable = false)
  public String getName ()
  {
    return name;
  }

  public void setName (String name)
  {
    this.name = name;
  }
  
  @Column(name = "machineUrl", unique = false, nullable = false)
  public String getUrl ()
  {
    return url;
  }

  public void setUrl (String url)
  {
    this.url = url;
  }

  @Transient
  public boolean isInProgress ()
  {
    return inProgress;
  }

  public void setInProgress (boolean inProgress)
  {
    this.inProgress = inProgress;
  }

  @Column(name = "gameId", unique = false, nullable = false)
  public int getGameId ()
  {
    return gameId;
  }

  public void setGameId (int gameId)
  {
    this.gameId = gameId;
  }

  
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "machineId", unique = true, nullable = false)
  public int getMachineId ()
  {
    return machineId;
  }

  public void setMachineId (int machineId)
  {
    this.machineId = machineId;
  }

  @Column(name = "status", unique = false, nullable = false)
  public String getStatus ()
  {
    return status;
  }

  public void setStatus (String status)
  {
    this.status = status;
    if (status.equalsIgnoreCase("idle")) {
      this.inProgress = false;
    }
    else if (status.equalsIgnoreCase("running")) {
      this.inProgress = true;
    }
  }

  
  @Column(name = "available", unique = false, nullable = false)
  public boolean isAvailable ()
  {
    return available;
  }

  public void setAvailable (boolean available)
  {
    this.available = available;
  }

  @Column(name = "visualizerUrl", unique = false, nullable = false)
  public String getVizUrl ()
  {
    return vizUrl;
  }

  public void setVizUrl (String vizUrl)
  {
    this.vizUrl = vizUrl;
  }

  @Column(name = "visualizerQueue", unique = false, nullable = false)
  public String getVizQueue ()
  {
    return vizQueue;
  }

  public void setVizQueue (String vizQueue)
  {
    this.vizQueue = vizQueue;
  }
}
