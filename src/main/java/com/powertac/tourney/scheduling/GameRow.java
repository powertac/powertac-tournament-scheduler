package com.powertac.tourney.scheduling;

public class GameRow {
	GameRowLet[] gamerow;
	AgentLet agent;
	
	GameRow(int[] g) {
		int i;
		gamerow = new GameRowLet[g.length];
		for(i=0;i<g.length;i++) {
			gamerow[i]  = new GameRowLet();
			gamerow[i].setGameType(g[i]);
			gamerow[i].setGameFreq(0);
		}
		agent = new AgentLet(0,0);
	}
		
	private int fetch (int gtype) {
		int i;
		for(i=0;i<gamerow.length;i++) {
			if(gamerow[i].getGameType() == gtype) return i;
 		}
		return -1;
	}
	public void setAgentType(int atype) {
		agent.setAgentType(atype);		
	}
	public int getAgentType() {
		return agent.getAgentType();		
	}	
	public void setFreqGameType(int gtype, int freq){
		gamerow[fetch(gtype)].setGameFreq(freq);
	}	
	public int getFreqGameType(int gtype){
		return gamerow[fetch(gtype)].getGameFreq();
	}
}
