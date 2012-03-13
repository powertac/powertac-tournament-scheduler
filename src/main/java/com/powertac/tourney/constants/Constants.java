package com.powertac.tourney.constants;

public class Constants {
	
	
	// Possible Rest Parameters for Broker Login
	public static final String REQ_PARAM_AUTH_TOKEN = "authToken";
	public static final String REQ_PARAM_JOIN = "requestJoin";
	public static final String REQ_PARAM_TYPE = "type";
	
	
	// Possible Rest Paramenters for Server Interface
	public static final String REQ_PARAM_STATUS = "status";
	public static final String REQ_PARAM_MACHINE = "machine";
	public static final String REQ_PARAM_GAME_ID = "gameId";
	// action=config - returns server.properties file
	// action=pom - returns the pom.xml file
	// action=bootstrap - returns the bootstrap.xml file
	public static final String REQ_PARAM_ACTION = "action";
	

}
