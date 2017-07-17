package org.powertac.tournament.listeners;

import org.powertac.tournament.services.HibernateUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Set;


public class Initializer implements ServletContextListener
{
  public void contextDestroyed (ServletContextEvent e)
  {
    HibernateUtil.shutdown();

    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    for (Thread t : threadSet) {
      if (t.getName().contains("Abandoned connection cleanup thread")) {
        synchronized (t) {
          t.stop(); //don't complain, it works
        }
      }
    }
  }

  public void contextInitialized (ServletContextEvent e)
  {
  }
}
