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
	public static final String LOGIN_SALT = "SELECT password, salt, permissionId, userId FROM users WHERE userName=?;";
	/***
	 * @param userName : User name to update account info   
	 */
	public static final String UPDATE_USER = "";
	
	/***
	 * @param userName : The desired username to use (this must be unique)
	 * @param password : The salted md5 hash of the password
	 * @param permissionId : The desired permission level 0=Admin 4=Guest (Recommend Guest)
	 */
	public static final String ADD_USER = "INSERT INTO tourney.users (userName, salt, password, permissionId) VALUES (?,?,?,?); ";
	
	
	/***
	 * Select all users
	 */
	public static final String SELECT_USERS = "SELECT * FROM tourney.users;";
	
	/***
	 * @param brokerName : The name of the Broker to use for logins
	 * @param brokerAuth : The md5 hash token to use for broker authorization
	 * @param brokerShort : The short description about the broker
	 * @param userId : The userId of the user that owns this broker
	 */
	public static final String ADD_BROKER = "INSERT INTO tourney.brokers (brokerName,brokerAuth,brokerShort, userId, numberInGame) VALUES (?,?,?,?,0);";
	
	/***
	 * Select all brokers by their userId
	 * @param userId : The userId of the brokers to return
	 */
	public static final String SELECT_BROKERS_BY_USERID = "SELECT * FROM tourney.brokers WHERE userID = ?;";
	
	/***
	 * Select broker by their brokerId
	 * @param brokerId : The brokerId of the broker you wish to return
	 */
	public static final String SELECT_BROKER_BY_BROKERID = "SELECT * FROM tourney.brokers WHERE brokerId = ? LIMIT 1;";
	
	/**
	 * Delete a broker by their brokerId
	 * @param brokerId : The brokerId of the broker you wish to delete 
	 * 
	 */
	public static final String DELETE_BROKER_BY_BROKERID = "DELETE FROM tourney.brokers WHERE brokerId = ?;";
	
	/**
	 * Update a broker by their brokerId
	 * 
	 * @param brokerName 
	 * @param brokerAuth
	 * @param brokerShort
	 * @param brokerID : The brokerId of the broker you wish to update
	 */
	public static final String UPDATE_BROKER_BY_BROKERID = "UPDATE tourney.brokers SET brokerName = ?, brokerAuth = ?, brokerShort = ? WHERE brokerId = ?;";
	
	
	
	
	/***
	 * Returns a list of brokers for a userId
	 * @param userId : The userId of the user you want to query for
	 */
	public static final String SELECT_BROKER = "SELECT * FROM tourney.brokers WHERE brokerId=?;";
	public static final String UPDATE_BROKER = "";
		
	/***
	 * Returns the list of all tournaments in the database of a particular status (pending, in-progress, complete) possible
	 * @param status : either "pending", "in-progress", or "complete" 
	 */
	public static final String SELECT_TOURNAMENTS = "SELECT * FROM tourney.tournaments WHERE status=?";
	public static final String SELECT_TOURNAMENT_BYID = "SELECT * FROM tourney.tournaments WHERE tournamentId=?;";
	public static final String UPDATE_TOURNAMENT_BYID = "";
	
	public static final String SELECT_GAME = "";
	public static final String UPDATE_GAME = "";
	
	

}
