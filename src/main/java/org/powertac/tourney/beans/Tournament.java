package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.SpringApplicationContext;
import org.powertac.tourney.services.Utils;

import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;


// Technically not a managed bean, this is an internal Class to the 
// Tournaments bean which is an application scoped bean that acts as 
// a collection for all the active tournaments
@Entity
@Table(name = "tournaments", catalog = "tourney", uniqueConstraints = {
            @UniqueConstraint(columnNames = "tourneyId")})
public class Tournament
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int tourneyId = 0;
  private Date startTime;
  private Date dateFrom;
  private Date dateTo;
  private String tournamentName;
  private String status = STATE.pending.toString();

  private int maxBrokers;
  private int maxAgents = 2;

  private String locations = "";
  private String type = TYPE.SINGLE_GAME.toString();

  private int size1 = 2;
  private int size2 = 4;
  private int size3 = 8;

  private int pomId;
  private String pomName;

  public static enum STATE {
    pending, in_progress, complete
  }

  public static enum TYPE {
    SINGLE_GAME, MULTI_GAME
  }

  public Tournament ()
  {
  }

  public Tournament (ResultSet rs)
  {
    try {
      setStatus(rs.getString("status"));
      setTournamentId(rs.getInt("tourneyId"));
      setTournamentName(rs.getString("tourneyName"));
      setType(rs.getString("type"));
      setPomId(rs.getInt("pomId"));
      setMaxBrokers(rs.getInt("maxBrokers"));
      setStartTime(Utils.dateFormatUTCmilli(rs.getString("startTime")));
      setDateFrom(Utils.dateFormatUTCmilli(rs.getString("dateFrom")));
      setDateTo(Utils.dateFormatUTCmilli(rs.getString("dateTo")));
      setSize1(rs.getInt("gameSize1"));
      setSize2(rs.getInt("gameSize2"));
      setSize3(rs.getInt("gameSize3"));
      setMaxAgents(rs.getInt("maxAgents"));
      setTournamentName(rs.getString("tourneyName"));
      setLocations(rs.getString("locations"));
    }
    catch (Exception e) {
      log.error("Error creating tournament from result set");
      e.printStackTrace();
    }
  }

  public void setTournametInProgress(Database db) throws SQLException
  {
    if (!stateEquals(STATE.in_progress)) {
      db.updateTournamentStatus(tourneyId, STATE.in_progress);
    }
  }

  /**
   * If a game is complete, check if it was the last one to complete
   * If so, set tournament state to complete
   */
  public void processGameFinished(Database db, int finishedGameId)
      throws SQLException
  {
    boolean allDone = true;

    for (Game g: getGames()) {
      // The state of the finished game isn't in the db yet.
      if (g.getGameId() == finishedGameId) {
        continue;
      }
      if (!g.stateEquals(Game.STATE.game_complete)) {
        allDone = false;
      }
    }

    if (allDone) {
      db.updateTournamentStatus(tourneyId, STATE.complete);

      Scheduler scheduler =
          (Scheduler) SpringApplicationContext.getBean("scheduler");
      scheduler.unloadTournament();
    }
  }

  @Transient
  public List<Game> getGames ()
  {
    List<Game> result = new ArrayList<Game>();
    Database db = new Database();
    try {
      db.startTrans();
      result = db.getGamesInTourney(tourneyId);
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
      e.printStackTrace();
    }

    return result;
  }

  @Transient
  public String toUTCStartTime ()
  {
    return Utils.dateFormatUTC(startTime);
  }

  public boolean typeEquals(TYPE type) {
    return this.type.equals(type.toString());
  }

  public String remove ()
  {
    Database db = new Database();

    try {
      db.startTrans();

      List<Game> games = db.getGamesInTourney(tourneyId);
      List<Broker> brokers = db.getBrokersInTournament(tourneyId);

      // Disallow removal when games booting or running
      for (Game g: games) {
        if (g.stateEquals(Game.STATE.boot_in_progress) ||
            g.stateEquals(Game.STATE.game_pending) ||
            g.stateEquals(Game.STATE.game_ready) ||
            g.stateEquals(Game.STATE.game_in_progress)) {
          db.abortTrans();
          return String.format("Game %s can not be removed, state = %s",
                               g.getGameName(), g.getStatus());
        }
      }

      // Remove all registrations for this tournament
      for (Broker broker: brokers) {
        db.unregisterBroker(tourneyId, broker.getBrokerId());
      }

      for (Game game: games) {
        // Remove all agents
        db.deleteAgentsByGameId(game.getGameId());
        // Remove all properties for games for this tournament
        db.deletePropertiesByGameId(game.getGameId());
        // Remove all games for a tournament
        db.deleteGame(game.getGameId());
      }

      // Remove tournament
      db.deleteTournament(tourneyId);

      db.commitTrans();
      return "";
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
      return "Database error :" + e.getMessage();
    }
  }

  public static List<Tournament> getTournamentList ()
  {
    List<Tournament> ts = new ArrayList<Tournament>();

    Database db = new Database();
    try {
      db.startTrans();
      ts = db.getTournaments(STATE.pending);
      ts.addAll(db.getTournaments(STATE.in_progress));
      db.commitTrans();
    }
    catch(Exception e){
      db.abortTrans();
    }

    return ts;
  }

  public boolean stateEquals(STATE state)
  {
    return this.status.equals(state.toString());
  }

  public List<String> getLocationsList ()
  {
    List<String> locationList = new ArrayList<String>();
    for (String location: locations.split(",")) {
      locationList.add(location.trim());
    }
    return locationList;
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

  @Column(name = "gameSize1", unique = false, nullable = false)
  public int getSize1 ()
  {
    return size1;
  }
  public void setSize1 (int size1)
  {
    this.size1 = size1;
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

  @Column(name = "gameSize3", unique = false, nullable = false)
  public int getSize3 ()
  {
    return size3;
  }
  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }


  @Column(name = "maxAgents", unique = false, nullable = false)
  public int getMaxAgents()
  {
    return maxAgents;
  }
  public void setMaxAgents(int maxAgents)
  {
    this.maxAgents = maxAgents;
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
  public void setTournamentId (int tourneyId)
  {
    this.tourneyId = tourneyId;
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

  @Column(name = "dateFrom", unique = false, nullable = false)
  public Date getDateFrom() {
    return dateFrom;
  }
  public void setDateFrom(Date dateFrom) {
    this.dateFrom = dateFrom;
  }

  @Column(name = "dateTo", unique = false, nullable = false)
  public Date getDateTo() {
    return dateTo;
  }
  public void setDateTo(Date dateTo) {
    this.dateTo = dateTo;
  }
  //</editor-fold>
}
