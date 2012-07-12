package org.powertac.tourney.beans;

import org.hibernate.Query;
import org.hibernate.Session;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;



// Create hibernate mapping with annotations
@Entity
@Table(name = "games", catalog = "tourney", uniqueConstraints = {
		@UniqueConstraint(columnNames = "gameId")})
public class Game implements Serializable
{
  
  private Date startTime;
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

  @Transient
  private HashMap<String, String> brokersToLogin = null;

  @Transient
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
  
  
  public static List<Game> getGames(){
	  Session session = HibernateUtil.getSessionFactory().openSession();
	  
	  Query q = session.createQuery("from Games");
	  return q.list();
	  
  }

  @Transient
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

  public Broker getBrokerRegistration (String authToken)
  {
    System.out.println("Broker token: " + authToken);
    Database db = new Database();
    Broker result = null;
    try {
      db.startTrans();
      List<Broker> allBrokers = db.getBrokersInGame(gameId);
      for (Broker b: allBrokers) {
        if (b.getBrokerAuthToken().equalsIgnoreCase(authToken)) {
          result = b;
          break;
        }
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
    return result;
  }

  public String toUTCStartTime ()
  {
    return Utils.dateFormatUTC(startTime);
  }


  //<editor-fold desc="Setter and getters">
  @Temporal(TemporalType.DATE)
  @Column(name = "startTime", unique = false, nullable = false, length = 10)
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

  @Transient
  public String[] getBrokersLoggedIn ()
  {
    return brokersLoggedIn;
  }

  @Transient
  public HashMap<String, String> getBrokersToLogin ()
  {
    return brokersToLogin;
  }

  public void setBrokersToLogin (HashMap<String, String> brokersToLogin)
  {
    this.brokersToLogin = brokersToLogin;
  }


  @Column(name = "status", unique = false, nullable = false)
  public String getStatus ()
  {
    return status;
  }

  public void setStatus (String status)
  {
    this.status = status;
  }

  @Column(name = "jmsUrl", unique = false, nullable = true)
  public String getJmsUrl ()
  {
    return jmsUrl;
  }

  public void setJmsUrl (String jmsUrl)
  {
    this.jmsUrl = jmsUrl;
  }

  @Column(name = "maxBrokers", unique = false, nullable = true)
  public int getMaxBrokers ()
  {
    return maxBrokers;
  }

  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "gameId", unique = true, nullable = false)
  public int getGameId ()
  {
    return gameId;
  }

  public void setGameId (int gameId)
  {
    this.gameId = gameId;
  }

  @Column(name = "hasBootstrap", unique = false, nullable = false)
  public boolean getHasBootstrap()
  {
    return hasBootstrap;
  }

  public void setHasBootstrap (boolean hasBootstrap)
  {
    this.hasBootstrap = hasBootstrap;
  }

  @Column(name = "gameName", unique = false, nullable = false)
  public String getGameName ()
  {
    return gameName;
  }

  public void setGameName (String gameName)
  {
    this.gameName = gameName;
  }

  @Column(name = "visualizerUrl", unique = false, nullable = false)
  public String getVisualizerUrl ()
  {
    return visualizerUrl;
  }

  public void setVisualizerUrl (String visualizerUrl)
  {
    this.visualizerUrl = visualizerUrl;
  }

  @Column(name = "location", unique = false, nullable = false)
  public String getLocation ()
  {
    return location;
  }

  public void setLocation (String location)
  {
    this.location = location;
  }
  
  @Column(name = "tourneyId", unique = false, nullable = false)
  public int getTourneyId ()
  {
    return tourneyId;
  }

  public void setTourneyId (int tourneyId)
  {
    this.tourneyId = tourneyId;
  }

  // Comma delimited list of brokers to be sent to the server command line
  @Column(name = "brokers", unique = false, nullable = false)
  public String getBrokers ()
  {
    return brokers;
  }

  public void setBrokers (String brokers)
  {
    this.brokers = brokers;
  }

  @Column(name = "machineId", unique = false, nullable = true)
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
