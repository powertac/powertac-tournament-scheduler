package com.powertac.tourney.beans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import com.powertac.tourney.services.Database;

@ApplicationScoped
@ManagedBean
public class Machines {
	private static final String key = "machines";
	private Vector<Machine> machines;
	public static String getKey() {
		return key;
	}
	
	public static Machines getAllMachines(){
		return (Machines) FacesContext.getCurrentInstance()
				.getExternalContext().getApplicationMap().get(Machines.getKey());
	}

	
	public Vector<Machine> getFreeMachines(){
		Vector<Machine> newMachine = new Vector<Machine>();
		for (Machine m : machines){
			if(!m.isInProgress()){
				newMachine.add(m);
			}
		}
		return newMachine;
		
	}
	
	public List<Machine> getMachineList(){
		Database db = new Database();
		try {
			return db.getMachines();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ArrayList<Machine>();
		}
		
		/*Machine[] newMachine = new Machine[machines.size()];
		int i = 0;
		for(Machine m : machines){
			newMachine[i] = m;
			i++;
		}
		return newMachine;*/
	}
	
	
	public Machines(){
		machines = new Vector<Machine>();
		
		Machine tac04 = new Machine();
		Machine tac10 = new Machine();
		Machine tac11 = new Machine();
		Machine tac12 = new Machine();
		Machine tac13 = new Machine();
		
		tac04.setInProgress(false);
		tac10.setInProgress(false);
		tac11.setInProgress(false);
		tac12.setInProgress(false);
		tac13.setInProgress(false);
		
		tac04.setName("tac04");
		tac10.setName("tac10");
		tac11.setName("tac11");
		tac12.setName("tac12");
		tac13.setName("tac13");
		
		tac04.setUrl("tac04.cs.umn.edu");
		tac10.setUrl("tac10.cs.umn.edu");
		tac11.setUrl("tac11.cs.umn.edu");
		tac12.setUrl("tac12.cs.umn.edu");
		tac13.setUrl("tac13.cs.umn.edu");
		
		machines.add(tac04);
		machines.add(tac10);
		machines.add(tac11);
		machines.add(tac12);
		machines.add(tac13);

	}

}
