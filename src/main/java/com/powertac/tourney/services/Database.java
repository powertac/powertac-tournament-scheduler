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
import java.util.Properties;

import com.powertac.tourney.constants.Constants;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component("database")
@Scope("request")
public class Database {
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
	Properties connectionProps = new Properties();
	Properties prop = new Properties();

	public Database() {
		/*
		try {
			prop.load(Database.class.getClassLoader().getResourceAsStream(
					"/weatherserver.properties"));
			System.out.println(prop);
			// Database Connection related properties
			this.setDatabase(prop.getProperty("db.database"));
			this.setDbms(prop.getProperty("db.dbms"));
			this.setPort(prop.getProperty("db.port"));
			this.setDbUrl(prop.getProperty("db.dbUrl"));
			this.setUsername(prop.getProperty("db.username"));
			this.setPassword(prop.getProperty("db.password"));

			System.out.println("Successfully instantiated Database bean!");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

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
				}
			}else{
				System.out.println("Connection is good");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int loginUser(String username, String password) throws SQLException{
		checkDb();
		
		boolean userExist = false;
		if (saltStatement == null || saltStatement.isClosed()){
			saltStatement = conn.prepareStatement(String.format(Constants.LOGIN_SALT,username));
		}
		
		ResultSet rsSalt = saltStatement.executeQuery();
		// salt and hash password
		String salt = "";
		String digest = "";
		int permission = 99; // Lowest permission level
		String hashedPass = "";// DigestUtils.md5Hex(password	+ salt);
		if(rsSalt.next()){
			digest = rsSalt.getString("password");
            salt = rsSalt.getString("salt");
            permission = rsSalt.getInt("permissionId");
            userExist = true;
		}else{ // Time resistant attack we need to hash something
			digest = "000000000000000000000000000=";
            salt = "00000000000=";
            userExist = false;
		}
		
			
		
		
		conn.close();
		
		if (DigestUtils.md5Hex(password+salt).equalsIgnoreCase(digest) && userExist){
			return permission;
		}else{
			return -1;
		}
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
