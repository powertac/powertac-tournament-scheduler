package org.powertac.tournament.beans;

import org.hibernate.*;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "locations")
public class Location
{
  private int locationId;
  private String location;
  private int timezone;
  private Date dateFrom;
  private Date dateTo;

  public Location ()
  {
    // TODO Get this from some config
    Calendar initTime = Calendar.getInstance();
    initTime.set(2009, Calendar.JANUARY, 1, 0, 0, 0);
    dateFrom = new Date();
    dateFrom.setTime(initTime.getTimeInMillis());
    initTime.set(2011, Calendar.JULY, 1, 0, 0, 0);
    dateTo = new Date();
    dateTo.setTime(initTime.getTimeInMillis());
    location = "rotterdam";
  }

  @SuppressWarnings("unchecked")
  public static List<Location> getLocationList ()
  {
    List<Location> locations = new ArrayList<Location>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      org.hibernate.Query query = session.createQuery(Constants.HQL.GET_LOCATIONS);
      locations = (List<Location>) query.list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return locations;
  }

  public static Location getLocationByName (String name)
  {
    Location location = null;

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      org.hibernate.Query query = session.
          createQuery(Constants.HQL.GET_LOCATION_BY_NAME);
      query.setString("locationName", name);
      location = (Location) query.uniqueResult();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
    return location;
  }

  @Transient
  public String getFullString ()
  {
    String fullString = String.format("%-16s : %s - %s", location,
      dateFrom.toString().substring(0, 10), dateTo.toString().substring(0, 10));
    return fullString.replace(" ", "&nbsp;");
  }

  //<editor-fold desc="Setters and Getters">
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

  @Column(name = "location", nullable = false)
  public String getLocation ()
  {
    return location;
  }

  public void setLocation (String location)
  {
    this.location = location;
  }

  @Column(name = "timezone", nullable = false)
  public int getTimezone ()
  {
    return timezone;
  }

  public void setTimezone (int timezone)
  {
    this.timezone = timezone;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "fromDate", nullable = false, length = 10)
  public Date getDateFrom ()
  {
    return dateFrom;
  }

  public void setDateFrom (Date dateFrom)
  {
    this.dateFrom = dateFrom;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "toDate", nullable = false, length = 10)
  public Date getDateTo ()
  {
    return dateTo;
  }

  public void setDateTo (Date dateTo)
  {
    this.dateTo = dateTo;
  }
  //</editor-fold>
}
