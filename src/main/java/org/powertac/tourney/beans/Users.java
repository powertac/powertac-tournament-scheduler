package org.powertac.tourney.beans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.powertac.tourney.services.Database;


@ApplicationScoped
@ManagedBean
public class Users
{
  private String sortColumn = null;
  private boolean sortAscending = true;
  private int rowCount = 5;

  private static final String key = "users";

  public List<Database.User> getUserList ()
  {
    Database db = new Database();

    try {
      return db.getAllUsers();
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new ArrayList<Database.User>();
  }

  public static String getKey ()
  {
    return key;
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
