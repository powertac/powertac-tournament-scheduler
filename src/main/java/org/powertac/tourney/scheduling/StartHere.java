package org.powertac.tourney.scheduling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import static org.powertac.tourney.services.Utils.log;

class StartHere {

	public static void main(String args[]) throws Exception {

		MainScheduler mainScheduler;
		// TODO Read from config file
		int noofagents = 12;
		int noofcopies = 2;
		int noofservers = 10;
		int[] gtypes = { 2, 4, 8 };
		int[] mxs = { 12, 12, 12 };

		log("The number of Agents/n(duplicates)/Servers: {0}/{1}/{2}",
        new Object[] {noofagents, noofcopies, noofservers});

		mainScheduler = new MainScheduler(noofagents, noofservers);
		mainScheduler.initServerPanel(noofservers);
		mainScheduler.initializeAgentsDB(noofagents, noofcopies);
		mainScheduler.initGameCube(gtypes, mxs);
		log("No. of games: {0}", mainScheduler.getGamesEstimate());
		mainScheduler.resetCube();

		/* comment the while loop to prevent simulation */
		HashMap<Server, AgentLet[]> currScheduler = new HashMap<Server, AgentLet[]>();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));

		while (!mainScheduler.equilibrium()) {
			currScheduler.putAll(mainScheduler.Schedule());
			log("Currently Running Schedule:");

			for (Server s : currScheduler.keySet()) {
				log("Running on server: {0}", s.getServerNumber());
				AgentLet[] agentsInGame = currScheduler.get(s);
				for (AgentLet a : agentsInGame) {
          log("Agent: {0}", a.getAgentType());
				}
			}

			// Read in to clear server
			log("Enter server to finish:	");
			String serverNumber = "";
			int server = 0;		
			if ((serverNumber = in.readLine()) != null) {
				server = Integer.parseInt(serverNumber);
			}
			
			Server tmp = null;
			for (Server s : currScheduler.keySet()) {
				if (s.getServerNumber() == server) {
					tmp = s;
					break;
				}
			}
			
			currScheduler.remove(tmp);
			
			mainScheduler.resetServers(server);
		}

		log("Final !!");
	}
}