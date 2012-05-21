package com.powertac.tourney.beans;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.faces.context.FacesContext;

import org.springframework.stereotype.Service;

@Service("scheduler")
public class Scheduler extends Timer {
	
	private Vector<TimerTask> tasks;
	
	public static final String key = "scheduler";
	
	private HashMap<Integer,TimerTask> bootToBeRun = new HashMap<Integer,TimerTask>();
	private HashMap<Integer,TimerTask> simToBeRun = new HashMap<Integer,TimerTask>();
	
	public static String getKey(){
		return key;
	}
	
	public Scheduler(){
		super(false);
		tasks = new Vector<TimerTask>();
		System.out.println("Checking tasks at: " + new Date());
		//this.checkTask();
	}
	
	public void runBootTimer(int gameId, TimerTask t, Date time){
		bootToBeRun.put(gameId, t);
		this.schedule(t, time);
	}
	
	public void runSimTimer(int gameId, TimerTask t, Date time){
		simToBeRun.put(gameId, t);
		this.schedule(t, time);
	}
	
	public void deleteSimTimer(int gameId){
		TimerTask t = simToBeRun.get(gameId);
		if (t!=null){
			t.cancel();
			simToBeRun.remove(gameId);
		}else{
			System.out.println("Timer thread is null for game: " + gameId);
		}
	}
	
	public void deleteBootTimer(int gameId){
		TimerTask t = bootToBeRun.get(gameId);
		if (t!=null){
			t.cancel();
			bootToBeRun.remove(gameId);
		}else{
			System.out.println("Timer thread is null for game: " + gameId);
		}
	}
	
	public static Scheduler getScheduler(){
		return (Scheduler) FacesContext.getCurrentInstance()
				.getExternalContext().getApplicationMap().get(Scheduler.getKey());
	}
	
	public void addTask(long delay, TimerTask tt){
		this.schedule(tt, delay);
	}
	
	public void addPermanentTask(TimerTask tt){
		tasks.add(tt);
	}
	
	public void checkTask(){
		Calendar newTime = Calendar.getInstance();
		newTime.set(Calendar.MINUTE, newTime.get(Calendar.MINUTE) + 1);
		
		TimerTask tmp = new TimerTask() {
			public void run() {
				System.out.println("Checking tasks at: " + new Date());
				//for(TimerTask tt : tasks){
				//	addTask(newTime, tt);
				//	tasks.remove(tt);					
				//}
				
				checkTask();
			}
		};
		this.addTask(60000, tmp);
		
	}

}
