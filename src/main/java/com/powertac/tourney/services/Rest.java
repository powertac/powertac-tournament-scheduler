package com.powertac.tourney.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Games;
import com.powertac.tourney.constants.*;

public class Rest{
	public static String parseBrokerLogin(Map params){
		String response = "";
		String responseType = ((String[]) params.get(Constants.REQ_PARAM_TYPE))[0];
		String brokerAuthToken = ((String[]) params.get(Constants.REQ_PARAM_AUTH_TOKEN))[0];
		String competitionName = ((String []) params.get(Constants.REQ_PARAM_JOIN))[0];
		
		String retryResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><retry>%d</retry></message>";
		String loginResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><login><jmsUrl>%s</jmsUrl><gameToken>%s</gameToken></login></message>";
		String doneResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><done></done></message>";
		if(competitionName != null){
			
			// Anyone can log into the test competition
			if(competitionName.equalsIgnoreCase("test")){
				//List<String> brokers = new ArrayList<String>();

				
				// TODO: Lookup broker name from brokerAuthToken in DB
				
				if(Jenkins.startTestGame(0)){
					Games allGames = (Games) FacesContext.getCurrentInstance()
							.getExternalContext().getApplicationMap().get(Games.getKey());
					Game game = allGames.getGames().get(0);
					
					//MessageDigest md5 = null;
					//try {
					//	md5 = MessageDigest.getInstance("MD5");
					//} catch (NoSuchAlgorithmException e) {
					//	// TODO Auto-generated catch block
					//	e.printStackTrace();
					//}
					//byte[] hash = md5.digest();
					//String gameToken = hash.toString();
					
					//game.getBrokersToLogin().put("Sample-broker", gameToken);
					
					return String.format(loginResponse, game.getJmsUrl(),"1234");
				}else{
					return String.format(retryResponse,5);
				}	
			}else{
				// Normal Competition
				
			}
			
		}
		
		/*
		// Check if a response type was specified, default is xml
		if(responseType != null){
			if(responseType.equalsIgnoreCase("xml")){
				
				
				response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message><retry>5</retry></message>";
			}else if(responseType.equalsIgnoreCase("json")){
				
				
				response = "{\n \"retry\":5\n}";				
			}else{
				response = "Error making rest call, please check your parameters:"+responseType;
			}
			
			
		// Default xml
		}else{
			responseType = "xml";
			
			
		}*/
		
		
		return response;
	}
	
	public static String parseServerInterface(Map params){
		if(params!=null){
			String actionString = ((String[]) params.get(Constants.REQ_PARAM_ACTION))[0];			
			String gameIdString = ((String[]) params.get(Constants.REQ_PARAM_GAME_ID))[0];
			String statusString = ((String[]) params.get(Constants.REQ_PARAM_STATUS))[0];
			
			if(actionString != null){
				if(actionString.compareToIgnoreCase("pom")==0){
					
				}else if (actionString.compareToIgnoreCase("config")==0){
					
				}else if (actionString.compareToIgnoreCase("bootstrap")==0){
					
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
	
	public void respond() {

		String queryString = ((HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest())
				.getQueryString();

		queryString = "stuff";
		if (queryString != null) {
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext ext = context.getExternalContext();
			HttpServletResponse response = (HttpServletResponse) ext
					.getResponse();
			response.setContentType("text/plain; charset=UTF-8");
			try {
				PrintWriter pw = response.getWriter();
				pw.print(queryString);
			} catch (IOException ex) {
				throw new FacesException(ex);
			}
			context.responseComplete();
		}

	}

}
