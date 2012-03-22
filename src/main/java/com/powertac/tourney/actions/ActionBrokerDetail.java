package com.powertac.tourney.actions;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.powertac.tourney.beans.Broker;
import com.powertac.tourney.beans.User;

@SessionScoped
@ManagedBean
public class ActionBrokerDetail {
	private static final String key = "abd";
	
	
	
	private boolean edit = false;
	private Broker selectedBroker;
	private String newName;
	private String newAuthorization;
	private String newShortDescription;
	
	public static String getKey(){
		return key;
	}

	public boolean isEdit() {
		return edit;
	}

	public void setEdit(boolean edit) {
		this.edit = edit;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getNewAuthorization() {
		return newAuthorization;
	}

	public void setNewAuthorization(String newAuthorization) {
		this.newAuthorization = newAuthorization;
	}

	public String getNewShortDescription() {
		return newShortDescription;
	}

	public void setNewShortDescription(String newShortDescription) {
		this.newShortDescription = newShortDescription;
	}

	public Broker getSelectedBroker() {
		return selectedBroker;
	}

	public void setSelectedBroker(Broker selectedBroker) {
		this.selectedBroker = selectedBroker;
	}

	public String startDetail(int selectedBrokerId) {
		//ActionAccount aa = (ActionAccount) FacesContext.getCurrentInstance()
		//		.getExternalContext().getSessionMap()
		//		.get(ActionAccount.getKey());
		
		int brokerId = selectedBrokerId;//aa.getSelectedBrokerId();
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap()
				.get(User.getKey());
		
		selectedBroker = user.getBroker(brokerId);
		
		newName = selectedBroker.getBrokerName();
		newAuthorization = selectedBroker.getBrokerAuthToken();
		newShortDescription = selectedBroker.getShortDescription();
		
		FacesContext.getCurrentInstance().renderResponse();
		
		return "Detail";
	}
	
	public String startEdit(){
		this.edit = true;
		return "Detail";
	}
	
	public String discardEdit(){
		this.edit = false;
		return null;
	}
	
	public String saveEdit(){
		selectedBroker.setBrokerAuthToken(newAuthorization);
		selectedBroker.setBrokerName(newName);
		selectedBroker.setShortDescription(newShortDescription);
		

		this.edit = false;
		return "Detail";
		
	}

}
