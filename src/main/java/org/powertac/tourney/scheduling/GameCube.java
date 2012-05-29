package org.powertac.tourney.scheduling;

import java.sql.ResultSet;

/*
 * send me the N and m 
 * 
 */

public class GameCube {
	private int noofagents;
	private int ngames; /*no of games */
	private  GameCubeLet[]  cube;
	private AgentLet[]  map; /* look up map for the agent ids */
	private String queuetype; 
	private int index;
	
	public GameCube (DbConnection db, int nagents, int[] gametypes, int[] maxarray) throws Exception {		
		noofagents = nagents;
		ngames = gametypes.length;
		queuetype = "MAXDISP";
		index = 0;
		map = new AgentLet[nagents];
		initializeMap(db);
		initializeCube (nagents, gametypes, maxarray);
	}


	private void initializeCube(int nagents, int[] gametypes,int[] maxarray) {
		int i;
		cube = new GameCubeLet[gametypes.length];
		for(i=0;i<gametypes.length;i++) {			
			cube[i] = new GameCubeLet(nagents, gametypes[i], maxarray[i]);
		}	
	}
		
	private void initializeMap(DbConnection db) throws Exception {
		AgentLet a;
		ResultSet rs;
		int i=0;
		//System.out.println("Kailash: initializeMap");
		String sql_load_agents = "select distinct AgentType as atype from AgentQueue order by rand()";
		//System.out.println(sql_load_agents);
		rs = db.SetQuery(sql_load_agents);
		while(rs.next()) {
			//System.out.println("Kailash:initializeMap:While");
			map[i] = new AgentLet(0,rs.getInt("atype"));
			map[i].getAgentId();
			i++;
		}
		//System.out.println("Kailash:initializeMap:"+i);
	}	
	
	/* 
	 * 
	 * returns the game id where (current_games_played) - (max_games_to_play) is max.
	 * 
	 * 
	 */
	private int maxDisparity() {
		int i,tempmax,maxdiff= 0,mark = -1;
		int maxgame=0;
		int j =0;
		for(i=index;j<cube.length; i=(i+1)%cube.length,j++) {
			//System.out.println("i ="+i);
			if(!cube[i].getLookAhead()) {
				tempmax = cube[i].getReqMax() - cube[i].getCurrentMin();
				if(maxdiff<tempmax) {
					maxdiff = tempmax;
					maxgame = cube[i].getTypeOfGame();
					mark = i;
				}	
			}
		}
		index = (mark+1)%cube.length;
		return maxgame;		
	}	

	
	public int getDisparity() {
		int gamenumber = -1;
		
		/*if(queuetype  == "FCFS") {
			gamenumber = firstComeFirstServed();
		}*/
		if(queuetype == "MAXDISP") { /*I types BFS*/
			gamenumber = maxDisparity();			
		}
		/*if(queuetype == "MINDISP") { /*I type DFS*/
		/*	gamenumber = minDisparity();
		}*/		
		return gamenumber;
	}
	
	private int findGameTypeIndex(int gtype) {
		int i=-1;
		for (i=0;i<cube.length;i++) {
			if(cube[i].getGameType() == gtype){
				break;		
			}			
		}		
		return i;		
	}
	
	public void clearLookAheads() {
		for (int i=0;i<ngames;i++) {
			cube[i].setLookAhead(false);
		}
		
	}
	
	
	private AgentLet[] canSchedule(	DbConnection db,int[] tmask) throws Exception {		
		AgentLet [] agentlist;
		AgentLet [] returnAgentList;
		int i,len;
		AgentLet a;
		ResultSet rs;
		/*
		 * uses the map
		 * queries the db for availability
		 * and says yes or no
		 */
		String wherestring=" (";		
		for(i=0;i<tmask.length;i++) {
			//System.out.println("Kailash:tmask:"+tmask[i]);
			wherestring+= map[tmask[i]].getAgentType()+", ";
		}
		len = wherestring.length();
		wherestring = wherestring.substring(0,len-2)+")";
		
		String sql_get_query = "select InternalAgentID, AgentType from AgentQueue" +
				" where AgentType in "+ wherestring +" " +
				" and IsPlaying = 0  " +
				" group by AgentType ";
		//System.out.println(sql_get_query);
		rs = db.SetQuery(sql_get_query);
		i = 0;
		agentlist = new AgentLet[tmask.length];
		while(rs.next()) {
			agentlist[i] = new AgentLet(rs.getInt("InternalAgentID"),rs.getInt("AgentType"));
			i++;	
		}
		returnAgentList =  new AgentLet[i];
		System.arraycopy(agentlist,0,returnAgentList,0,i);
		return returnAgentList;
	}
	
	public AgentLet[] getAgents(DbConnection db,int gametype) throws Exception {
		int i=0,index;
		int[] agentsindices;
		int[] rmask = new int[gametype];
		AgentLet[] agentarray = new AgentLet[gametype];
		/*
		 * 1. first get the schedule from the cube
		 * 2. Query corresponding agents from the db
		 * 3. if present send the schedule
		 * 
		 */
		
                
		index = findGameTypeIndex(gametype);
		cube[index].initializeCombination();
		do {
			agentsindices = cube[index].sortAndGetIndices();
			//System.out.println("getAgents AgentsIndicies: "+ agentsindices.length);
	                //System.out.println("getAgents GameType: "+ gametype);
	                
			System.arraycopy(agentsindices, 0, rmask, 0, gametype);
			agentarray = canSchedule(db, rmask);
			cube[index].addGameToProposedSumArray();
		} while (agentarray.length < gametype && !(cube[index].getLookAhead()));
		/*false */
		if(agentarray.length < gametype) {
			return null;
		}
		else {
			if(!(cube[index].getLookAhead())) {
				cube[index].finalizeCombination(agentsindices);
				return agentarray;
			}
			return null;
		}
	}
	public void resetActualSumsAndProposedSums() {		
		int i,g;
		for(g=0;g<ngames;g++) {
			cube[g].resetActualSums();		
			cube[g].resetProposedSums();
		}		
	}
	public AgentLet[] getAgentsForGamesEstimates(DbConnection db,int gametype) throws Exception {
		int i=0,index;
		int[] agentsindices;
		int[] rmask = new int[gametype];
		AgentLet[] agentarray = new AgentLet[gametype];
		/*
		 * 1. first get the schedule from the cube
		 * 2. Query corresponding agents from the db
		 * 3. if present send the schedule
		 * 
		 */
		index = findGameTypeIndex(gametype);
		cube[index].initializeCombination();
		agentsindices = cube[index].sortAndGetIndices();
		System.out.println("AgentsIndicies: "+ agentsindices.length);
		System.out.println("GameType: "+ gametype);
		
		//System.arraycopy(agentsindices, 0, rmask, 0, gametype);
		cube[index].addGameToProposedSumArray();	
		cube[index].finalizeCombination(agentsindices);
		return agentarray;
	}

}