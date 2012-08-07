package org.powertac.tourney.beans;

import org.apache.log4j.Logger;

import javax.persistence.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import static javax.persistence.GenerationType.IDENTITY;


//Create hibernate mapping with annotations
@Entity
@Table(name = "poms", catalog = "tourney", uniqueConstraints = {
		@UniqueConstraint(columnNames = "pomId")})
public class Pom {
  private static Logger log = Logger.getLogger("TMLogger");

	private int pomId;
	private String name;
	private String uploadingUser;

  public Pom ()
  {
  }

  public Pom (ResultSet rs)
  {
    try {
      setName(rs.getString("name"));
      setUploadingUser(rs.getString("uploadingUser"));
      setPomId(rs.getInt("pomId"));
    }
    catch (SQLException sqle) {
      sqle.printStackTrace();
      log.error("Unable to create Pom from result set");
    }
  }

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "pomId", unique = true, nullable = false)
	public int getPomId() {
		return pomId;
	}
	public void setPomId(int pomId) {
		this.pomId = pomId;
	}
	
	@Column(name = "name", unique = true, nullable = false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "uploadingUser", unique = true, nullable = false)
	public String getUploadingUser() {
		return uploadingUser;
	}
	public void setUploadingUser(String uploadingUser) {
		this.uploadingUser = uploadingUser;
	}
}
