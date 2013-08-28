package org.powertac.tournament.services;

import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.PooledDataSource;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;


public class HibernateUtil
{
  private static Logger log = Utils.getLogger();

  private static final SessionFactory sessionFactory = buildSessionFactory();

  private static SessionFactory buildSessionFactory ()
  {
    try {
      // Create the SessionFactory from hibernate.cfg.xml
      Configuration configuration = new Configuration();
      configuration.configure();
      ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
          .applySettings(configuration.getProperties()).buildServiceRegistry();
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

  public static Session getSession ()
  {
    return getSessionFactory().openSession();
  }

  public static void shutdown ()
  {
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      try {
        DriverManager.deregisterDriver(driver);
      } catch (Exception ignored) {}
    }

    for (Object obj : C3P0Registry.getPooledDataSources()) {
      try {
        PooledDataSource dataSource = (PooledDataSource) obj;
        dataSource.close();
      } catch (Exception ignored) {
      }
    }

    getSessionFactory().close();
  }
}