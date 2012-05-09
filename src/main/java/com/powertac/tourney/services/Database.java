package com.powertac.tourney.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.faces.context.FacesContext;

import com.powertac.tourney.actions.ActionAdmin;
import com.powertac.tourney.beans.Broker;
import com.powertac.tourney.constants.Constants;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;




@Component("database")
@Scope("request")
public class Database {
	// Database User container
	public class User{
		private String username;
		private String password;
		private int permission;
		private String salt;
		private String id;
		private String info;
		
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getSalt() {
			return salt;
		}
		public void setSalt(String salt) {
			this.salt = salt;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getInfo() {
			return info;
		}
		public void setInfo(String info) {
			this.info = info;
		}
		public int getPermission() {
			return permission;
		}
		public void setPermission(int permission) {
			this.permission = permission;
		}
	}
	
	
	
	// Connection Related
	private String dbUrl = "";
	private String database = "";
	private String port = "";
	private String username = "";
	private String password = "";
	private String dbms = "";

	// Database Configurations
	private Connection conn = null;
	private PreparedStatement loginStatement = null;
	private PreparedStatement saltStatement = null;
	private PreparedStatement addUserStatement = null;
	private PreparedStatement selectUsersStatement = null;
	private PreparedStatement addBrokerStatement = null;
	private PreparedStatement selectBrokersByUserId = null;
	private PreparedStatement selectBrokerByBrokerId = null;
	private PreparedStatement updateBrokerById = null;
	private PreparedStatement deleteBrokerById = null;
	private PreparedStatement selectPropsById = null;
	private PreparedStatement addPropsById = null;
	private PreparedStatement addPom = null;
	private PreparedStatement selectPoms = null;
	
	Properties connectionProps = new Properties();
	Properties prop = new Properties();

