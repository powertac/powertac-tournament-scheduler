package org.powertac.tourney.beans;

import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.SpringApplicationContext;
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

  private String brokerName;
  private int brokerId = 0;
  private int userId = 0;
  private String brokerAuthToken;
  private String shortDescription;

  // For edit mode, web interface
  private boolean edit = false;
  private String newName;
  private String newAuth;
  private String newShort;
  // For registration, web interface
  private String selectedTourney;

  public Broker ()
  {
  }

  public Broker (ResultSet rs) throws SQLException
  {
    setBrokerId(rs.getInt("brokerId"));
    setUserId(rs.getInt("userId"));
    setBrokerName(rs.getString("brokerName"));
    setBrokerAuthToken(rs.getString("brokerAuth"));
    setShortDescription(rs.getString("brokerShort"));
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

  /**
   * Checks if not more than maxBrokers are running (only multi_game tourneys)
   */
  public boolean agentsAvailable(Database db) throws SQLException
  {
    Scheduler scheduler =
        (Scheduler) SpringApplicationContext.getBean("scheduler");
    Tournament runningTournament = scheduler.getRunningTournament();

    // When no tournament loaded (thus only SINGLE_GAMES will be run),
    // assume enough agents ready
    if (runningTournament == null ||
        runningTournament.getMaxAgents() == -1) {
      return true;
    }

    // Check if we have less than the max allowed # of instances running
    List<Agent> agents = db.getRunningAgents(getBrokerId());
    return agents.size() < runningTournament.getMaxAgents();
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
            && (startStamp - nowStamp) > loginDeadline) {
          if (t.getMaxBrokers() == -1 ||
              t.getNumberRegistered() < t.getMaxBrokers()){
            availableTourneys.add(t);
          }
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

  public Agent getAgent (int gameId)
  {
    Agent agent = null;

    Database db = new Database();
    try {
      db.startTrans();
      agent = db.getAgentByGameIdBrokerId(gameId, brokerId);
      db.commitTrans();
    }
    catch (SQLException e) {
      e.printStackTrace();
      db.abortTrans();
    }

    return agent;
  }

  public double getBalance (int gameId)
  {
    Agent agent = getAgent(gameId);

    if (agent != null) {
      return agent.getBalance();
    }

    return -2;
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

  @Transient
  public String getSelectedTourney ()
  {
    return selectedTourney;
  }
  public void setSelectedTourney (String selectedTourney)
  {
    this.selectedTourney = selectedTourney;
  }
  //</editor-fold>
}
