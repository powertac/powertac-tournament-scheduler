package com.powertac.tourney.beans;

public class User {

	/*
	 * Possible permisssions that a user can have Changes the form displayed to
	 * the user
	 */

	public static final String key = "user";

	private String username;
	private String password;
	private int permissions;
	private boolean loggedIn;

	public User() {
		this.username = "Guest";
		this.password = "";
		this.permissions = Permission.GUEST;
		this.loggedIn = false;
	}

	public static String getKey() {
		return key;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPermissions() {
		return this.permissions;
	}

	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}

	public boolean login(String username, String password) {
		// Set loggedIn value
		this.loggedIn = true;

		return false;
	}

	public boolean logout() {
		//There is probably a better way to do this
		this.username = "Guest";
		this.password = "";
		this.permissions = Permission.GUEST;
		this.loggedIn = false;

		return false;

	}

	public boolean getLoggedIn() {
		return this.loggedIn;
	}

}