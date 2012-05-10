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

public class RunGame extends TimerTask {
	private int numAttempts;

	public RunGame(Game game) {
		this.numAttempts = 0;
	}

	public void run() {

		// Stop the timer after some attempts to prevent memory leaks
		if (this.numAttempts > 15) {
			this.cancel();
			return;
		}

		// Check if a machine is free to start on
		int numRegistered = allTournaments.getTournamentById(game.getTournamentId()).getNumberRegistered();

		// Check if a machine is free to start on
		if (allMachines.getFreeMachines().firstElement() != null
				&& numRegistered > 0) {

			InetAddress thisIp = null;
			try {
				thisIp = InetAddress.getLocalHost();
				System.out.println("IP:" + thisIp.getHostAddress());
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			/*
			 * String finalUrl = "http://" + thisIp.getHostAddress() +
			 * ":8080/jenkins/job/" +
			 * "start-server-instance/buildWithParameters?" +
			 * "token=start-instance&tourneyUrl=" + tourneyUrl + "&suffix=" +
			 * logSuffix + "&serverConfig=" + serverConfig + "&pomUrl=" + pomUrl
			 * + "&bootstrapUrl=" + bootstrapUrl + "&machine=" + machineName +
			 * "&gameId=" + gameId + "&brokers=" + brokers;
			 */

			try {
				URL url = new URL(finalUrl);
				URLConnection conn = url.openConnection();
				// Get the response
				InputStream input = conn.getInputStream();
				System.out.println("Jenkins request sent for : " + logSuffix);
				// game.setStatus("started");

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Jenkins failure");
			}
		} else {

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
