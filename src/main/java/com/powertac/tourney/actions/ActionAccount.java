package com.powertac.tourney.actions;

import java.util.List;
import java.util.Vector;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import com.powertac.tourney.beans.Broker;
import com.powertac.tourney.beans.Tournament;
import com.powertac.tourney.beans.Tournaments;
import com.powertac.tourney.beans.User;

@SessionScoped
@ManagedBean
public class ActionAccount {
	
	private static final String key = "account";

	private String newBrokerName;
	private int selectedBrokerId;
	private String selectedBrokerName;
	private String selectedBrokerAuth;

	public ActionAccount() {

	}
	
	public static String getKey(){
		return key;
	}

	public String getNewBrokerName() {
		return newBrokerName;
	}

	public void setNewBrokerName(String newBrokerName) {
		this.newBrokerName = newBrokerName;
	}

	public String addBroker() {
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());

		Broker b = user.addBroker(newBrokerName);

		if (user.getBrokers().length == 0) {
			this.selectedBrokerName = newBrokerName;
			this.selectedBrokerId = b.getBrokerId();
			this.selectedBrokerAuth = b.getBrokerAuthToken();
		}

		return "Account";
	}

	public String getSelectedBrokerName() {
		return selectedBrokerName;
	}

	public void setSelectedBrokerName(String selectedBrokerName) {
		this.selectedBrokerName = selectedBrokerName;
	}

	public int getSelectedBrokerId() {
		return selectedBrokerId;
	}

	public void setSelectedBrokerId(int selectedBrokerId) {
		this.selectedBrokerId = selectedBrokerId;
	}

	public String deleteSelectedBroker() {
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		user.deleteBroker(selectedBrokerId);

		return null;
	}

	public void listChanged(ValueChangeEvent ve) {
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		
		
		if (ve.getNewValue() != null) {
			//System.out.println("listChanged: " + ve.getNewValue().toString());
			setSelectedBrokerId(Integer.parseInt(ve.getNewValue().toString()));
			setSelectedBrokerName(user.getBroker(selectedBrokerId).getBrokerName());
			setSelectedBrokerAuth(user.getBroker(selectedBrokerId).getBrokerAuthToken());
			
		} else {
			//System.out.println("listChanged: " + ve.getOldValue().toString());
			setSelectedBrokerId(Integer.parseInt(ve.getOldValue().toString()));
			if(user.getBroker(selectedBrokerId)!= null){
				setSelectedBrokerName(user.getBroker(selectedBrokerId).getBrokerName());
				setSelectedBrokerAuth(user.getBroker(selectedBrokerId).getBrokerAuthToken());
			}
		}
	}


	public List<Tournament> getAvailableTournaments() {
		if (selectedBrokerName == null) {
			return null;
		}

		Tournaments allTournaments = Tournaments.getAllTournaments();
		Vector<Tournament> availableTourneys = new Vector<Tournament>();
		for (Tournament t : allTournaments.getLists()) {
			if (!t.isRegistered(selectedBrokerName)) {
				availableTourneys.add(t);
			}
		}

		return (List<Tournament>) availableTourneys;

	}

	public String register(String tournamentName) {
		Tournaments allTournaments = Tournaments.getAllTournaments();
		for (Tournament t : allTournaments.getLists()) {
			if (!t.isRegistered(selectedBrokerName)
					&& t.getTournamentName().equalsIgnoreCase(tournamentName)) {
				
				User user = (User) FacesContext.getCurrentInstance()
						.getExternalContext().getSessionMap().get(User.getKey());
				
				t.register(selectedBrokerName, selectedBrokerId, user.getBroker(selectedBrokerId).getBrokerAuthToken() );
				
			}
		}

		return null;
	}

	public String getSelectedBrokerAuth() {
		return selectedBrokerAuth;
	}

	public void setSelectedBrokerAuth(String selectedBrokerAuth) {
		this.selectedBrokerAuth = selectedBrokerAuth;
	}

}
