package org.powertac.tourney.beans;

import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.Utils;

import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;
import static org.powertac.tourney.services.Utils.log;


// Technically not a managed bean, this is an internal Class to the 
// Tournaments bean which is an application scoped bean that acts as 
// a collection for all the active tournaments
@Entity
@Table(name = "tournaments", catalog = "tourney", uniqueConstraints = {
            @UniqueConstraint(columnNames = "tourneyId")})
public class Tournament
{
  private int tourneyId = 0;
  private Date startTime;
  private String tournamentName;
  private String status = STATE.pending.toString();
  private int maxBrokers; // -1 means inf, otherwise integer specific
  private boolean openRegistration = false;
  private int maxGames;
  private String locations = "";

  private int size1 = 2;
  private int numberSize1 = 2;
  private int size2 = 4;
  private int numberSize2 = 4;
  private int size3 = 8;
  private int numberSize3 = 4;
  
  private String type = TYPE.SINGLE_GAME.toString();

  private int maxBrokerInstances = 2;

  private int pomId;
  private String pomName;

  // TODO Set completed Tournaments to 'complete'. Combine with Hibernate?
  public static enum STATE {
    pending, in_progress, complete
  }

  public static enum TYPE {
    SINGLE_GAME, MULTI_GAME
  }

  public Tournament ()
  {
  }

  public Tournament (ResultSet rsTs)
  {
    try {
      setStatus(rsTs.getString("status"));
      setTournamentId(rsTs.getInt("tourneyId"));
      setTournamentName(rsTs.getString("tourneyName"));
      setOpenRegistration(rsTs.getBoolean("openRegistration"));
      setType(rsTs.getString("type"));
      setMaxGames(rsTs.getInt("maxGames"));
      setPomId(rsTs.getInt("pomId"));
      setMaxBrokers(rsTs.getInt("maxBrokers"));
      setStartTime(Utils.dateFormatUTCmilli((rsTs.getString("startTime"))));
      setSize1(rsTs.getInt("gameSize1"));
      setSize2(rsTs.getInt("gameSize2"));
      setSize3(rsTs.getInt("gameSize3"));
      setNumberSize1(rsTs.getInt("numberGameSize1"));
      setNumberSize2(rsTs.getInt("numberGameSize2"));
      setNumberSize3(rsTs.getInt("numberGameSize3"));
      setMaxBrokerInstances(rsTs.getInt("maxBrokerInstances"));
      setTournamentName(rsTs.getString("tourneyName"));
    }
    catch (Exception e) {
      log("[ERROR] Error creating tournament from result set");
      e.printStackTrace();
    }
  }

  @Transient
  public List<Game> getGames ()
  {
    List<Game> result = new ArrayList<Game>();
    Database db = new Database();
    try {
      db.startTrans();
      result = db.getGamesInTourney(this.tourneyId);
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }
    return result;
  }

  @Transient
  public int getNumberRegistered ()
  {
    Database db = new Database();
    int result = 0;
    try {
      db.startTrans();
      result = db.getBrokersInTournament(tourneyId).size();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return result;
  }

  @Transient
  public String toUTCStartTime ()
  {
    return Utils.dateFormatUTC(startTime);
  }

  // TODO Cleanup. Still needed ??
  /*
  // Probably Should check name against auth token
  private HashMap<Integer, String> registeredBrokers;

  public boolean isRegistered (String authToken)
  {
    return registeredBrokers.containsValue(authToken);
  }

  // This goes into the default constructor
  registeredBrokers = new HashMap<Integer, String>();
  */

  public boolean typeEquals(TYPE type) {
    return this.type.equals(type.toString());
  }

  //<editor-fold desc="Getters and setters">
  @Column(name = "pomName", unique = false, nullable = false)
  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    this.pomName = pomName;
  }

  @Column(name = "openRegistration", unique = false, nullable = false)
  public boolean getOpenRegistration ()
  {
    return openRegistration;
  }

  public void setOpenRegistration (boolean openRegistration)
  {
    this.openRegistration = openRegistration;
  }

  @Column(name = "maxGames", unique = false, nullable = false)
  public int getMaxGames ()
  {
    return maxGames;
  }

  public void setMaxGames (int maxGames)
  {
    this.maxGames = maxGames;
  }

  @Column(name = "gameSize1", unique = false, nullable = false)
  public int getSize1 ()
  {
    return size1;
  }

  public void setSize1 (int size1)
  {
    this.size1 = size1;
  }

  @Column(name = "maxGames", unique = false, nullable = false)
  public int getNumberSize1 ()
  {
    return numberSize1;
  }

  public void setNumberSize1 (int numberSize1)
  {
    this.numberSize1 = numberSize1;
  }

  @Column(name = "gameSize2", unique = false, nullable = false)
  public int getSize2 ()
  {
    return size2;
  }

  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  @Column(name = "numberGameSize2", unique = false, nullable = false)
  public int getNumberSize2 ()
  {
    return numberSize2;
  }

  public void setNumberSize2 (int numberSize2)
  {
    this.numberSize2 = numberSize2;
  }

  @Column(name = "gameSize3", unique = false, nullable = false)
  public int getSize3 ()
  {
    return size3;
  }

  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  @Column(name = "numberGameSize3", unique = false, nullable = false)
  public int getNumberSize3 ()
  {
    return numberSize3;
  }

  public void setNumberSize3 (int numberSize3)
  {
    this.numberSize3 = numberSize3;
  }

  @Column(name = "maxGames", unique = false, nullable = false)
  public int getMaxBrokerInstances ()
  {
    return maxBrokerInstances;
  }

  public void setMaxBrokerInstances (int maxBrokerInstances)
  {
    this.maxBrokerInstances = maxBrokerInstances;
  }

  @Column(name = "type", unique = false, nullable = false)
  public String getType ()
  {
    return type;
  }

  public void setType (String type)
  {
    this.type = type;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "tourneyId", unique = true, nullable = false)
  public int getTournamentId ()
  {
    return tourneyId;
  }

  public void setTournamentId (int competitionId)
  {
    this.tourneyId = competitionId;
  }

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

  @Column(name = "tourneyName", unique = false, nullable = false)
  public String getTournamentName ()
  {
    return tournamentName;
  }

  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  @Column(name = "maxBrokers", unique = false, nullable = false)
  public int getMaxBrokers ()
  {
    return maxBrokers;
  }

  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  @Column(name = "pomId", unique = false, nullable = false)
  public int getPomId ()
  {
    return pomId;
  }

  public void setPomId (int pomId)
  {
    this.pomId = pomId;
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
 
  @Column(name = "locations", unique = false, nullable = false)
  public String getLocations ()
  {
    return locations;
  }

  public void setLocations (String locations)
  {
    this.locations = locations;
  }
  //</editor-fold>
}
