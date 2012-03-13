package com.powertac.tourney.services;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.TimerTask;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Machines;
import com.powertac.tourney.beans.Scheduler;

public class StartServer extends TimerTask {
	private Game game;

	public StartServer(Game game) {
		this.game = game;
	}

	public void run() {

		// Check if a machine is free to start on
		if (Machines.getAllMachines().getFreeMachines().firstElement() != null) {

			// Issue rest call to jenkins, wait for server ready response in
			// serverInterface.jsp
			String logSuffix = "game-" + game.getGameId() + "tourney-"
					+ game.getCompetitionName();
			String tourneyUrl = game.getTournamentSchedulerUrl();
			String serverConfig = game.getServerConfigUrl();
			String bootstrapUrl = game.getBootstrapUrl();
			String pomUrl = game.getPomUrl();

			String machineName = Machines.getAllMachines().getFreeMachines()
					.firstElement().getName();

			String finalUrl = "http://tac04.cs.umn.edu:8080/job/"
					+ "start-server-instance/buildWithParameters?"
					+ "token=start-instance&tourneyUrl=" + tourneyUrl
					+ "&suffix=" + logSuffix + "&serverConfig=" + serverConfig
					+ "&pomUrl=" + pomUrl + "&bootstrapUrl=" + bootstrapUrl
					+ "&machine=" + machineName;

			try {
				URL url = new URL(finalUrl);
				URLConnection conn = url.openConnection();
				// Get the response
				InputStream input = conn.getInputStream();
				System.out.println("Jenkins request sent for : " + logSuffix);

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Jenkins failure");
			}
		} else {

			// No machines free for this game try again in a minute
			Scheduler.getScheduler().schedule(this, 60000);

		}

	}

}
