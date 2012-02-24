package com.powertac.tourney.beans;

import java.util.HashMap;

public class Games {
	HashMap<Integer,Game> games = null;
	
	public static final String key = "games";
	
	private static int gameId = 0;
	
	public Games(){
		this.games = new HashMap<Integer,Game>();
	}
	
	public static String getKey(){
		return key;
	}
	
	public HashMap<Integer,Game> getGames(){
		return this.games;
	}
	
	public static int getNewGameId(){
		return gameId++;
	}
	
	

}
