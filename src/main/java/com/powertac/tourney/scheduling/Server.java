package com.powertac.tourney.scheduling;

public class Server {
	
	private int ServerId; 
	private int ServerNumber;
	private String ServerName;	
	
	public String getServerName() {
		return ServerName;		
	}
	
	public int getServerId() {		
		return ServerId;
	}
	
	public void setServerId (int a){
		ServerId  = a;
	}
	public void setServerName (String a) {
		ServerName  = a;
	}
	public void setServerNumber(int a) {
		ServerNumber = a;
	}
	public int getServerNumber() {
		return ServerNumber;
	}
}
