package com.powertac.tourney.beans;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler extends Timer {
	
	public Scheduler(){
		super();
	}
	
	public void addTask(Date time, TimerTask tt){
		this.schedule(tt, time);
	}

}
