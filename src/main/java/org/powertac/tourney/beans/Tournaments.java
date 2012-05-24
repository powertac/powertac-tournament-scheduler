package org.powertac.tourney.beans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.powertac.tourney.services.Database;


@ApplicationScoped
@ManagedBean
public class Tournaments
{
  private static final String key = "tournaments";

  private Vector<Tournament> tournaments;

  private String sortColumn = null;
  private boolean sortAscending = true;
  private int rowCount = 5;

  public Tournaments ()
  {
    tournaments = new Vector<Tournament>();
  }

  public static String getKey ()
  {
    return key;
  }

  public static Tournaments getAllTournaments ()
  {

    return (Tournaments) FacesContext.getCurrentInstance().getExternalContext()
            .getApplicationMap().get(Tournaments.getKey());
  }

  public void addTournament (Tournament t)
  {
    this.tournaments.add(t);
  }

  public List<Tournament> getTournamentList ()
  {
    Database db = new Database();

    List<Tournament> ts = new ArrayList<Tournament>();

    try {
      ts = db.getTournaments("pending");
      ts.addAll(db.getTournaments("in-progress"));
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return ts;
  }

  public List<Tournament> getLists ()
  {
    return getTournamentList();

    // return (List<Tournament>) tournaments;
  }

  public Tournament getTournamentById (int id)
  {
    Database db = new Database();
    Tournament t = new Tournament();
    try {
      t = db.getTournamentById(id);
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return t;
  }

  public String getSortColumn ()
  {
    return sortColumn;
  }

  public void setSortColumn (String sortColumn)
  {
    this.sortColumn = sortColumn;
  }

  public boolean isSortAscending ()
  {
    return sortAscending;
  }

  public void setSortAscending (boolean sortAscending)
  {
    this.sortAscending = sortAscending;
  }

  public int getRowCount ()
  {
    return rowCount;
  }

  public void setRowCount (int rowCount)
  {
    this.rowCount = rowCount;
  }
}
