package org.powertac.tourney.beans;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "locations", catalog = "tourney", uniqueConstraints = {
    @UniqueConstraint(columnNames = "locationId")})
public class Location {
  private int locationId;
  private String location;
  private int timezone;
  private Date fromDate;
  private Date toDate;

  public Location() {
  }

  @SuppressWarnings("unchecked")
  public static List<Location> getLocationList() {
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
  public int getLocationId() {
    return locationId;
  }

  public void setLocationId(int locationId) {
    this.locationId = locationId;
  }

  @Column(name = "location", nullable = false)
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  @Column(name = "timezone", nullable = false)
  public int getTimezone() {
    return timezone;
  }

  public void setTimezone(int timezone) {
    this.timezone = timezone;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "fromDate", nullable = false, length = 10)
  public Date getFromDate() {
    return fromDate;
  }

  public void setFromDate(Date fromDate) {
    this.fromDate = fromDate;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "toDate", nullable = false, length = 10)
  public Date getToDate() {
    return toDate;
  }

  public void setToDate(Date toDate) {
    this.toDate = toDate;
  }
  //</editor-fold>
}
