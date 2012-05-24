package org.powertac.tourney.beans;

import java.util.Calendar;
import java.util.Date;

import javax.faces.bean.ManagedBean;

@ManagedBean
public class Location
{
  private String name;
  private Date fromDate;
  private Date toDate;
  private int locationId;

  public String getName ()
  {
    return name;
  }

  public void setName (String name)
  {
    this.name = name;
  }

  public Date getFromDate ()
  {
    return fromDate;
  }

  public void setFromDate (Date fromDate)
  {
    this.fromDate = fromDate;
  }

  public Date getToDate ()
  {
    return toDate;
  }

  public void setToDate (Date toDate)
  {
    this.toDate = toDate;
  }

  public int getLocationId ()
  {
    return locationId;
  }

  public void setLocationId (int locationId)
  {
    this.locationId = locationId;
  }
}
