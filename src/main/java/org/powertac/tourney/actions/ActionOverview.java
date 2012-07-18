package org.powertac.tourney.actions;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.RunBootstrap;
import org.powertac.tourney.services.RunGame;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.powertac.tourney.services.Utils.log;

@ManagedBean
@RequestScoped
public class ActionOverview
{
  private String sortColumnBrokers = null;
  private boolean sortAscendingBrokers = true;

  private String sortColumnTournaments = null;
  private boolean sortAscendingTournaments = true;

  private String sortColumnGames = null;
  private boolean sortAscendingGames = true;

  public ActionOverview()
  {
  }

  public List<Broker> getBrokerList ()
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

  public String getTournamentByBrokerId(int brokerId){
    String result = "";

    List<Tournament> tournaments = new ArrayList<Tournament>();
    Database db = new Database();
    try {
      db.startTrans();
      tournaments = db.getTournamentsByBrokerId(brokerId);
      db.commitTrans();
    }
    catch(Exception e) {
      db.abortTrans();
    }

    for (Tournament t: tournaments) {
      result += t.getTournamentName() + ", ";
    }
    result = result.substring(0, result.length()-2);

    return result;
  }

  public List<Tournament> getTournamentList(){
    List<Tournament> ts = new ArrayList<Tournament>();
    
    Database db = new Database();
    try {
      db.startTrans();
      ts = db.getTournaments(Tournament.STATE.pending);
      ts.addAll(db.getTournaments(Tournament.STATE.in_progress));
      db.commitTrans();
    }
    catch(Exception e) {
      db.abortTrans();
    }

    return ts;
  }

  public List<Game> getGameList()
  {
    List<Game> games = new ArrayList<Game>();

    Database db = new Database();
    try {
      db.startTrans();
      games = db.getGames();
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
    }

    return games;
  }

  public void restartGame (Game g)
  {
    Database db = new Database();
    int gameId = g.getGameId();
    log("[INFO] Restarting Game {0} has status: {1}", gameId, g.getStatus());
    Tournament t = new Tournament();

    try {
      db.startTrans();
      t = db.getTournamentByGameId(gameId);

      db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
      log("[INFO] Setting machine: {0} to idle", g.getMachineId());
      db.commitTrans();

    }
    catch (SQLException e) {
      db.abortTrans();
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (g.stateEquals(Game.STATE.boot_failed) ||
        g.stateEquals(Game.STATE.boot_pending) ||
        g.stateEquals(Game.STATE.boot_in_progress) ) {
      log("[INFO] Attempting to restart bootstrap {0}", gameId);

      RunBootstrap runBootstrap = new RunBootstrap(gameId, t.getPomId());
      new Thread(runBootstrap).start();
    }
    else if (g.stateEquals(Game.STATE.game_failed) ||
        g.stateEquals(Game.STATE.game_in_progress) ||
        g.stateEquals(Game.STATE.boot_failed) ) {
      log("[INFO] Attempting to restart sim {0}", gameId);

      RunGame runGame = new RunGame(g.getGameId(), t.getPomId());
      new Thread(runGame).start();
    }
  }

  /**
   * We should be able to delete games
   * But should we be able to abandon running games?
   * @param g : the Game to delete
   */
  public void deleteGame (Game g)
  {
    // TODO: ARE YOU SURE?
    //scheduler.deleteBootTimer(g.getGameId());
    //scheduler.deleteSimTimer(g.getGameId());
  }

  public void refresh ()
  {
  }

  //<editor-fold desc="Setters and Getters">
  public boolean isSortAscendingBrokers()
  {
    return sortAscendingBrokers;
  }
  public void setSortAscendingBrokers(boolean sortAscendingBrokers)
  {
    this.sortAscendingBrokers = sortAscendingBrokers;
  }

  public String getSortColumnBrokers()
  {
    return sortColumnBrokers;
  }
  public void setSortColumnBrokers(String sortColumnBrokers)
  {
    this.sortColumnBrokers = sortColumnBrokers;
  }

  public String getSortColumnTournaments ()
  {
    return sortColumnTournaments;
  }
  public void setSortColumnTournaments (String sortColumnTournaments)
  {
    this.sortColumnTournaments = sortColumnTournaments;
  }

  public boolean isSortAscendingTournaments ()
  {
    return sortAscendingTournaments;
  }
  public void setSortAscendingTournaments (boolean sortAscendingTournaments)
  {
    this.sortAscendingTournaments = sortAscendingTournaments;
  }

  public String getSortColumnGames ()
  {
    return sortColumnGames;
  }
  public void setSortColumnGames (String sortColumnGames)
  {
    this.sortColumnGames = sortColumnGames;
  }

  public boolean isSortAscendingGames ()
  {
    return sortAscendingGames;
  }
  public void setSortAscendingGames (boolean sortAscendingGames)
  {
    this.sortAscendingGames = sortAscendingGames;
  }
  //</editor-fold>
}
