package com.powertac.tourney.actions;

import java.util.Date;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Games;
import com.powertac.tourney.beans.Machines;
import com.powertac.tourney.beans.Scheduler;
import com.powertac.tourney.beans.Tournament;
import com.powertac.tourney.beans.Tournaments;
import com.powertac.tourney.services.StartServer;

public class ActionTournament {

	public enum TourneyType {
		SINGLE_GAME,
		MULTI_GAME;
	}
	
	private Date startTime = new Date(); // Default to current date/time
	private String tournamentName;
	private int maxBrokers; // -1 means inf, otherwise integer specific
	
	
	
	private TourneyType type = TourneyType.SINGLE_GAME;
	
	// Method to list the type enumeration in the jsf select Item component
	public SelectItem[] getTypes(){
		SelectItem[] items = new SelectItem[TourneyType.values().length];
	    int i = 0;
	    for(TourneyType t: TourneyType.values()) {
	      items[i++] = new SelectItem(t, t.name());
	    }
	    return items;
	}

	public TourneyType getType() {
		return type;
	}

	public void setType(TourneyType type) {
		this.type = type;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public int getMaxBrokers() {
		return maxBrokers;
	}

	public void setMaxBrokers(int maxBrokers) {
		this.maxBrokers = maxBrokers;
	}

	public String getTournamentName() {
		return tournamentName;
	}

	public void setTournamentName(String tournamentName) {
		this.tournamentName = tournamentName;
	}

	public String createTournament() {
		// Create a tournament and insert it into the application context
		Tournament newTourney = new Tournament();
		if(type == TourneyType.SINGLE_GAME){
			// TODO: Change this to the correct hosted files
			newTourney.setBootstrapUrl("http://www-users.cselabs.umn.edu/~onarh001/bootstraprun.xml");
			newTourney.setPomUrl("default");
			newTourney.setMaxBrokers(getMaxBrokers());
			newTourney.setStartTime(getStartTime());
			newTourney.setTournamentName(getTournamentName());
			
			// Add one game to the global context and to the tournament
			Game newGame = new Game();
			newGame.setMaxBrokers(getMaxBrokers());
			newGame.setCompetitionId(newTourney.getCompetitionId());
			newGame.setCompetitionName(getTournamentName());
			newGame.setStatus("pending");
			newGame.setStartTime(getStartTime());
			
			Games allGames = (Games) FacesContext.getCurrentInstance()
					.getExternalContext().getApplicationMap().get(Games.getKey());
			
			// Add game to all games and to Tournament
			allGames.addGame(newGame);
			newTourney.addGame(newGame);
			
			Tournaments.getAllTournaments().addTournament(newTourney);
			
			// Start a single game and send jenkins request to kick the server at the appropriate time
			Scheduler.getScheduler().schedule(new StartServer(newGame, Machines.getAllMachines(), Tournaments.getAllTournaments()), newGame.getStartTime());
			
		}else if(type == TourneyType.MULTI_GAME){
			
		}else{
			
		}
		
		
		
		//Tournaments allTournaments = (Tournaments) FacesContext.getCurrentInstance()
		//		.getExternalContext().getApplicationMap().get(Tournaments.getKey());
		
		//allTournaments.addTournament(newTourney);
		
		return "Success";
		
		
	}

}
