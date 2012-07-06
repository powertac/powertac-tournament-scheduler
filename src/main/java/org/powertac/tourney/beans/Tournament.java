package org.powertac.tourney.beans;

import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


// Technically not a managed bean, this is an internal Class to the 
// Tournaments bean which is an application scoped bean that acts as 
// a collection for all the active tournaments
@ManagedBean
public class Tournament
{
  private int tourneyId = 0;
  private Date startTime;
  private String tournamentName;
  private String status = "pending";
  private int maxBrokers; // -1 means inf, otherwise integer specific
  private boolean openRegistration = false;
  private int maxGames;

  private int size1 = 2;
  private int numberSize1 = 2;
  private int size2 = 4;
  private int numberSize2 = 4;
  private int size3 = 8;
  private int numberSize3 = 4;
  
  private String type = "SINGLE_GAME";

  private int maxBrokerInstances = 2;

  private String pomName;

  // Probably Should check name against auth token
  private HashMap<Integer, String> registeredBrokers;

  private String pomUrl;

  private HashMap<Integer, Game> allGames;

  public Tournament ()
  {
    registeredBrokers = new HashMap<Integer, String>();
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
      setPomUrl(rsTs.getString("pomUrl"));
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
      System.out.println("[ERROR] Error creating tournament from result set");
      e.printStackTrace();
    }
  }

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

  public String toUTCStartTime ()
  {
    return Utils.dateFormatUTC(startTime);
  }

  // TODO Still needed ??
  /*
  public boolean isRegistered (String authToken)
  {
    return registeredBrokers.containsValue(authToken);
  }
  */

  //<editor-fold desc="Getters and setters">
  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    this.pomName = pomName;
  }

  public boolean getOpenRegistration ()
  {
    return openRegistration;
  }

  public void setOpenRegistration (boolean openRegistration)
  {
    this.openRegistration = openRegistration;
  }

  public int getMaxGames ()
  {
    return maxGames;
  }

  public void setMaxGames (int maxGames)
  {
    this.maxGames = maxGames;
  }

  public int getSize1 ()
  {
    return size1;
  }

  public void setSize1 (int size1)
  {
    this.size1 = size1;
  }

  public int getNumberSize1 ()
  {
    return numberSize1;
  }

  public void setNumberSize1 (int numberSize1)
  {
    this.numberSize1 = numberSize1;
  }

  public int getSize2 ()
  {
    return size2;
  }

  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  public int getNumberSize2 ()
  {
    return numberSize2;
  }

  public void setNumberSize2 (int numberSize2)
  {
    this.numberSize2 = numberSize2;
  }

  public int getSize3 ()
  {
    return size3;
  }

  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  public int getNumberSize3 ()
  {
    return numberSize3;
  }

  public void setNumberSize3 (int numberSize3)
  {
    this.numberSize3 = numberSize3;
  }

  public int getMaxBrokerInstances ()
  {
    return maxBrokerInstances;
  }

  public void setMaxBrokerInstances (int maxBrokerInstances)
  {
    this.maxBrokerInstances = maxBrokerInstances;
  }

  public String getType ()
  {
    return type;
  }

  public void setType (String type)
  {
    this.type = type;
  }

  public int getTournamentId ()
  {
    return tourneyId;
  }

  public void setTournamentId (int competitionId)
  {
    this.tourneyId = competitionId;
  }

  public Date getStartTime ()
  {
    return startTime;
  }

  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  public String getTournamentName ()
  {
    return tournamentName;
  }

  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  public int getMaxBrokers ()
  {
    return maxBrokers;
  }

  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  public String getPomUrl ()
  {
    return pomUrl;
  }

  public void setPomUrl (String pomUrl)
  {
    this.pomUrl = pomUrl;
  }

  public String getStatus ()
  {
    return status;
  }
  public void setStatus (String status)
  {
    this.status = status;
  }
  //</editor-fold>
}
