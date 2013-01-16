package org.powertac.tourney.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

public class Initializer implements ServletContextListener {
  public void contextDestroyed(ServletContextEvent e) {
    // e.getServletContext().getAttribute(Games.getKey());
    // e.getServletContext().getAttribute(Tournaments.getKey());
    // e.getServletContext().getAttribute(Machines.getKey());

    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      try {
        DriverManager.deregisterDriver(driver);
      } catch (Exception ignored) {
      }
    }
  }

  public void contextInitialized(ServletContextEvent e) {
  }
}
