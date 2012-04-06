package com.powertac.tourney.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.springframework.stereotype.Service;

@Service
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
	private PreparedStatement weatherStatement = null;
	private PreparedStatement forecastStatement = null;
	private PreparedStatement energyStatement = null;
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
		if (conn == null) {
			System.out.println("Connection is null");
			if (this.getDbms().equalsIgnoreCase("mysql")) {
				System.out.println("Using mysql as dbms ...");
				try {
					connectionProps.setProperty("user", this.getUsername());
					connectionProps.setProperty("password", this.getPassword());
					Class.forName("com.mysql.jdbc.Driver").newInstance();
					conn = DriverManager.getConnection("jdbc:" + this.getDbms()
							+ "://" + this.getDbUrl() + "/" + this.getDatabase(),
							connectionProps);
					System.out.println("Connected Successfully");
				} catch (Exception e) {
					System.out.println("Connection Error");
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Connection is good");
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
