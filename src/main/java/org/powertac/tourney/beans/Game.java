package org.powertac.tourney.beans;

import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


@ManagedBean
public class Game
{
  private String competitionName = "";
  private Date startTime;
  private int competitionId = -1;
  private int tourneyId = 0;
  private int gameId = 0;
  private int machineId;
  private String status = "pending";
  private boolean hasBootstrap = false;
  private String brokers = "";

  private String gameName = "";
  private String location = "";
  private String jmsUrl = "";
  private String visualizerUrl = "";

  private HashMap<String, String> brokersToLogin = null;

  private String[] brokersLoggedIn = null;
  private int maxBrokers = 1;

  public static final String key = "game";

  public Game ()
  {
    brokersToLogin = new HashMap<String, String>();
  }

  public Game (ResultSet rs)
  {
    try {
      setStatus(rs.getString("status"));
      setMaxBrokers(rs.getInt("maxBrokers"));
      setStartTime(Utils.dateFormatUTCmilli(rs.getString("startTime")));
      setBrokers(rs.getString("brokers"));
      setTourneyId(rs.getInt("tourneyId"));
      setMachineId(rs.getInt("machineId"));
      setHasBootstrap(rs.getBoolean("hasBootstrap"));
      setGameName(rs.getString("gameName"));
      setGameId(rs.getInt("gameId"));
      setJmsUrl(rs.getString("jmsUrl"));
      setVisualizerUrl(rs.getString("visualizerUrl"));
      setLocation(rs.getString("location"));
    }
    catch (Exception e) {
      System.out.println("[ERROR] Error creating game from result set");
      e.printStackTrace();
    }
  }

  public int getNumBrokersRegistered ()
  {
    Database db = new Database();
    int result = 0;

    try {
      db.startTrans();
      result = db.getBrokersInGame(gameId).size();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
    return result;
  }

  public void addBroker (int brokerId)
  {
    Database db = new Database();
    Broker b = new Broker("new");
    try {
      db.startTrans();
      b = db.getBroker(brokerId);
      db.addBrokerToGame(gameId, b);
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    brokers += b.getBrokerName() + ", ";
  }

  public boolean isBrokerRegistered (String authToken)
  {
    System.out.println("Broker token: " + authToken);
    Database db = new Database();
    boolean ingame = false;
    try {
      db.startTrans();
      List<Broker> allBrokers = db.getBrokersInGame(gameId);
      for (Broker b: allBrokers) {
        if (b.getBrokerAuthToken().equalsIgnoreCase(authToken)) {
          ingame = true;
          break;
        }
      }
      db.commitTrans();

    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return ingame;
  }

  public String toUTCStartTime ()
  {
    return Utils.dateFormatUTC(startTime);
  }


  //<editor-fold desc="Setter and getters">
  public Date getStartTime ()
  {
    return startTime;
  }

  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  public static String getKey ()
  {
    return key;
  }

  public String[] getBrokersLoggedIn ()
  {
    return brokersLoggedIn;
  }

  public HashMap<String, String> getBrokersToLogin ()
  {
    return brokersToLogin;
  }

  public void setBrokersToLogin (HashMap<String, String> brokersToLogin)
  {
    this.brokersToLogin = brokersToLogin;
  }

  public String getCompetitionName ()
  {
    return competitionName;
  }

  public void setCompetitionName (String competitionName)
  {
    this.competitionName = competitionName;
  }

  public int getCompetitionId ()
  {
    return competitionId;
  }

  public void setCompetitionId (int competitionId)
  {
    this.competitionId = competitionId;
  }

  public String getStatus ()
  {
    return status;
  }

  public void setStatus (String status)
  {
    this.status = status;
  }

  public String getJmsUrl ()
  {
    return jmsUrl;
  }

  public void setJmsUrl (String jmsUrl)
  {
    this.jmsUrl = jmsUrl;
  }

  public int getMaxBrokers ()
  {
    return maxBrokers;
  }

  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  public int getGameId ()
  {
    return gameId;
  }

  public void setGameId (int gameId)
  {
    this.gameId = gameId;
  }

  public boolean getHasBootstrap()
  {
    return hasBootstrap;
  }

  public void setHasBootstrap (boolean hasBootstrap)
  {
    this.hasBootstrap = hasBootstrap;
  }

  public String getGameName ()
  {
    return gameName;
  }

  public void setGameName (String gameName)
  {
    this.gameName = gameName;
  }

  public String getVisualizerUrl ()
  {
    return visualizerUrl;
  }

  public void setVisualizerUrl (String visualizerUrl)
  {
    this.visualizerUrl = visualizerUrl;
  }

  public String getLocation ()
  {
    return location;
  }

  public void setLocation (String location)
  {
    this.location = location;
  }

  public int getTourneyId ()
  {
    return tourneyId;
  }

  public void setTourneyId (int tourneyId)
  {
    this.tourneyId = tourneyId;
  }

  public String getBrokers ()
  {
    return brokers;
  }

  public void setBrokers (String brokers)
  {
    this.brokers = brokers;
  }

  public int getMachineId ()
  {
    return machineId;
  }

  public void setMachineId (int machineId)
  {
    this.machineId = machineId;
  }
  //</editor-fold>
}
