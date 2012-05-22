package com.powertac.tourney.beans;

import javax.faces.bean.ManagedBean;


@ManagedBean
public class Machine {
	private String name;
	private String url;
	private boolean available;
	private boolean inProgress;
	private String status;
	private int machineId;
	private int gameId;
	private String vizUrl;
	private String vizQueue;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public boolean isInProgress() {
		return inProgress;
	}
	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}
	public int getGameId() {
		return gameId;
	}
	public void setGameId(int gameId) {
		this.gameId = gameId;
	}
	public int getMachineId() {
		return machineId;
	}
	public void setMachineId(int machineId) {
		this.machineId = machineId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
		if(status.equalsIgnoreCase("idle")){
			this.inProgress = false;
		}else if(status.equalsIgnoreCase("running")){
			this.inProgress = true;
		}
	}
	public boolean isAvailable() {
		return available;
	}
	public void setAvailable(boolean available) {
		this.available = available;
	}
	public String getVizUrl() {
		return vizUrl;
	}
	public void setVizUrl(String vizUrl) {
		this.vizUrl = vizUrl;
	}
	public String getVizQueue() {
		return vizQueue;
	}
	public void setVizQueue(String vizQueue) {
		this.vizQueue = vizQueue;
	}
}
