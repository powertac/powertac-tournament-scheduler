package com.powertac.tourney.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.powertac.tourney.beans.Games;
import com.powertac.tourney.beans.Machines;
import com.powertac.tourney.beans.Tournaments;

public class Initializer implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void contextInitialized(ServletContextEvent e) {
		// TODO Check database and load in pending and inprogress games
		// Load games, tournaments and machines into the application context
		e.getServletContext().setAttribute(Games.getKey(), new Games());
		e.getServletContext().setAttribute(Tournaments.getKey(), new Tournaments());
		e.getServletContext().setAttribute(Machines.getKey(), new Machines());
	}

}
