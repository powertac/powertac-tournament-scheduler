package com.powertac.tourney.beans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.powertac.tourney.services.Database;

@ManagedBean(eager = true)
@ApplicationScoped
public class Locations
{
  private Vector<Location> locations;

  public Locations ()
  {
    setLocations(new Vector<Location>());

    // TODO: Load from properties file
    /**
     * Location tl = new Location();
     * tl.setName("minneapolis");
     * tl.setFromDate(Calendar.getInstance());
     * tl.setToDate(Calendar.getInstance());
     * 
     * tl.getFromDate().set(Calendar.YEAR, 2004);
     * tl.getToDate().set(Calendar.YEAR, 2012);
     * 
     * locations.add(tl);
     */

  }

  public Vector<Location> getLocations ()
  {
    return locations;
  }

  public void setLocations (Vector<Location> locations)
  {
    this.locations = locations;
  }

  public List<Location> getLocationList ()
  {

    Database db = new Database();
    List<Location> result = new ArrayList<Location>();
    try {
      result = db.getLocations();
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return result;
    /*
     * Location[] tmp = new Location[locations.size()];
     * int i = 0;
     * for(Location l : locations){
     * tmp[i++] = l;
     * }
     * 
     * return tmp;
     */
  }

}
