package com.powertac.tourney.services;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.TimerTask;

import org.springframework.stereotype.Service;

public class RunBootstrap extends TimerTask{
	
	String logSuffix = "boot-";//boot-game-" + game.getGameId() + "-tourney-"+ game.getCompetitionName();
	String tourneyUrl = "";//game.getTournamentSchedulerUrl();
	String serverConfig = "";//game.getServerConfigUrl();
	String bootstrapUrl = "";//This needs to be empty for jenkins to run a bootstrapgame.getBootstrapUrl();
	String pomUrl = "";//game.getPomUrl();
	String gameId = "";//String.valueOf(game.getGameId());
	String brokers = "";
	String machineName = "";
	String destination = "";
	
	
	public RunBootstrap(int gameId, String tourneyUrl, String pomUrl, String machineName, String destination){
		this.gameId = String.valueOf(gameId);
		this.tourneyUrl = tourneyUrl;
		this.pomUrl = pomUrl;
		this.destination = destination;
		
		//Assumes Jenkins and TS live in the same location as per the install
		this.serverConfig = "http://localhost:8080/TournamentScheduler/faces/properties.jsp?gameId="+this.gameId;
	}
	

	
	public void run() {
		String finalUrl = "http://localhost:8080/jenkins/job/" 
				+ "start-server-instance/buildWithParameters?"
				+ "token=start-instance"
				+ "&tourneyUrl=" + tourneyUrl
				+ "&suffix=" + logSuffix 
				+ "&propUrl=" + serverConfig
				+ "&pomUrl=" + pomUrl 
				+ "&bootstrapUrl=" + bootstrapUrl
				+ "&machine=" + machineName 
				+ "&gameId=" + gameId
				+ "&destination=" + destination;
		
		
		try {
			URL url = new URL(finalUrl);
			URLConnection conn = url.openConnection();
			// Get the response
			InputStream input = conn.getInputStream();
			System.out.println("Jenkins request to bootstrap game: "+this.gameId);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Jenkins failure to bootstrap game: "+this.gameId);
		}
		
	}

}
