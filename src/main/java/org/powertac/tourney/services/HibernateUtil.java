package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;


public class HibernateUtil
{
  private static Logger log = Logger.getLogger("TMLogger");

  private static final SessionFactory sessionFactory = buildSessionFactory();

  private static SessionFactory buildSessionFactory ()
  {
    try {
      // Create the SessionFactory from hibernate.cfg.xml
      Configuration configuration = new Configuration();
      configuration.configure();
      ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
      return configuration.buildSessionFactory(serviceRegistry);
    } catch (Throwable ex) {
      // Make sure you log the exception, as it might be swallowed
      log.error("Initial SessionFactory creation failed.\n" + ex);
      throw new ExceptionInInitializerError(ex);
    }
  }

  public static SessionFactory getSessionFactory ()
  {
    return sessionFactory;
  }

  public static void shutdown ()
  {
    // Close caches and connection pools
    getSessionFactory().close();
  }
}