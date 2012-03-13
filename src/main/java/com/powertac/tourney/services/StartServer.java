package com.powertac.tourney.services;

import java.util.TimerTask;

import com.powertac.tourney.beans.Game;

public class StartServer extends TimerTask {
	private Game game;

	public StartServer(Game game){
		this.game = game;
	}
	
	public void run() {

	}

}
