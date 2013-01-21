package org.powertac.tourney.beans;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;

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
    initTime.set(2009, Calendar.MARCH, 3, 0, 0, 0);
    dateFrom = new Date();
    dateFrom.setTime(initTime.getTimeInMillis());
    initTime.set(2011, Calendar.MARCH, 3, 0, 0, 0);
    dateTo = new Date();
    dateTo.setTime(initTime.getTimeInMillis());
    location = "rotterdam";
  }

  @SuppressWarnings("unchecked")
  public static List<Location> getLocationList ()
  {
    List<Location> locations = new ArrayList<Location>();

    Session session = HibernateUtil.getSessionFactory().openSession();
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
