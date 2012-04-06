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
	
	
	
	// Prepared Statements for Database access
	/***
	 * @param userName : User name attempting to login
	 * @param password : salted md5 hash of entered password
	 */
	public static final String LOGIN_USER = "SELECT * FROM users WHERE userName=? AND password=? LIMIT 1;";
	
	/***
	 * @param userName : User name to update account info   
	 */
	public static final String UPDATE_USER = "";
	
	/***
	 * @param userName : The desired username to use (this must be unique)
	 * @param password : The salted md5 hash of the password
	 * @param permissionId : The desired permission level 0=Admin 4=Guest (Recommend Guest)
	 */
	public static final String ADD_USER = "INSERT INTO tourney.users (userName, password, permissionId) VALUES (?,?,?); ";
	
	
	/***
	 * @param brokerName : The name of the Broker to use for logins
	 * @param brokerAuth : The md5 hash token to use for broker authorization
	 * @param brokerShort : The short description about the broker
	 */
	public static final String ADD_BROKER = "INSERT INTO tourney.brokers (brokerName,brokerAuth,brokerShort) VALUES (?,?,?);";
	
	/***
	 * Returns a list of brokers for a userId
	 * @param userId : The userId of the user you want to query for
	 */
	public static final String SELECT_BROKERS = "SELECT * FROM tourney.brokers WHERE userId=?";
	public static final String UPDATE_BROKER = "";
		
	public static final String SELECT_TOURNAMENT = "";
	public static final String UPDATE_TOURNAMENT = "";
	
	public static final String SELECT_GAME = "";
	public static final String UPDATE_GAME = "";
	
	

}
