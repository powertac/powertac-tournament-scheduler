package com.powertac.tourney.actions;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import com.powertac.tourney.beans.Broker;
import com.powertac.tourney.beans.Tournament;
import com.powertac.tourney.beans.Tournaments;
import com.powertac.tourney.beans.User;
import com.powertac.tourney.services.Database;


@ManagedBean
@RequestScoped
public class ActionAccount {
	

	private String newBrokerName;
	private String newBrokerShortDescription;
	private int selectedBrokerId;
	private String selectedBrokerName;
	private String selectedBrokerAuth;
	
	public ActionAccount() {

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
		// Check if user is null?
		user.addBroker(getNewBrokerName(), getNewBrokerShortDescription());
		
				

		return "Account";
	}
	
	public List<Broker> getBrokers(){
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		return user.getBrokers();
	}
	
	public void deleteBroker(Broker b){
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		user.deleteBroker(b.getBrokerId());
		
	}
	
	public void editBroker(Broker b){
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		user.setEdit(true);
		b.setEdit(true);
		b.setNewAuth(b.getBrokerAuthToken());
		b.setNewName(b.getBrokerName());
		b.setNewShort(b.getShortDescription());
	}
	
	public void saveBroker(Broker b){
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		user.setEdit(false);
		b.setEdit(false);
		b.setBrokerName(b.getNewName());
		b.setShortDescription(b.getNewShort());
		b.setBrokerAuthToken(b.getNewAuth());
		

		Database db = new Database();
		try {
			db.updateBrokerByBrokerId(b.getBrokerId(),b.getBrokerName(), b.getBrokerAuthToken(), b.getShortDescription());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void cancelBroker(Broker b){
		User user = (User) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(User.getKey());
		user.setEdit(false);
		b.setEdit(false);
	}
	


	public List<Tournament> getAvailableTournaments(Broker b) {
		if (b == null) {
			return null;
		}

		Tournaments allTournaments = Tournaments.getAllTournaments();
		Vector<Tournament> availableTourneys = new Vector<Tournament>();
		for (Tournament t : allTournaments.getLists()) {
			if (!t.isRegistered(b.getBrokerName())) {
				availableTourneys.add(t);
			}
		}

		return (List<Tournament>) availableTourneys;

	}

	public String register(Broker b) {
		String tournamentName = b.getSelectedTourney();
		if(tournamentName==null || tournamentName==""){
			return null;
		}
		
		Tournaments allTournaments = Tournaments.getAllTournaments();
		for (Tournament t : allTournaments.getLists()) {
			if (!t.isRegistered(b.getBrokerAuthToken())
					&& t.getTournamentName().equalsIgnoreCase(tournamentName)) {
				
				
				
				t.register(b.getBrokerName(), b.getBrokerId(), b.getBrokerAuthToken());
				
			}
		}

		return null;
	}



	public String getNewBrokerShortDescription() {
		return newBrokerShortDescription;
	}



	public void setNewBrokerShortDescription(String newBrokerShortDescription) {
		this.newBrokerShortDescription = newBrokerShortDescription;
	}



}
