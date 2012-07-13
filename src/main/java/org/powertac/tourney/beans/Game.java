package org.powertac.tourney.beans;

import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.SpringApplicationContext;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Utils;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;
import static org.powertac.tourney.services.Utils.log;


// Create hibernate mapping with annotations
@Entity
@Table(name = "games", catalog = "tourney", uniqueConstraints = {
		@UniqueConstraint(columnNames = "gameId")})
public class Game implements Serializable
{
  private String competitionName = "";
  private Date startTime;
  private int competitionId = -1;
  private int tourneyId = 0;
  private int gameId = 0;
  private int machineId;
  private String status = STATE.boot_pending.toString();
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

  public static enum STATE {
    boot_pending, boot_in_progress, boot_complete, boot_failed,
    game_pending, game_ready, game_in_progress, game_complete, game_failed
  }

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
      log("[ERROR] Error creating game from result set");
      e.printStackTrace();
    }
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

  public boolean isBrokerRegistered (String authToken)
  {
    log("Broker token: {0}", authToken);
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

  // TODO Make this an object method, combine with Hibernate
  // TODO Add status machine for Game
  public static String handleStatus(String status, int gameId)
  {
    log("[INFO] Recieved {0} message from game: {1}", status, gameId);

    STATE state;
    try {
      state = STATE.valueOf(status);
    }
    catch (Exception e) {
      return "error";
    }

    Scheduler scheduler = (Scheduler) SpringApplicationContext.getBean("scheduler");
    Database db = new Database();

    try {
      db.startTrans();
      Game g = db.getGame(gameId);

      db.updateGameStatusById(gameId, state);
      log("[INFO] Update game: {0} to {1}", gameId, status);

      switch (state) {
        case boot_in_progress:
          // Remove bootfile, it shouldn't exist anyway
          g.removeBootFile();
          break;

        case boot_complete:
          db.updateGameBootstrapById(gameId, true);
          log("[INFO] Update game: {0} to hasBootstrap", gameId);

          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          Scheduler.bootRunning = false;
          break;

        case boot_failed:
          log("[WARN] BOOT {0} FAILED!", gameId);

          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          Scheduler.bootRunning = false;
          break;

        case game_complete:
          db.updateGameFreeBrokers(gameId);
          log("[INFO] Freeing Brokers for game: {0}", gameId);
          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          scheduler.resetServer(g.getMachineId());
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          break;

        case game_failed:
          log("[WARN] GAME {0} FAILED!", gameId);

          db.updateGameFreeBrokers(gameId);
          log("[INFO] Freeing Brokers for game: {0}", gameId);
          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          scheduler.resetServer(g.getMachineId());
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          break;
      }

      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
    return "success";
  }

  private void removeBootFile()
  {
    TournamentProperties properties = new TournamentProperties();
    String bootLocation = properties.getProperty("bootLocation") +
        gameId + "-boot.xml";
    File f = new File(bootLocation);

    if (!f.exists()) {
      return;
    }

    if (!f.canWrite()) {
      log("[Error] Write protected: {0}", bootLocation);
    }

    if (!f.delete()) {
      log("[Error] Failed to delete : {0}", bootLocation);
    }
  }

  public String toUTCStartTime ()
  {
    return Utils.dateFormatUTC(startTime);
  }

  public boolean stateEquals(STATE state)
  {
    return this.status.equals(state.toString());
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