	public Database() {
		
		try {
			prop.load(Database.class.getClassLoader().getResourceAsStream(
					"/tournament.properties"));
			System.out.println(prop);
			// Database Connection related properties
			this.setDatabase(prop.getProperty("db.database"));
			this.setDbms(prop.getProperty("db.dbms"));
			this.setPort(prop.getProperty("db.port"));
			this.setDbUrl(prop.getProperty("db.dbUrl"));
			this.setUsername(prop.getProperty("db.username"));
			this.setPassword(prop.getProperty("db.password"));

			//System.out.println("Successfully instantiated Database bean!");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// TODO: Strategy Object find the correct dbms by reflection and call its connection method
	private void checkDb() {
		try {
			if (conn == null || conn.isClosed()) {
				System.out.println("Connection is null");
				if (this.dbms.equalsIgnoreCase("mysql")) {
					System.out.println("Using mysql as dbms ...");
					try {
						connectionProps.setProperty("user", this.username);
						connectionProps.setProperty("password", this.password);
						Class.forName ("com.mysql.jdbc.Driver").newInstance();
						conn = DriverManager.getConnection("jdbc:" + this.dbms
								+ "://" + this.dbUrl +  "/" + this.database,
								connectionProps);
						System.out.println("Connected Successfully");
					} catch (Exception e) {
						System.out.println("Connection Error");
						e.printStackTrace();
					}
				}else{
					System.out.println("DBMS: " + this.dbms + " is not supported");
				}
			}else{
				System.out.println("Connection is good");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Connection established correctly");
	}
	
	public List<User> getAllUsers() throws SQLException{
		checkDb();
		List<User> users = new ArrayList<User>();
		
		if(selectUsersStatement == null || selectUsersStatement.isClosed()){
			selectUsersStatement = conn.prepareStatement(Constants.SELECT_USERS);
		}
		
		ResultSet rsUsers = selectUsersStatement.executeQuery();
		while(rsUsers.next()){
			User tmp = new User();
			tmp.setUsername(rsUsers.getString("userName"));
			tmp.setPassword(rsUsers.getString("password"));
			tmp.setId(rsUsers.getString("userId"));
			tmp.setPermission(rsUsers.getInt("permissionId"));
			users.add(tmp);
			
		}
		
		
		
		return users;
	}
	
	public int updateUser(String username, String key, String value){
		return 0;
		
	}
	
	
	
	public int addUser(String username, String password) throws SQLException{
		checkDb();
		
		if(addUserStatement == null || addUserStatement.isClosed()){
			addUserStatement = conn.prepareStatement(Constants.ADD_USER);
			// Use a pool of entropy to secure salts
			String genSalt = DigestUtils.md5Hex(Math.random() + (new Date()).toString());
			addUserStatement.setString(1,username);
			addUserStatement.setString(2,genSalt);
			addUserStatement.setString(3,DigestUtils.md5Hex(password+genSalt)); // Store hashed and salted passwords
			addUserStatement.setInt(4,3); //Lowest permission level for logged in user is 3
			
		}
		
		
		return addUserStatement.executeUpdate();
	}
	
	public int[] loginUser(String username, String password) throws SQLException{
		checkDb();
		
		boolean userExist = false;
		if (saltStatement == null || saltStatement.isClosed()){
			saltStatement = conn.prepareStatement(Constants.LOGIN_SALT);
			saltStatement.setString(1, username);
		}
		
		ResultSet rsSalt = saltStatement.executeQuery();
		// salt and hash password
		String salt = "";
		String digest = "";
		int userId = -1;
		int permission = 99; // Lowest permission level
		String hashedPass = "";// DigestUtils.md5Hex(password	+ salt);
		if(rsSalt.next()){
			digest = rsSalt.getString("password");
            salt = rsSalt.getString("salt");
            permission = rsSalt.getInt("permissionId");
            userId = rsSalt.getInt("userId");
            userExist = true;
		}else{ // Time resistant attack we need to hash something
			digest = "000000000000000000000000000=";
            salt = "00000000000=";
            userExist = false;
		}
		
			
		
		
		conn.close();
		// TODO: make sure things are inserted correctly in the database;
		if (DigestUtils.md5Hex(password+salt).equalsIgnoreCase(digest) && userExist){
			int[] result = new int[2];
			result[0] = permission;
			result[1] = userId;
			return result;
		}else{
			int[] result = new int[2];
			result[0] = -1;
			result[1] = -1;
			return result;
		}
	}
	
	public int addBroker(int userId, String brokerName, String shortDescription) throws SQLException{
		checkDb();
		com.powertac.tourney.beans.Broker b = new com.powertac.tourney.beans.Broker(brokerName, shortDescription);
		
		if(addBrokerStatement == null || addBrokerStatement.isClosed()){
			addBrokerStatement = conn.prepareStatement(Constants.ADD_BROKER);	
		}
		
		addBrokerStatement.setString(1, brokerName);
		addBrokerStatement.setString(2, b.getBrokerAuthToken());
		addBrokerStatement.setString(3, shortDescription);
		addBrokerStatement.setInt(4, userId);
		
		
		return addBrokerStatement.executeUpdate();
		
	}
	
	public List<Broker> getBrokersByUserId(int userId) throws SQLException{
		checkDb();
		List<Broker> brokers = new ArrayList<Broker>();
		
		if(selectBrokersByUserId == null || selectBrokersByUserId.isClosed()){
			selectBrokersByUserId = conn.prepareStatement(Constants.SELECT_BROKERS_BY_USERID);
		}
		selectBrokersByUserId.setInt(1, userId);
		ResultSet rsBrokers = selectBrokersByUserId.executeQuery();
		while(rsBrokers.next()){
			Broker tmp = new Broker("new");
			tmp.setBrokerAuthToken(rsBrokers.getString("brokerAuth"));
			tmp.setBrokerId(rsBrokers.getInt("brokerId"));
			tmp.setBrokerName(rsBrokers.getString("brokerName"));
			tmp.setShortDescription(rsBrokers.getString("brokerShort"));
			tmp.setNumberInGame(rsBrokers.getInt("numberInGame"));
			
			
			brokers.add(tmp);
			
		}
		
		return brokers;
			
	}
	
	public int deleteBrokerByBrokerId(int brokerId) throws SQLException{
		checkDb();
		
		if(deleteBrokerById == null || deleteBrokerById.isClosed()){
			deleteBrokerById = conn.prepareStatement(Constants.DELETE_BROKER_BY_BROKERID);
		}
		deleteBrokerById.setInt(1, brokerId);
		
		return deleteBrokerById.executeUpdate();		
	}
	
	public int updateBrokerByBrokerId(int brokerId, String brokerName, String brokerAuth, String brokerShort) throws SQLException{
		checkDb();
		
		
		if(updateBrokerById == null || updateBrokerById.isClosed()){
			updateBrokerById = conn.prepareStatement(Constants.UPDATE_BROKER_BY_BROKERID);
		}
		updateBrokerById.setString(1, brokerName);
		updateBrokerById.setString(2, brokerAuth);
		updateBrokerById.setString(3, brokerShort);
		updateBrokerById.setInt(4, brokerId);
		
		return updateBrokerById.executeUpdate();
	}
	
	public Broker getBroker(int brokerId) throws SQLException{
		checkDb();
		Broker broker = new Broker("new");
		if(selectBrokerByBrokerId == null || selectBrokerByBrokerId.isClosed()){
			selectBrokerByBrokerId = conn.prepareStatement(Constants.SELECT_BROKER_BY_BROKERID);
		}
		
		ResultSet rsBrokers = selectBrokersByUserId.executeQuery();
		if(rsBrokers.next()){
			broker.setBrokerAuthToken(rsBrokers.getString("brokerAuth"));
			broker.setBrokerId(rsBrokers.getInt("brokerId"));
			broker.setBrokerName(rsBrokers.getString("brokerName"));
			broker.setShortDescription(rsBrokers.getString("brokerShort"));
			broker.setNumberInGame(rsBrokers.getInt("numberInGame"));
		}
		
		return broker;
	}
	
	public List<String> getProperties(int gameId) throws SQLException {
		checkDb();
		List<String> props = new ArrayList<String>();
		
		
		if(selectPropsById == null || selectPropsById.isClosed()){
			selectPropsById = conn.prepareStatement(Constants.SELECT_PROPERTIES_BY_ID);
		}
		
		selectPropsById.setInt(1, gameId);
		
		ResultSet rsProps = selectPropsById.executeQuery();
		if(rsProps.next()){
			props.add(rsProps.getString("location"));
			props.add(rsProps.getString("startTime"));
		}
		
		return props;
		
	}
	
	public int addProperties(int gameId, String locationKV, String startTimeKV) throws SQLException{
		checkDb();
		if(addPropsById == null || addPropsById.isClosed()){
			addPropsById = conn.prepareStatement(Constants.ADD_PROPERTIES);
		}
		
		addPropsById.setInt(1, gameId);
		addPropsById.setString(2, locationKV);
		addPropsById.setString(3, startTimeKV);
		
		return addPropsById.executeUpdate();
	}
	
	public int addPom(String uploadingUser, String name, String location) throws SQLException{
		checkDb();
		if(addPom == null || addPom.isClosed()){
			addPom = conn.prepareStatement(Constants.ADD_POM);
		}
		
		addPom.setString(1, uploadingUser);
		addPom.setString(2, name);
		addPom.setString(3, location);
		
		return addPom.executeUpdate();
	}
	
	public class Pom {
		private String name;
		private String location;
		private String uploadingUser;
		
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getLocation() {
			return location;
		}
		public void setLocation(String location) {
			this.location = location;
		}
		public String getUploadingUser() {
			return uploadingUser;
		}
		public void setUploadingUser(String uploadingUser) {
			this.uploadingUser = uploadingUser;
		}
		
	}
	
	public List<Pom> getPoms() throws SQLException{
		checkDb();
		List<Pom> poms = new ArrayList<Pom>();
		
		if(selectPoms == null || selectPoms.isClosed()){
			selectPoms = conn.prepareStatement(Constants.SELECT_POMS);
		}
		
		ResultSet rsPoms = selectPoms.executeQuery();
		while(rsPoms.next()){
			Pom tmp = new Pom();
			tmp.setLocation(rsPoms.getString("location"));
			tmp.setName(rsPoms.getString("name"));
			tmp.setUploadingUser(rsPoms.getString("uploadingUser"));
			
			poms.add(tmp);
		}
		
		return poms;
		
	}
	

	public String getDbUrl() {
		return dbUrl;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDbms() {
		return dbms;
	}

	public void setDbms(String dbms) {
		this.dbms = dbms;
	}

}
