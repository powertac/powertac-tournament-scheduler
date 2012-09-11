package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.TournamentProperties;
import org.powertac.tourney.services.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.persistence.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="machines", catalog="tourney", uniqueConstraints={
            @UniqueConstraint(columnNames="machineId")})
public class Machine
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Integer machineId;
  private String machineName;
  private String machineUrl;
  private String vizUrl;
  private boolean available;
  private String status;

  public static enum STATE { idle, running }

  public Machine ()
  {
  }

  public boolean stateEquals(STATE state)
  {
    return this.status.equals(state.toString());
  }

  @Transient
  public boolean isInProgress ()
  {
    return status.equals(STATE.running.toString());
  }

  @Transient
  public String getJmsUrl ()
  {
    return "tcp://" + machineUrl + ":61616";
  }

  /**
   * Check the status of the Jenkins slaves against the local status
   */
  public static void checkMachines ()
  {
    log.info("WatchDogTimer Checking Machine States..");

    TournamentProperties properties = TournamentProperties.getProperties();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      String url = properties.getProperty("jenkins.location")
          + "computer/api/xml";
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder docB = dbf.newDocumentBuilder();
      Document doc = docB.parse(new URL(url).openStream());
      NodeList nList = doc.getElementsByTagName("computer");

      for (int temp = 0; temp < nList.getLength(); temp++) {
        try {
          Node nNode = nList.item(temp);
          if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            Element eElement = (Element) nNode;

            String displayName = eElement.getElementsByTagName("displayName")
                .item(0).getChildNodes().item(0).getNodeValue();
            String offline = eElement.getElementsByTagName("offline")
                .item(0).getChildNodes().item(0).getNodeValue();
            String idle = eElement.getElementsByTagName("idle")
                .item(0).getChildNodes().item(0).getNodeValue();

            // We don't check the status of the master
            log.debug("Checking machine " + displayName);
            if (displayName.equals("master")) {
              continue;
            }

            Query query = session.
                createQuery(Constants.HQL.GET_MACHINE_BY_MACHINENAME);
            query.setString("machineName", displayName);
            Machine machine = (Machine) query.uniqueResult();

            if (machine == null) {
              log.warn("Machine " + displayName + " doesn't exist in the TM");
              continue;
            }

            if (machine.isAvailable() && offline.equals("true")) {
              machine.setAvailable(false);
              log.warn(String.format("Machine %s is set available, but "
                  + "Jenkins reports offline", displayName));
            }

            if (machine.stateEquals(Machine.STATE.idle) && idle.equals("false")) {
              machine.setStatus(STATE.running.toString());
              log.warn(String.format("Machine %s has status 'idle', but "
                  + "Jenkins reports 'not idle'", displayName));
            }

            session.update(machine);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (session.isDirty()) {
        transaction.commit();
      }
    }
    catch (IOException ioe) {
      transaction.rollback();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  @SuppressWarnings("unchecked")
  public static Machine getFreeMachine (Session session)
  {
    return (Machine) session.createCriteria(Machine.class)
        .add(Restrictions.eq("status", Machine.STATE.idle.toString()))
        .add(Restrictions.eq("available", true))
        .setMaxResults(1).uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public static List<Machine> getMachineList ()
  {
    List<Machine> machines = new ArrayList<Machine>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_MACHINES);
      machines = (List<Machine>) query.list();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return machines;
  }

  public static void delayedMachineUpdate (int machineId)
  {
    // We're delaying setting the machine to idle, because after a job kill,
    // the viz doesn't get an end-of-sim message. It takes a viz 2 mins to
    // recover from this. To be on the safe side, we delay for 5 mins.
    class updateThread implements Runnable {
      private int machineId;

      public updateThread(int machineId) {
        this.machineId = machineId;
      }

      public void run() {
        Utils.secondsSleep(300);

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try {
          Machine machine = (Machine) session.get(Machine.class, machineId);
          machine.setStatus(Machine.STATE.idle.toString());
          transaction.commit();
          log.info("Setting machine " + machineId + " to idle");
        }
        catch (Exception e) {
          transaction.rollback();
          e.printStackTrace();
          log.error("Error updating machine status after job kill");
        }
        session.close();
      }
    }
    Runnable r = new updateThread(machineId);
    new Thread(r).start();
  }

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy=IDENTITY)
  @Column(name="machineId", unique=true, nullable=false)
  public Integer getMachineId ()
  {
    return machineId;
  }
  public void setMachineId (Integer machineId)
  {
    this.machineId = machineId;
  }

  @Column(name="machineName", unique=true, nullable=false)
  public String getMachineName()
  {
    return machineName;
  }
  public void setMachineName(String machineName)
  {
    this.machineName = machineName;
  }

  @Column(name="machineUrl", unique=true, nullable=false)
  public String getMachineUrl ()
  {
    return machineUrl;
  }
  public void setMachineUrl (String machineUrl)
  {
    this.machineUrl = machineUrl;
  }

  @Column(name="visualizerUrl", unique=false, nullable=false)
  public String getVizUrl ()
  {
    return vizUrl;
  }
  public void setVizUrl (String vizUrl)
  {
    this.vizUrl = vizUrl;
  }

  @Column(name="status", unique=false, nullable=false)
  public String getStatus ()
  {
    return status;
  }
  public void setStatus (String status)
  {
    this.status = status;
  }

  @Column(name="available", unique=false, nullable=false)
  public boolean isAvailable ()
  {
    return available;
  }
  public void setAvailable (boolean available)
  {
    this.available = available;
  }
  //</editor-fold>
}
