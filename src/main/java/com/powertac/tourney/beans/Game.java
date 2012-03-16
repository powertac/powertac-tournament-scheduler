package com.powertac.tourney.beans;

import java.util.Date;
import java.util.HashMap;

public class Game {
	private String competitionName = "";
	private Date startTime;
	private int competitionId = -1;
	private int gameId = 0;
	private static int maxGameId = 0;
	private String status = "pending";
	private Machine runningMachine = null; // This is set when the game is actually running on a machine
	
	private String jmsUrl = "";
	private String serverConfigUrl = "";
	private String tournamentSchedulerUrl = "";
	private String bootstrapUrl = "";
	private String pomUrl = "";
	
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
		System.out.println("Created Game Bean: " + gameId);
		gameId = maxGameId;
		maxGameId++;
		
		brokersToLogin = new HashMap<String,String>();
		
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

	public String getPomUrl() {
		return pomUrl;
	}

	public void setPomUrl(String pomUrl) {
		this.pomUrl = pomUrl;
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
	
	public void addBrokerLogin(String brokerName, String authToken){
		brokersToLogin.put(authToken, brokerName);
	}
	
	public void addGameLogin(String gameToken){
		
	}
	
	public boolean isGameTokenValid(String gameToken){
		return true;
	}
	
	
	public boolean isBrokerRegistered(String authToken){
		return brokersToLogin.containsKey(authToken);
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

	public String getTournamentSchedulerUrl() {
		return tournamentSchedulerUrl;
	}

	public void setTournamentSchedulerUrl(String tournamentSchedulerUrl) {
		this.tournamentSchedulerUrl = tournamentSchedulerUrl;
	}

	public String getServerConfigUrl() {
		return serverConfigUrl;
	}

	public void setServerConfigUrl(String serverConfigUrl) {
		this.serverConfigUrl = serverConfigUrl;
	}

	public String getBootstrapUrl() {
		return bootstrapUrl;
	}

	public void setBootstrapUrl(String bootstrapUrl) {
		this.bootstrapUrl = bootstrapUrl;
	}

}
