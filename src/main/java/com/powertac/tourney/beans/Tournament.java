package com.powertac.tourney.beans;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

// Technically not a managed bean, this is an internal Class to the 
// Tournaments bean which is an application scoped bean that acts as 
// a collection for all the active tournaments
public class Tournament {
	public enum TourneyType {
		SINGLE_GAME, MULTI_GAME;
	}
	private static int maxCompetitionId = 0;
	private int competitionId = 0;
	private Date startTime;
	private String tournamentName;
	private int maxBrokers; // -1 means inf, otherwise integer specific
	
	
	// Probably Should check name against auth token
	private HashMap<Integer,String> registeredBrokers;
	
	
	private String bootstrapUrl;
	private String pomUrl;

	private HashMap<Integer,Game> allGames;
	
	public Tournament(){
		System.out.println("Created Tournament Bean: " + competitionId);
		competitionId = maxCompetitionId;
		maxCompetitionId++;
		
		allGames = new HashMap<Integer,Game>();
		registeredBrokers = new HashMap<Integer,String>();
	}

	public int getCompetitionId() {
		return competitionId;
	}

	public void setCompetitionId(int competitionId) {
		this.competitionId = competitionId;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public String getTournamentName() {
		return tournamentName;
	}

	public void setTournamentName(String tournamentName) {
		this.tournamentName = tournamentName;
	}

	public int getMaxBrokers() {
		return maxBrokers;
	}

	public void setMaxBrokers(int maxBrokers) {
		this.maxBrokers = maxBrokers;
	}

	public String getBootstrapUrl() {
		return bootstrapUrl;
	}

	public void setBootstrapUrl(String bootstrapUrl) {
		this.bootstrapUrl = bootstrapUrl;
	}

	public String getPomUrl() {
		return pomUrl;
	}

	public void setPomUrl(String pomUrl) {
		this.pomUrl = pomUrl;
	}

	
	public void addGame(Game game){
		this.allGames.put(game.getGameId(), game);		
	}
	
	public String register(String name,int id, String authToken){
		//TODO: Fix this so that brokers are added to games according to the csp spec
		
		System.out.println("Registering broker: " + name);
		// Only open registration
		if(registeredBrokers.size() < maxBrokers && maxBrokers != -1){
			registeredBrokers.put(id, name);
			for(Game g : allGames.values()){
				if (g.getCompetitionId() == this.getCompetitionId()){
					g.addBrokerLogin(name, authToken);	
					System.out.println("Broker: " + name + ":" + authToken + " is registered for Competition:" + g.getCompetitionName());
				}
			}
			
			return "Success";
		}else{
			return "Failure";
		}
	}
	
	public boolean isRegistered(String authToken){
		return registeredBrokers.containsValue(authToken);
	}
	
	public int getNumberRegistered(){
		return registeredBrokers.size();
	}

}
