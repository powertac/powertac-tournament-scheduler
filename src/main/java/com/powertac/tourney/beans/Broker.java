package com.powertac.tourney.beans;

import java.util.Date;

import javax.faces.bean.ManagedBean;

import org.apache.commons.codec.digest.DigestUtils;

@ManagedBean
public class Broker {
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

	public Broker(String brokerName) {
		this.brokerName = brokerName;
		//System.out.println("Created Broker Bean: " + brokerId);
		brokerId = maxBrokerId;
		maxBrokerId++;

		// Generate MD5 hash
		DigestUtils dg = new DigestUtils();
		brokerAuthToken = dg.md5Hex(brokerName + brokerId
				+ (new Date()).toString() + Math.random());

	}

	public Broker(String brokerName, String shortDescription) {
		this.brokerName = brokerName;
		this.shortDescription = shortDescription;
		System.out.println("Created Broker Bean: " + brokerId);
		brokerId = maxBrokerId;
		maxBrokerId++;

		// Generate MD5 hash
		DigestUtils dg = new DigestUtils();
		brokerAuthToken = dg.md5Hex(brokerName + brokerId
				+ (new Date()).toString() + Math.random());

	}

	public String getBrokerName() {
		return brokerName;
	}

	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}

	public int getBrokerId() {
		return brokerId;
	}

	public void setBrokerId(int brokerId) {
		this.brokerId = brokerId;
	}

	public String getBrokerAuthToken() {
		return brokerAuthToken;
	}

	public void setBrokerAuthToken(String brokerAuthToken) {
		this.brokerAuthToken = brokerAuthToken;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		if (shortDescription != null && shortDescription.length() >= 200) {
			this.shortDescription = shortDescription.substring(0, 199);
		} else {

			this.shortDescription = shortDescription;
		}
	}

	public boolean isEdit() {
		return edit;
	}

	public void setEdit(boolean edit) {
		this.edit = edit;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getNewAuth() {
		return newAuth;
	}

	public void setNewAuth(String newAuth) {
		this.newAuth = newAuth;
	}

	public String getNewShort() {
		return newShort;
	}

	public void setNewShort(String newShort) {
		this.newShort = newShort;
	}

	public String getSelectedTourney() {
		return selectedTourney;
	}

	public void setSelectedTourney(String selectedTourney) {
		this.selectedTourney = selectedTourney;
	}

	public int getNumberInGame() {
		return numberInGame;
	}

	public void setNumberInGame(int numberInGame) {
		this.numberInGame = numberInGame;
	}

}
