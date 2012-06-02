package org.powertac.tourney.scheduling;

import java.sql.ResultSet;

public class Server {
	
	private int ServerId; 
	private int ServerNumber;
	private String ServerName;	
	
	public Server(){
		
	}
	
	public Server(ResultSet rs){
		try{
			ServerName = rs.getString("ServerName");
			ServerNumber = rs.getInt("ServerNumber");
			ServerId = rs.getInt("ServerId");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
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
