package com.powertac.tourney.services;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CreateProperties {
	
	public static int genProperties(int gameId, List<String> locations, Date fromTime, Date toTime){
		Date starting = new Date();
		
		double randLocation = Math.random()*locations.size();
		String selectedLocation = locations.get((int) Math.floor(randLocation));
		
		// Number of msecs in a year divided by 4
		double gameLength = (3.1556926 * Math.pow(10,10))/4;
		
		// Max amount of time between the fromTime to the toTime to start a game
		long msLength = (long)gameLength;
		
		if(toTime.getTime()-fromTime.getTime()<msLength){
			// Use fromTime in all games in the tournament as the start time
			starting = fromTime;
		}else{
			long start = fromTime.getTime();
			long end = fromTime.getTime()-msLength;
			long startTime = (long)(Math.random()*(end-start)+start);
			
			
			starting.setTime(startTime);
		}
		
		Database db = new Database();
		java.sql.Date newDate = new java.sql.Date(starting.getTime());
		
		try {
			db.addProperties(gameId, selectedLocation, newDate.toString());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return 0;
	}
	
	public static List<String> getPropertiesForGameId(int gameId){
		List<String> result = new ArrayList<String>();
		Database db = new Database();
		
		try {
			result = db.getProperties(gameId);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return result;
	}

}
