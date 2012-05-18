package com.powertac.tourney.services;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.TimerTask;

import javax.faces.context.FacesContext;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Machines;
import com.powertac.tourney.beans.Scheduler;
import com.powertac.tourney.beans.Tournaments;

public class StartServer extends TimerTask {
	private Game game;
	private Machines allMachines;
	private Tournaments allTournaments;
	private int numAttempts;

	public StartServer(Game game, Machines allMachines, Tournaments allTournaments) {
		this.numAttempts = 0;
		this.game = game;
		this.allMachines = allMachines;
		this.allTournaments = allTournaments;
	}

	public void run() {

		if(this.numAttempts > 9){
			return;
		}

		int numRegistered = allTournaments.getTournamentById(game.getCompetitionId()).getNumberRegistered();

		// Check if a machine is free to start on
		if (allMachines.getFreeMachines().firstElement() != null
				&& numRegistered > 0) {

			//Set the jmsUrl
			//game.setJmsUrl("tcp://"+ allMachines.getFreeMachines().firstElement().getUrl()+":61616");
			
			// Issue rest call to jenkins, wait for server ready response in
			// serverInterface.jsp
			String logSuffix = "game-" + game.getGameId() + "-tourney-"
					+ game.getCompetitionName();
			String tourneyUrl = game.getTournamentSchedulerUrl();
			String serverConfig = game.getServerConfigUrl();
			String bootstrapUrl = game.getBootstrapUrl();
			String pomUrl = game.getPomUrl();
			String gameId = String.valueOf(game.getGameId());
			String brokers = "";
			for (String s : game.getBrokersToLogin().values()){
				brokers = brokers + s + " ";
			}
			
			

			String machineName = allMachines.getFreeMachines().firstElement()
					.getName();
			
			InetAddress thisIp = null;
			try {
				thisIp = InetAddress.getLocalHost();
				System.out.println("IP:"+thisIp.getHostAddress());
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			String finalUrl = "http://" + thisIp.getHostAddress() + ":8080/jenkins/job/" 
					+ "start-server-instance/buildWithParameters?"
					+ "token=start-instance&tourneyUrl=" + tourneyUrl
					+ "&suffix=" + logSuffix + "&serverConfig=" + serverConfig
					+ "&pomUrl=" + pomUrl + "&bootstrapUrl=" + bootstrapUrl
					+ "&machine=" + machineName + "&gameId=" + gameId
					+ "&brokers=" + brokers;

			try {
				URL url = new URL(finalUrl);
				URLConnection conn = url.openConnection();
				// Get the response
				InputStream input = conn.getInputStream();
				System.out.println("Jenkins request sent for : " + logSuffix);
				//game.setStatus("started");

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Jenkins failure");
			}
		} else {

			// No machines free for this game try again in a minute
			System.out.println("Retry server start for game: " + game.getCompetitionName() + ":" + game.getGameId());
			System.out.println("Number registered: " + numRegistered);
			
			try {
				Thread.sleep(60000);
				this.run();
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
