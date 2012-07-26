package org.powertac.tourney.scheduling;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

class StartHere {
  private static Logger log = Logger.getLogger("TMLogger");

	public static void main(String args[]) throws Exception {

		MainScheduler mainScheduler;
		// TODO Read from config file
		int noofagents = 12;
		int noofcopies = 2;
		int noofservers = 10;
		int[] gtypes = { 2, 4, 8 };
		int[] mxs = { 12, 12, 12 };

		log.info(String.format("The number of Agents/n(duplicates)/Servers: "
        + "%s/%s/%s", noofagents, noofcopies, noofservers));

		mainScheduler = new MainScheduler(noofagents, noofservers);
		mainScheduler.initServerPanel(noofservers);
		mainScheduler.initializeAgentsDB(noofagents, noofcopies);
		mainScheduler.initGameCube(gtypes, mxs);
		log.info("No. of games: " + mainScheduler.getGamesEstimate());
		mainScheduler.resetCube();

		/* comment the while loop to prevent simulation */
		HashMap<Server, AgentLet[]> currScheduler = new HashMap<Server, AgentLet[]>();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));

		while (!mainScheduler.equilibrium()) {
			currScheduler.putAll(mainScheduler.Schedule());
			log.info("Currently Running Schedule:");

			for (Server s : currScheduler.keySet()) {
				log.info("Running on server: " + s.getServerNumber());
				AgentLet[] agentsInGame = currScheduler.get(s);
				for (AgentLet a : agentsInGame) {
          log.info("Agent: " + a.getAgentType());
				}
			}

			// Read in to clear server
			log.info("Enter server to finish:	");
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

		log.info("Final !!");
	}
}