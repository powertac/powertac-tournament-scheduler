package org.powertac.tourney.listeners;

import java.util.Timer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.powertac.tourney.beans.Games;
import org.powertac.tourney.beans.Machines;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournaments;
import org.powertac.tourney.beans.Users;


public class Initializer implements ServletContextListener
{

  public void contextDestroyed (ServletContextEvent e)
  {

    // e.getServletContext().getAttribute(Games.getKey());
    // e.getServletContext().getAttribute(Tournaments.getKey());
    // e.getServletContext().getAttribute(Machines.getKey());
    ((Timer) e.getServletContext().getAttribute(Scheduler.getKey())).cancel();

  }

  public void contextInitialized (ServletContextEvent e)
  {
    // TODO Check database and load in pending and inprogress games
    // Load games, tournaments and machines into the application context

    e.getServletContext().setAttribute(Users.getKey(), new Users());
    e.getServletContext().setAttribute(Games.getKey(), new Games());
    e.getServletContext().setAttribute(Tournaments.getKey(), new Tournaments());
    e.getServletContext().setAttribute(Machines.getKey(), new Machines());
    e.getServletContext().setAttribute(Scheduler.getKey(), new Scheduler());
  }

}
