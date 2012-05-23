package com.powertac.tourney.beans;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;

import org.springframework.stereotype.Service;

import com.powertac.tourney.services.Database;
import com.powertac.tourney.services.RunBootstrap;

@Service("scheduler")
public class Scheduler {
	
	
	
	public static final String key = "scheduler";
	public static boolean running = false;
	private Timer watchDogTimer = null;
	SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
	
	Properties props = new Properties();
	
	private HashMap<Integer,Timer> bootToBeRun = new HashMap<Integer,Timer>();
	private HashMap<Integer,Timer> simToBeRun = new HashMap<Integer,Timer>();
	
	public static String getKey(){
		return key;
	}
	
	@PreDestroy
	public void cleanUp() throws Exception {
		  System.out.println("[INFO] Spring Container is destroyed! Scheduler clean up");
		  if(watchDogTimer!=null){
			  watchDogTimer.cancel();
		  }
		  for(Timer t: bootToBeRun.values()){
			  if(t!=null){
				  t.cancel();
			  }
		  }
		  for(Timer t: simToBeRun.values()){
			  if(t!=null){
				  t.cancel();
			  }
		  }
		  
		  
	}
	
	public Scheduler(){
		try {
			props.load(Database.class.getClassLoader().getResourceAsStream(
					"/tournament.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.startWatchDog();
	}
	
	public void startWatchDog(){
		if(!running){
			Timer t = new Timer();
			TimerTask watchDog = new TimerTask(){
	
				@Override
				public void run() {
					System.out.println("[INFO] " + dateFormatUTC.format(new Date()) + " : WatchDogTimer Looking for Games To Start..");
					Database db = new Database();
					try {
						List<Game> games = db.getStartableGames();
						
						String hostip = "http://";
						
						try {
							InetAddress thisIp =InetAddress.getLocalHost();
							hostip += thisIp.getHostAddress() + ":8080";
						} catch (UnknownHostException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
						
						for(Game g : games){
							Tournament t = db.getTournamentByGameId(g.getGameId());
							System.out.println("[INFO] " + dateFormatUTC.format(new Date()) + " : Game: " + g.getGameId() + " will be started...");
							Scheduler.this.runSimTimer(g.getGameId(),new RunBootstrap(g.getGameId(), hostip+"/TournamentScheduler/", t.getPomUrl(), props.getProperty("destination")), new Date());
						}
						
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						this.cancel();
						e.printStackTrace();
					}
					
				}
				
			};
			
			System.out.println("[INFO] " + dateFormatUTC.format(new Date()) + " : Starting WatchDog...");
			running=true;
			t.schedule(watchDog, new Date(), 120000);
			
			this.watchDogTimer = t;
			
		}else{
			System.out.println("[WARN] Watchdog already running");
		}
	}
	
	public void restartWatchDog(){
		this.stopWatchDog();
		this.startWatchDog();
		
	}
	
	public void stopWatchDog(){
		if(watchDogTimer!=null){
			watchDogTimer.cancel();
			running=false;
			System.out.println("[INFO] " + dateFormatUTC.format(new Date()) + " : Stopping WatchDog...");
		}else{
			System.out.println("[WARN] " + dateFormatUTC.format(new Date()) + " : WatchDogTimer Already Stopped");
		}
	}
	
	
	public void runBootTimer(int gameId, TimerTask t, Date time){
		Timer timer = new Timer();
		timer.schedule(t, time);
		bootToBeRun.put(gameId, timer);
	}
	
	public void runSimTimer(int gameId, TimerTask t, Date time){
		Timer timer = new Timer();
		timer.schedule(t, time);
		simToBeRun.put(gameId, timer);
	}
	
	public void deleteSimTimer(int gameId){
		Timer t = simToBeRun.get(gameId);
		if (t!=null){
			t.cancel();
			simToBeRun.remove(gameId);
		}else{
			System.out.println("Timer thread is null for game: " + gameId);
		}
	}
	
	public void deleteBootTimer(int gameId){
		Timer t = bootToBeRun.get(gameId);
		if (t!=null){
			t.cancel();
			bootToBeRun.remove(gameId);
		}else{
			System.out.println("Timer thread is null for game: " + gameId);
		}
	}
	

}
