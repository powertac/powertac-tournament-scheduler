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
	
	public void addGame(Game game){
		games.put(game.getGameId(), game);
	}
	
	public Game[] getGameList(){
		if(games.size() == 0){
			return null;
		}else{
			Game[] newGame = new Game[games.size()];
			int i = 0;
			for(Game t : games.values()){
				newGame[i] = t;
				i++;
			}
			
			return newGame;
		}
	}
	
	

}
