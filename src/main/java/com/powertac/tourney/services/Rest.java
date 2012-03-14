package com.powertac.tourney.services;

import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;
import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Games;
import com.powertac.tourney.beans.Scheduler;
import com.powertac.tourney.constants.*;

public class Rest{
	public static String parseBrokerLogin(Map<?, ?> params){
		String responseType = ((String[]) params.get(Constants.REQ_PARAM_TYPE))[0];
		String brokerAuthToken = ((String[]) params.get(Constants.REQ_PARAM_AUTH_TOKEN))[0];
		String competitionName = ((String []) params.get(Constants.REQ_PARAM_JOIN))[0];
		
		String retryResponse;
		String loginResponse;
		String doneResponse;
		
		if(responseType.equalsIgnoreCase("xml")){
			retryResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><retry>%d</retry></message>";
			loginResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><login><jmsUrl>%s</jmsUrl><gameToken>%s</gameToken></login></message>";
			doneResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><done></done></message>";			
		}else{
			retryResponse = "{\n \"retry\":%d\n}";
			loginResponse = "{\n \"login\":%d\n \"jmsUrl\":%s\n \"gameToken\":%s\n}";
			doneResponse = "{\n \"done\":\"true\"\n}";
		}
		if(competitionName != null){
			for (Game g : Games.getAllGames().getGameList()){
				// Only consider games that have started and are ready for brokers to join
				if(g.getStartTime().before(new Date()) && g.getStatus().equalsIgnoreCase("ready")){
					//Anyone can start and join a test competition
					if(competitionName.equalsIgnoreCase("test")){
						// Spawn a new test competition and rerun
						Game game = new Game();
						game.setBootstrapUrl("http://www.cselabs.umn.edu/~onarh001/bootstraprun.xml");
						game.setCompetitionName("test");
						game.setMaxBrokers(1);
						game.setStartTime(new Date());
						game.setPomUrl("");
						game.setServerConfigUrl("");
						game.addBrokerLogin("anybroker", brokerAuthToken);
						Scheduler.getScheduler().schedule(new StartServer(game,FacesContext.getCurrentInstance()), new Date());						
						return String.format(retryResponse,5);
					}else if(competitionName.equalsIgnoreCase(g.getCompetitionName()) && g.isBrokerRegistered(brokerAuthToken)){
						// If a broker is registered and knows the competition name, give them an the jmsUrl and gameToken to login
						return String.format(loginResponse, g.getJmsUrl(),"1234");
					}
				}
				// If the game has yet to start and broker is registered send retry message
				if(g.isBrokerRegistered(brokerAuthToken)){
					return String.format(retryResponse, g.getStartTime().getTime()-(new Date()).getTime());
				}
				
			}
		}
		return doneResponse;
	}
	
	public static String parseServerInterface(Map<?, ?> params){
		if(params!=null){
			String actionString = ((String[]) params.get(Constants.REQ_PARAM_ACTION))[0];			
			String gameIdString = ((String[]) params.get(Constants.REQ_PARAM_GAME_ID))[0];
			String statusString = ((String[]) params.get(Constants.REQ_PARAM_STATUS))[0];
			
			if(actionString != null){
				if(actionString.equalsIgnoreCase("pom")){
					
				}else if (actionString.equalsIgnoreCase("config")){
					
				}else if (actionString.equalsIgnoreCase("bootstrap")){
					
				}else if (actionString.equalsIgnoreCase("status")){
					
				}else{
					return "Invalid action parameter";
				}
							
			}
			
			
			Integer gameId = Integer.parseInt(gameIdString);
			
			
			Games allGames = (Games) FacesContext.getCurrentInstance()
					.getExternalContext().getApplicationMap().get(Games.getKey());
			Game game = allGames.getGames().get(gameId);
			
			if(game!=null){
				game.setStatus(statusString);
				return String.format("Game ID: %d, Reports: %s", gameId, statusString);
				
			}else{
				return "Game doesn't exist!";
			}
			
		}
		
		return "Not yet implemented";		
	}
	
	

}
