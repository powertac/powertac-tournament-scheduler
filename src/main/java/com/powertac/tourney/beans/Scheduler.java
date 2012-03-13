package com.powertac.tourney.beans;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.faces.context.FacesContext;

public class Scheduler extends Timer {
	
	private Vector<TimerTask> tasks;
	
	public static final String key = "scheduler";
	
	
	public static String getKey(){
		return key;
	}
	
	public Scheduler(){
		super(false);
		tasks = new Vector<TimerTask>();
		System.out.println("Checking tasks at: " + new Date());
		//this.checkTask();
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
