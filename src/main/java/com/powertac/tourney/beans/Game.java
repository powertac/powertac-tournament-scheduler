package com.powertac.tourney.beans;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Game {
	private String competitionName = "";
	private Date startTime;
	private int competitionId = -1;
	private int gameId = 0;
	private static int maxGameId = 0;
	private String status = "pending";
	private String jmsUrl = "";
	private HashMap<String, String> brokersToLogin = null;
	private String[] brokersLoggedIn = null;
	private int maxBrokers = 1;

	public static final String key = "game";
	
	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Game(){
		System.out.println("Created Tournament Bean: " + competitionId);
		competitionId = maxGameId;
		maxGameId++;
		
	}
	
	public HashMap<String, String> getBrokersToLogin() {
		return brokersToLogin;
	}

	public void setBrokersToLogin(HashMap<String, String> brokersToLogin) {
		this.brokersToLogin = brokersToLogin;
	}

	public static String getKey() {
		return key;
	}

	public String getCompetitionName() {
		return competitionName;
	}

	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}

	public int getCompetitionId() {
		return competitionId;
	}

	public void setCompetitionId(int competitionId) {
		this.competitionId = competitionId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getJmsUrl() {
		return jmsUrl;
	}

	public void setJmsUrl(String jmsUrl) {
		this.jmsUrl = jmsUrl;
	}

	public String[] getBrokersLoggedIn() {
		return brokersLoggedIn;
	}

	public int getMaxBrokers() {
		return maxBrokers;
	}

	public void setMaxBrokers(int maxBrokers) {
		this.maxBrokers = maxBrokers;
	}

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}
	
	

	public boolean setupGame() {
		// Read database for this game id and populate brokersToLogin with
		// name->gameToken pairs
		//if(competitionName.compareToIgnoreCase("test")){
			
		//}
		
		return false;

	}

	public boolean authorizeBroker(String brokerName, String gameToken) {
		if (getBrokersToLogin() != null
				&& getBrokersToLogin().get(brokerName) == gameToken) {

			// Send http response indicating success
			return true;
		} else {
			// Send http response back that authorization has failed
			return false;
		}

	}

}
