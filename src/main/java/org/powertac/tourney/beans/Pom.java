package org.powertac.tourney.beans;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;


//Create hibernate mapping with annotations
@Entity
@Table(name = "poms", catalog = "tourney", uniqueConstraints = {
		@UniqueConstraint(columnNames = "pomId")})
public class Pom {
	private int pomId;
	private String name;
	private String uploadingUser;

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
