package org.powertac.tourney.beans;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

//Create hibernate mapping with annotations
@Entity
@Table(name = "locations", catalog = "tourney", uniqueConstraints = {
              @UniqueConstraint(columnNames = "locationId")})
public class Location
{
  private String name;
  private Date fromDate;
  private Date toDate;
  private int locationId;

  
  @Column(name = "name", unique = false, nullable = false)
  public String getName ()
  {
    return name;
  }

  public void setName (String name)
  {
    this.name = name;
  }

  
  @Temporal(TemporalType.DATE)
  @Column(name = "fromDate", unique = false, nullable = false, length = 10)
  public Date getFromDate ()
  {
    return fromDate;
  }

  public void setFromDate (Date fromDate)
  {
    this.fromDate = fromDate;
  }

  @Temporal(TemporalType.DATE)
  @Column(name = "toDate", unique = false, nullable = false, length = 10)
  public Date getToDate ()
  {
    return toDate;
  }

  public void setToDate (Date toDate)
  {
    this.toDate = toDate;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "locationId", unique = true, nullable = false)
  public int getLocationId ()
  {
    return locationId;
  }

  public void setLocationId (int locationId)
  {
    this.locationId = locationId;
  }
}
