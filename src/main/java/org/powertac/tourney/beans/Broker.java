package org.powertac.tourney.beans;

import org.apache.commons.codec.digest.DigestUtils;

import javax.persistence.*;
import java.sql.ResultSet;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "brokers", catalog = "tourney", uniqueConstraints = {
            @UniqueConstraint(columnNames = "brokerId")})
public class Broker
{
  private static final String key = "broker";
  private static int maxBrokerId = 0;

  // For edit mode
  private boolean edit = false;
  private String newName;
  private String newAuth;
  private String newShort;

  // For registration
  private String selectedTourney;

  private String brokerName;
  private int brokerId = 0;
  private String brokerAuthToken;
  private String shortDescription;
  private int numberInGame = 0;

  public Broker (ResultSet rs)
  {

  }

  public Broker (String brokerName)
  {
    this(brokerName, "");
  }

  public Broker (String brokerName, String shortDescription)
  {
    this.brokerName = brokerName;
    this.shortDescription = shortDescription;
    brokerId = maxBrokerId;
    maxBrokerId++;

    // Generate MD5 hash
    brokerAuthToken = DigestUtils.md5Hex(brokerName + brokerId +
        (new Date()).toString() + Math.random());
  }

  @Column(name = "brokerName", unique = false, nullable = false)
  public String getBrokerName ()
  {
    return brokerName;
  }

  public void setBrokerName (String brokerName)
  {
    this.brokerName = brokerName;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "brokerId", unique = true, nullable = false)
  public int getBrokerId ()
  {
    return brokerId;
  }

  public void setBrokerId (int brokerId)
  {
    this.brokerId = brokerId;
  }

  @Column(name = "brokerAuth", unique = true, nullable = false)
  public String getBrokerAuthToken ()
  {
    return brokerAuthToken;
  }

  public void setBrokerAuthToken (String brokerAuthToken)
  {
    this.brokerAuthToken = brokerAuthToken;
  }

  @Column(name = "brokerShort", unique = false, nullable = false)
  public String getShortDescription ()
  {
    return shortDescription;
  }

  public void setShortDescription (String shortDescription)
  {
    if (shortDescription != null && shortDescription.length() >= 200) {
      this.shortDescription = shortDescription.substring(0, 199);
    }
    else {

      this.shortDescription = shortDescription;
    }
  }

  @Transient
  public boolean isEdit ()
  {
    return edit;
  }

  public void setEdit (boolean edit)
  {
    this.edit = edit;
  }

  @Transient
  public String getNewName ()
  {
    return newName;
  }

  public void setNewName (String newName)
  {
    this.newName = newName;
  }

  @Transient
  public String getNewAuth ()
  {
    return newAuth;
  }

  public void setNewAuth (String newAuth)
  {
    this.newAuth = newAuth;
  }

  @Transient
  public String getNewShort ()
  {
    return newShort;
  }

  public void setNewShort (String newShort)
  {
    this.newShort = newShort;
  }

  @Transient
  public String getSelectedTourney ()
  {
    return selectedTourney;
  }

  public void setSelectedTourney (String selectedTourney)
  {
    this.selectedTourney = selectedTourney;
  }

  @Column(name = "numberInGame", unique = false, nullable = false)
  public int getNumberInGame ()
  {
    return numberInGame;
  }

  public void setNumberInGame (int numberInGame)
  {
    this.numberInGame = numberInGame;
  }

}
