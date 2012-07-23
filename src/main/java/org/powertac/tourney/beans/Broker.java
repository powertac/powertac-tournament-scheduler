package org.powertac.tourney.beans;

import org.apache.commons.codec.digest.DigestUtils;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.TournamentProperties;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static javax.persistence.GenerationType.IDENTITY;

@ManagedBean
@Entity
@Table(name = "brokers", catalog = "tourney", uniqueConstraints = {
            @UniqueConstraint(columnNames = "brokerId")})
public class Broker
{
  private static final String key = "broker";
  private static int maxBrokerId = 0;

  // For edit mode
  private boolean edit = false;
  private String newName;
  private String newAuth;
  private String newShort;

  // For registration
  private String selectedTourney;

  private String brokerName;
  private int brokerId = 0;
  private int userId = 0;
  private String brokerAuthToken;
  private String brokerQueueName; // per-game value
  private String shortDescription;
  private int numberInGame = 0;
  private boolean brokerInGame = false;

  public Broker (ResultSet rs) throws SQLException
  {
    setBrokerId(rs.getInt("brokerId"));
    setUserId(rs.getInt("userId"));
    setBrokerName(rs.getString("brokerName"));
    setBrokerAuthToken(rs.getString("brokerAuth"));
    setShortDescription(rs.getString("brokerShort"));
    setNumberInGame(rs.getInt("numberInGame"));
  }

  public Broker (String brokerName)
  {
    this(brokerName, "");
  }

  public Broker (String brokerName, String shortDescription)
  {
    this.brokerName = brokerName;
    this.shortDescription = shortDescription;
    brokerId = maxBrokerId;
    maxBrokerId++;

    // Generate MD5 hash
    brokerAuthToken = DigestUtils.md5Hex(brokerName + brokerId +
        (new Date()).toString() + Math.random());
  }

  public String getUserName()
  {
    String userName = "";

    Database db = new Database();
    try {
      db.startTrans();
      userName = db.getUserName(userId);
      db.commitTrans();
    }
    catch (SQLException e) {
      e.printStackTrace();
      db.abortTrans();
    }

    return userName;
  }

  public List<Tournament> getAvailableTournaments ()
  {
    Vector<Tournament> availableTourneys = new Vector<Tournament>();
    List<Tournament> allTournaments = Tournament.getTournamentList();
    TournamentProperties properties = TournamentProperties.getProperties();
    long loginDeadline =
        Integer.parseInt(properties.getProperty("loginDeadline", "3600000"));
    long nowStamp = new Date().getTime();

    Database db = new Database();
    try {
      db.startTrans();
      for (Tournament t: allTournaments) {
        long startStamp = t.getStartTime().getTime();

        if (!db.isRegistered(t.getTournamentId(), brokerId)
            && t.getNumberRegistered() < t.getMaxBrokers()
            && (startStamp - nowStamp) > loginDeadline) {
          availableTourneys.add(t);
        }
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return availableTourneys;
  }

  public List<Tournament> getRegisteredTournaments ()
  {
    List<Tournament> tournaments = new ArrayList<Tournament>();

    Database db = new Database();

    try {
      db.startTrans();
      tournaments = db.getTournamentsByBrokerId(brokerId);
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return tournaments;
  }

  public String getRegisteredString ()
  {
    String result = "";
    for (Tournament t: getRegisteredTournaments()) {
      result += t.getTournamentName() + ", ";
    }
    if (!result.isEmpty()) {
      result = result.substring(0, result.length()-2);
    }

    return result;
  }

  public static List<Broker> getBrokerList ()
  {
    List<Broker> brokers = new ArrayList<Broker>();

    Database db = new Database();
    try {
      db.startTrans();
      brokers = db.getBrokers();
      db.commitTrans();
    }
      catch (SQLException e) {
      e.printStackTrace();
      db.abortTrans();
    }

    return brokers;
  }

  //<editor-fold desc="Setters and Getters">
  @Column(name = "userId", unique = false, nullable = false)
  public int getUserId ()
  {
    return userId;
  }
  public void setUserId (int userId)
  {
    this.userId = userId;
  }

  @Column(name = "brokerName", unique = false, nullable = false)
  public String getBrokerName ()
  {
    return brokerName;
  }
  public void setBrokerName (String brokerName)
  {
    this.brokerName = brokerName;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "brokerId", unique = true, nullable = false)
  public int getBrokerId ()
  {
    return brokerId;
  }
  public void setBrokerId (int brokerId)
  {
    this.brokerId = brokerId;
  }

  @Column(name = "brokerAuth", unique = true, nullable = false)
  public String getBrokerAuthToken ()
  {
    return brokerAuthToken;
  }
  public void setBrokerAuthToken (String brokerAuthToken)
  {
    this.brokerAuthToken = brokerAuthToken;
  }
  
  @Transient
  public String getQueueName ()
  {
    return brokerQueueName;
  }
  public void setQueueName (String queue)
  {
    brokerQueueName = queue;
  }
  
  @Transient
  public boolean getBrokerInGame ()
  {
    return brokerInGame;
  }
  public void setBrokerInGame (boolean value)
  {
    brokerInGame = value;
  }

  @Column(name = "brokerShort", unique = false, nullable = false)
  public String getShortDescription ()
  {
    return shortDescription;
  }
  public void setShortDescription (String shortDescription)
  {
    if (shortDescription != null && shortDescription.length() >= 200) {
      this.shortDescription = shortDescription.substring(0, 199);
    }
    else {

      this.shortDescription = shortDescription;
    }
  }

  @Transient
  public boolean isEdit ()
  {
    return edit;
  }
  public void setEdit (boolean edit)
  {
    this.edit = edit;
  }

  @Transient
  public String getNewName ()
  {
    return newName;
  }
  public void setNewName (String newName)
  {
    this.newName = newName;
  }

  @Transient
  public String getNewAuth ()
  {
    return newAuth;
  }
  public void setNewAuth (String newAuth)
  {
    this.newAuth = newAuth;
  }

  @Transient
  public String getNewShort ()
  {
    return newShort;
  }
  public void setNewShort (String newShort)
  {
    this.newShort = newShort;
  }

	// TODO Check if needed
  @Transient
  public String getSelectedTourney ()
  {
    return selectedTourney;
  }
  public void setSelectedTourney (String selectedTourney)
  {
    this.selectedTourney = selectedTourney;
  }

  @Column(name = "numberInGame", unique = false, nullable = false)
  public int getNumberInGame ()
  {
    return numberInGame;
  }
  public void setNumberInGame (int numberInGame)
  {
    this.numberInGame = numberInGame;
  }
  //</editor-fold>

}
