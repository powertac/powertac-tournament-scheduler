package com.powertac.tourney.beans;

import java.util.Vector;

public class Tournaments {
	private static final String key = "tournaments";
	
	private Vector<Tournament> tournaments;
	
	public Tournaments(){
		tournaments = new Vector<Tournament>();
	}

	public static String getKey() {
		return key;
	}
	
	public void addTournament(Tournament t){
		this.tournaments.add(t);
	}
	
	public Tournament[] getTournamentList(){
		if(tournaments.size() == 0){
			return null;
		}else{
			Tournament[] newTourney = new Tournament[tournaments.size()];
			int i = 0;
			for(Tournament t : tournaments){
				newTourney[i] = t;
				i++;
			}
			
			return newTourney;
		}
	}
}
