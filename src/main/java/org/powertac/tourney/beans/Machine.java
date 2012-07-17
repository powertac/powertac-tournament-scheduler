package org.powertac.tourney.beans;

import javax.persistence.*;
import java.sql.ResultSet;

import static javax.persistence.GenerationType.IDENTITY;
import static org.powertac.tourney.services.Utils.log;

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

  public static enum STATE { idle, running }

  public Machine ()
  {
  }

  public Machine (ResultSet rsMachines)
  {
    try {
      setMachineId(rsMachines.getInt("machineId"));
      setStatus(rsMachines.getString("status"));
      setUrl(rsMachines.getString("machineUrl"));
      setVizUrl(rsMachines.getString("visualizerUrl"));
      setName(rsMachines.getString("machineName"));
      setAvailable(rsMachines.getBoolean("available"));
    }
    catch (Exception e) {
      log("[ERROR] Error creating machine from result set");
      e.printStackTrace();
    }
  }

  public boolean stateEquals(STATE state)
  {
    return this.status.equals(state.toString());
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
    if (status.equals(STATE.idle.toString())) {
      inProgress = false;
    }
    else if (status.equals(STATE.running.toString())) {
      inProgress = true;
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

}
