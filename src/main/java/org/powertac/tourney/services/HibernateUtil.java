package org.powertac.tourney.services;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import static org.powertac.tourney.services.Utils.log;
 
public class HibernateUtil {
 
	private static final SessionFactory sessionFactory = buildSessionFactory();
 
	private static SessionFactory buildSessionFactory() {
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			return new Configuration().configure().buildSessionFactory();
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			log("Initial SessionFactory creation failed. {0}", ex);
			throw new ExceptionInInitializerError(ex);
		}
	}
 
	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
 
	public static void shutdown() {
		// Close caches and connection pools
		getSessionFactory().close();
	}
 
}