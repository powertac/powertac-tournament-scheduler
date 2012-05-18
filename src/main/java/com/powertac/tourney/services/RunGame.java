package com.powertac.tourney.services;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimerTask;

import javax.faces.context.FacesContext;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Machines;
import com.powertac.tourney.beans.Scheduler;
import com.powertac.tourney.beans.Tournaments;

public class RunGame extends TimerTask {
	private int numAttempts;

	

	String logSuffix = "sim-";//boot-game-" + game.getGameId() + "-tourney-"+ game.getCompetitionName();
	String tourneyUrl = "";//game.getTournamentSchedulerUrl();
	String serverConfig = "";//game.getServerConfigUrl();
	String bootstrapUrl = "sim";//This needs to be empty for jenkins to run a bootstrapgame.getBootstrapUrl();
	String pomUrl = "";//game.getPomUrl();
	String gameId = "";//String.valueOf(game.getGameId());
	String brokers = "";
	String machineName = "";
	
	
	public RunGame(int gameId, String tourneyUrl, String pomUrl, String machineName, String destination){
		this.gameId = String.valueOf(gameId);
		this.tourneyUrl = tourneyUrl;
		this.pomUrl = pomUrl;
		
				
		//Assumes Jenkins and TS live in the same location as per the install
		this.serverConfig = "http://localhost:8080/TournamentScheduler/faces/properties.jsp?gameId="+this.gameId;
	}
	
	/***
	 * Make sure a bootstrap has been run for the sim
	 */
	private void checkBootstrap(){
		Database db = new Database();
		
		try {
			if(db.isGameReady(Integer.parseInt(gameId))){
				this.bootstrapUrl = "http://localhost:8080/TournamentScheduler/faces/pom.jsp?location="+gameId+"-boot.xml";
			}else{
				System.out.println("Game: " + gameId + " reports that bootstrap is not ready! retring in 15 seconds...");
				Thread.sleep(15000);
				this.run();
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	/***
	 * Make sure brokers are registered for the tournament
	 */
	private void checkBrokers(){
		Database db = new Database();
		
		
		
	}
	

	
	public void run() {
		checkBootstrap();
		checkBrokers();
		String finalUrl = "http://localhost:8080/jenkins/job/" 
				+ "start-server-instance/buildWithParameters?"
				+ "token=start-instance"
				+ "&tourneyUrl=" + tourneyUrl
				+ "&suffix=" + logSuffix 
				+ "&propUrl=" + serverConfig
				+ "&pomUrl=" + pomUrl 
				+ "&bootstrapUrl=" + bootstrapUrl
				+ "&brokers=" + brokers
				+ "&machine=" + machineName 
				+ "&gameId=" + gameId;
		
		
		try {
			URL url = new URL(finalUrl);
			URLConnection conn = url.openConnection();
			// Get the response
			InputStream input = conn.getInputStream();
			System.out.println("Jenkins request to start simulation game: "+this.gameId);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Jenkins failure to start simulation game: "+this.gameId);
		}
		
	}
	
}
