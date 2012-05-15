package com.powertac.tourney.beans;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.faces.bean.ManagedBean;

// Technically not a managed bean, this is an internal Class to the 
// Tournaments bean which is an application scoped bean that acts as 
// a collection for all the active tournaments
@ManagedBean
public class Tournament {
	public enum TourneyType {
		SINGLE_GAME, MULTI_GAME;
	}
	private static int maxTournamentId = 0;
	private int tournyId = 0;
	private Date startTime;
	private String tournamentName;
	private int maxBrokers; // -1 means inf, otherwise integer specific
	
	private String pomName;
	
	
	// Probably Should check name against auth token
	private HashMap<Integer,String> registeredBrokers;

	private String pomUrl;

	private HashMap<Integer,Game> allGames;
	
	public Tournament(){
		System.out.println("Created Tournament Bean: " + tournyId);
		//tournyId = maxTournamentId;
		//maxTournamentId++;
		
		allGames = new HashMap<Integer,Game>();
		registeredBrokers = new HashMap<Integer,String>();
	}

	public int getTournamentId() {
		return tournyId;
	}

	public void setTournamentId(int competitionId) {
		this.tournyId = competitionId;
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
				if (g.getCompetitionId() == this.getTournamentId()){
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

	/**
	 * @return the pomName
	 */
	public String getPomName() {
		return pomName;
	}

	/**
	 * @param pomName the pomName to set
	 */
	public void setPomName(String pomName) {
		this.pomName = pomName;
	}



}
