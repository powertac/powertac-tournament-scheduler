package com.powertac.tourney.beans;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import javax.faces.bean.ManagedBean;

import com.powertac.tourney.services.Database;

@ManagedBean
public class Game {
	private String competitionName = "";
	private Date startTime;
	private int competitionId = -1;
	private int tourneyId = 0;
	private int gameId = 0;
	private int machineId;
	private static int maxGameId = 0;
	private String status = "pending";
	private Machine runningMachine = null; // This is set when the game is actually running on a machine
	private boolean hasBootstrap = false;
	private String brokers = "";
	private HashMap<String, String> brokerAuth = new HashMap<String,String>();
	
	
	
	
	private String gameName = "";
	private String location = "";
	private String jmsUrl = "";
	private String serverConfigUrl = "";
	private String tournamentSchedulerUrl = "";
	private String bootstrapUrl = "";
	private String propertiesUrl = "";
	private String visualizerUrl = "";
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
		//gameId = maxGameId;
		//maxGameId++;
		
		brokersToLogin = new HashMap<String,String>();
		
	}
	
	public void addBroker(int brokerId){
		Database db = new Database();
		Broker b = new Broker("new");
		try {
			b = db.getBroker(brokerId);
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		brokers += b.getBrokerName() + ", ";
		this.brokerAuth.put(b.getBrokerAuthToken(), b.getBrokerName());
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
	
	//public void addBrokerLogin(String brokerName, String authToken){
	//	brokersToLogin.put(authToken, brokerName);
	//}
	
	public void addGameLogin(String gameToken){
		
	}
	
	public boolean isGameTokenValid(String gameToken){
		return true;
	}
	
	
	public boolean isBrokerRegistered(String authToken){
		return brokerAuth.containsKey(authToken);
	}
	

	/*public boolean authorizeBroker(String brokerName, String gameToken) {
		if (getBrokersToLogin() != null
				&& getBrokersToLogin().get(brokerName) == gameToken) {

			// Send http response indicating success
			return true;
		} else {
			// Send http response back that authorization has failed
			return false;
		}

	}*/

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

	public boolean isHasBootstrp() {
		return hasBootstrap;
	}

	public void setHasBootstrap(boolean hasBootstrap) {
		this.hasBootstrap = hasBootstrap;
	}

	public String getGameName() {
		return gameName;
	}

	public void setGameName(String gameName) {
		this.gameName = gameName;
	}

	public String getPropertiesUrl() {
		return propertiesUrl;
	}

	public void setPropertiesUrl(String propertiesUrl) {
		this.propertiesUrl = propertiesUrl;
	}

	public String getVisualizerUrl() {
		return visualizerUrl;
	}

	public void setVisualizerUrl(String visualizerUrl) {
		this.visualizerUrl = visualizerUrl;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getTourneyId() {
		return tourneyId;
	}

	public void setTourneyId(int tourneyId) {
		this.tourneyId = tourneyId;
	}

	public String getBrokers() {
		return brokers;
	}

	public void setBrokers(String brokers) {
		this.brokers = brokers;
	}

	public int getMachineId() {
		return machineId;
	}

	public void setMachineId(int machineId) {
		this.machineId = machineId;
	}

}
