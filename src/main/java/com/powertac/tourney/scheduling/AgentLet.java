package com.powertac.tourney.scheduling;

public class AgentLet {
	
	private int AgentId; 
	private int AgentType;	
	
	public AgentLet(int aid, int atype) {
		AgentId = aid;
		AgentType = atype;
	}
	
	public int getAgentType() {
		return AgentType;		
	}
	
	public int getAgentId() {		
		return AgentId;
	}
	
	public void setAgentId (int a){
		AgentId  = a;
	}
	public void setAgentType (int a) {
		AgentType  = a;
	}

}
