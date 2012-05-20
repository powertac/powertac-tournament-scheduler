package com.powertac.tourney.actions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Location;
import com.powertac.tourney.beans.Machine;
import com.powertac.tourney.beans.User;
import com.powertac.tourney.services.Database;
import com.powertac.tourney.services.Upload;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("actionAdmin")
@Scope("request")
public class ActionAdmin {

	@Autowired
	private Upload upload;

	private String sortColumn = null;
	private boolean sortAscending = true;
	private int rowCount = 5;

	private String sortColumnMachine = null;
	private boolean sortAscendingMachine = true;
	private int rowCountMachine = 5;

	private String newLocationName = "";
	private Date newLocationStartTime;
	private Date newLocationEndTime;
	
	private String newName = "";
	private String newUrl = "";

	private UploadedFile pom;
	private String pomName;

	public String getPomName() {
		return pomName;
	}

	public void setPomName(String pomName) {
		this.pomName = pomName;
	}

	public UploadedFile getPom() {
		return pom;
	}

	public void setPom(UploadedFile pom) {
		this.pom = pom;
	}

	public void submitPom() {
		if (pom != null) {
			System.out.println("Pom is not null");
			upload.setUploadedFile(this.pom);
			String finalName = upload.submit(this.pomName);

			User currentUser = (User) FacesContext.getCurrentInstance()
					.getExternalContext().getSessionMap().get(User.getKey());

			Database db = new Database();
			try {
				db.addPom(currentUser.getUsername(), this.getPomName(),
						upload.getUploadLocation() + finalName);
				
				db.closeConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("Pom was null");
		}
	}

	public List<Database.Pom> getPomList() {
		List<Database.Pom> poms = new ArrayList<Database.Pom>();

		Database db = new Database();

		try {
			poms = db.getPoms();
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return poms;

	}

	public List<Location> getLocationList() {
		List<Location> locations = new ArrayList<Location>();
		Database db = new Database();

		try {
			locations = db.getLocations();
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return locations;

	}

	public List<Machine> getMachineList() {
		List<Machine> machines = new ArrayList<Machine>();
		Database db = new Database();

		try {
			machines = db.getMachines();
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return machines;
	}

	public void toggleAvailable(Machine m) {
		Database db = new Database();

		try {
			if (m.isAvailable()) {
				db.setMachineAvailable(m.getMachineId(), false);
				db.closeConnection();
			} else {
				db.setMachineAvailable(m.getMachineId(), true);
				db.closeConnection();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	public void deleteMachine(Machine m) {
		Database db = new Database();
		try {
			db.deleteMachine(m.getMachineId());
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addMachine(){
		Database db = new Database();
		try {
			db.addMachine(newName, newUrl);
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void restartGame(Game g){
		
	}
	
	public void deleteGame(Game g){
		
	}

	public void deleteLocation(Location l) {
		Database db = new Database();
		try {
			db.deleteLocation(l.getLocationId());
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void addLocation() {
		Database db = new Database();
		try {
			db.addLocation(newLocationName, newLocationStartTime,
					newLocationEndTime);
			db.closeConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int getRowCount() {
		return rowCount;
	}

	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

	public void setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
	}

	public String getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(String sortColumn) {
		this.sortColumn = sortColumn;
	}

	public String getNewLocationName() {
		return newLocationName;
	}

	public void setNewLocationName(String newLocationName) {
		this.newLocationName = newLocationName;
	}

	public Date getNewLocationStartTime() {
		return newLocationStartTime;
	}

	public void setNewLocationStartTime(Date newLocationStartTime) {
		this.newLocationStartTime = newLocationStartTime;
	}

	public Date getNewLocationEndTime() {
		return newLocationEndTime;
	}

	public void setNewLocationEndTime(Date newLocationEndTime) {
		this.newLocationEndTime = newLocationEndTime;
	}

	public String getSortColumnMachine() {
		return sortColumnMachine;
	}

	public void setSortColumnMachine(String sortColumnMachine) {
		this.sortColumnMachine = sortColumnMachine;
	}

	public boolean isSortAscendingMachine() {
		return sortAscendingMachine;
	}

	public void setSortAscendingMachine(boolean sortAscendingMachine) {
		this.sortAscendingMachine = sortAscendingMachine;
	}

	public int getRowCountMachine() {
		return rowCountMachine;
	}

	public void setRowCountMachine(int rowCountMachine) {
		this.rowCountMachine = rowCountMachine;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getNewUrl() {
		return newUrl;
	}

	public void setNewUrl(String newUrl) {
		this.newUrl = newUrl;
	}

}
