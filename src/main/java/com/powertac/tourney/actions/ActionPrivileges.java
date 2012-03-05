package com.powertac.tourney.actions;

import javax.faces.context.FacesContext;

import com.powertac.tourney.beans.Permission;
import com.powertac.tourney.beans.User;

public class ActionPrivileges {
	public ActionPrivileges(){
		
	}
	
	public boolean getGuest(){
		User test = (User) FacesContext.getCurrentInstance().getExternalContext()
				.getSessionMap().get(User.getKey());
		
		// Each role above has the permissions of the previous
		if(test!=null && test.getPermissions() <= Permission.GUEST)	{
			return true;
		}else{
			return false;
		}
				
	}
	

}
