package com.powertac.tourney.scheduling;

import java.lang.Math;

class StartHere {

	public static void main(String args[]) throws Exception{

		/* MainSchedulerThread*/
		 MainScheduler mainsch;
		/*To be read from a configuration file*/
		 Server[] serverlist;
		int noofagents = 5;
		int noofcopies = 2; 
		int noofservers = 3;
		int iteration = 1,num;
		int[] gtypes = {2,3,4};
		int[] mxs = {2,3,4};
		int nservers;
		
		System.out.println("The number of Agents/n(duplicates)/Servers: "+noofagents+"/"+noofcopies+"/"+noofservers);
		
		mainsch = new MainScheduler();
		mainsch.init(noofagents,noofcopies,noofservers, gtypes, mxs);
		// mainsch.initScoreBoard(gtypes,mxs);
		
		mainsch.initServerPanel(noofservers);		
		mainsch.initializeAgentsDB(noofagents,noofcopies);
		mainsch.initGameCube(gtypes,mxs);
		num = noofservers;
		while(!mainsch.equilibrium()) {
			nservers = mainsch.Schedule();
		//	mainsch.resetServers();
			iteration++;
		}	
		//mainsch.resetServers();
		System.out.println("Final !!");
		
	}

	
}