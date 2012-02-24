package com.powertac.tourney.services;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Games;

public class Jenkins {

	// Initiates the game start process if hasnt happened yet, returns true or
	// false if ready or not
	public static boolean startTestGame(Integer gameId) {

		Games allGames = (Games) FacesContext.getCurrentInstance()
				.getExternalContext().getApplicationMap().get(Games.getKey());
		Game maybeGame = allGames.getGames().get(gameId);

		if (maybeGame == null) {

			// TODO: Create database entry

			// Create game bean and insert it into application context if it has
			// not been started or is not pending
			Game newGame = new Game();
			newGame.setGameId(gameId); // Lookup the proper gameId from the
										// database,
										// probably not 0
			newGame.setCompetitionName("test");
			newGame.setMaxBrokers(1);
			newGame.setStatus("pending");
			newGame.setupGame();// Populates internal map with broker <-> token
								// pairs for this game

			// Games allGames = (Games) FacesContext.getCurrentInstance()
			// .getExternalContext().getSessionMap().get(Games.getKey());

			HashMap<Integer, Game> hm = allGames.getGames();

			hm.put(newGame.getGameId(), newGame);

			// TODO: Select a machine to start server on

			// Issue rest call to jenkins, wait for server ready response in
			// serverInterface.jsp
			String finalUrl = "http://tac04.cs.umn.edu:8080/job/"+
							  "start-server-instance/buildWithParameters?"+
					          "token=start-instance&tourneyUrl=url&suffix=test&"+
							  "serverConfig=test&machine=test&pomUrl=test";

			try {
				URL url = new URL(finalUrl);
				URLConnection conn = url.openConnection();
				// Get the response
				InputStream input = conn.getInputStream();
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Update game bean and database entry if serverInterface
			// receivesUpdate

			// Game is definitely not ready to run return false
			return false;
		} else if (maybeGame.getStatus().equalsIgnoreCase("ready")) {
			maybeGame.setJmsUrl("tcp://tac04.cs.umn.edu:61616");
			// Server has sent the ready response, ready to join return true
			return true;
		}

		return false;
	}

	public static boolean startGame(String machineName) {
		return false;

	}

	private boolean startServer(String machineName) {
		return false;

	}

}
