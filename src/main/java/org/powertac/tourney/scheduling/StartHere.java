package org.powertac.tourney.scheduling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

class StartHere {

	public static void main(String args[]) throws Exception {

		MainScheduler mainsch;
		/* To be read from a configuration file */
		int noofagents = 12;
		int noofcopies = 2;
		int noofservers = 10;
		int[] gtypes = { 2, 4, 8 };
		int[] mxs = { 12, 12, 12 };

		System.out.println("The number of Agents/n(duplicates)/Servers: "
				+ noofagents + "/" + noofcopies + "/" + noofservers);

		mainsch = new MainScheduler(noofagents, noofservers);
		mainsch.initServerPanel(noofservers);
		mainsch.initializeAgentsDB(noofagents, noofcopies);
		mainsch.initGameCube(gtypes, mxs);
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
		}

		System.out.println("Final !!");
	}
}