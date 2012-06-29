package org.powertac.tourney.scheduling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

class StartHere {

	public static void main(String args[]) throws Exception {

		/* MainSchedulerThread */
		MainScheduler mainsch;
		/* To be read from a configuration file */
		Server[] serverlist;
		int noofagents = 12;
		int noofcopies = 2;
		int noofservers = 10;
		int iteration = 1, num;
		int[] gtypes = { 2, 4, 8 };
		int[] mxs = { 12, 12, 12 };
		int nservers;

		System.out.println("The number of Agents/n(duplicates)/Servers: "
				+ noofagents + "/" + noofcopies + "/" + noofservers);

		mainsch = new MainScheduler(noofagents, noofcopies, noofservers,
				gtypes, mxs);
		// mainsch.initScoreBoard(gtypes,mxs);

		mainsch.initServerPanel(noofservers);
		mainsch.initializeAgentsDB(noofagents, noofcopies);
		mainsch.initGameCube(gtypes, mxs);
		num = noofservers;
		System.out.println("No. of games: " + mainsch.getGamesEstimate());
		mainsch.resetCube();
		/* comment the while loop to prevent simulation */
		HashMap<Server, AgentLet[]> currScheduler = new HashMap<Server, AgentLet[]>();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));

		while (!mainsch.equilibrium()) {
			

			currScheduler.putAll(mainsch.Schedule());
			
			System.out.println("Currently Running Schedule:");
			for (Server s : currScheduler.keySet()) {
				System.out.println("Running on server: " + s.getServerNumber());
				AgentLet[] agentsInGame = currScheduler.get(s);
				for (AgentLet a : agentsInGame) {
					System.out.println("Agent: " + a.getAgentType());
				}

			}

			// Read in to clear server
			System.out.println("Enter server to finish:	");
			String serverNumber = "";
			int server = 0;		
			if((serverNumber = in.readLine()) != null){
				server = Integer.parseInt(serverNumber);
			}
			
			Server tmp = null;
			for(Server s : currScheduler.keySet()){
				if(s.getServerNumber() == server){
					tmp = s;
					break;
				}
			}
			
			currScheduler.remove(tmp);
			
			mainsch.resetServers(server);

			iteration++;
		}
		// mainsch.resetServers();
		System.out.println("Final !!");

	}

}