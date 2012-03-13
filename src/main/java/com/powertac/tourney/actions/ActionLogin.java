package com.powertac.tourney.actions;

import com.powertac.tourney.beans.User;
import javax.faces.context.FacesContext;

public class ActionLogin {

	private String username;
	private String password;

	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String username){
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String login() {
		// If groovy make bean
		// ive authenticated
		// Sql adapter Ibatis
		
		

		User test = (User) FacesContext.getCurrentInstance().getExternalContext()
				.getSessionMap().get(User.getKey());
		test.setUsername(this.username);
		test.setPermissions(0);
		test.login("", "");
		return "Success";
	}
	
	public String logout(){
		
		User test = (User) FacesContext.getCurrentInstance().getExternalContext()
				.getSessionMap().get(User.getKey());
		
		test.logout();
		
		
		return "Success";
	}

}
