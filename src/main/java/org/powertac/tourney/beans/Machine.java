package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.JenkinsConnector;
import org.powertac.tourney.services.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.persistence.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "machines")
public class Machine
{
  private static Logger log = Logger.getLogger("TMLogger");

  private Integer machineId;
  private String machineName;
  private String machineUrl;
  private String vizUrl;
  private STATE state;
  private boolean available;

  public static enum STATE
  {
    idle, running
  }

  public Machine ()
  {
  }

  public boolean stateEquals (STATE state)
  {
    return this.state.equals(state);
  }

  @Transient
  public boolean isInProgress ()
  {
    return state.equals(STATE.running);
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

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      NodeList nList = JenkinsConnector.getNodeList();

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

            log.debug("Checking machine " + displayName);

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
              machine.setState(STATE.running);
              log.warn(String.format("Machine %s has status 'idle', but "
                  + "Jenkins reports 'not idle'", displayName));
            }

            session.update(machine);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (session.isDirty()) {
        transaction.commit();
      }
    } catch (IOException ioe) {
      transaction.rollback();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  @SuppressWarnings("unchecked")
  public static Machine getFreeMachine (Session session)
  {
    return (Machine) session.createCriteria(Machine.class)
        .add(Restrictions.eq("state", Machine.STATE.idle))
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
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return machines;
  }

  public static void delayedMachineUpdate (Machine machine, int delay)
  {
    if (machine == null) {
      return;
    }

    // There are 2 scenarios where we want to delay the setting to idle.
    // 1 After we kill a job. The viz doesn't receive an end-of-sim message.
    // It takes the viz 2 minutes to recover from this, so we delay for 5 min.
    // 2 When the jenkins-script sends an we're-done message, it still takes
    // some time to end the jenkins job. If the check-machines method runs
    // before the jobs end, the machine gets set to not-idle and never recover.

    class updateThread implements Runnable
    {
      private int machineId;
      private int delay;

      public updateThread (int machineId, int delay)
      {
        this.machineId = machineId;
        this.delay = delay;
      }

      public void run ()
      {
        Utils.secondsSleep(delay);

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try {
          log.info("Setting machine " + machineId + " to idle");
          Machine machine = (Machine) session.get(Machine.class, machineId);
          machine.setState(Machine.STATE.idle);
          transaction.commit();
        } catch (Exception e) {
          transaction.rollback();
          e.printStackTrace();
          log.error("Error updating machine state after job kill");
        }
        session.close();
      }
    }
    Runnable r = new updateThread(machine.getMachineId(), delay);
    new Thread(r).start();
  }

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "machineId", unique = true, nullable = false)
  public Integer getMachineId ()
  {
    return machineId;
  }

  public void setMachineId (Integer machineId)
  {
    this.machineId = machineId;
  }

  @Column(name = "machineName", unique = true, nullable = false)
  public String getMachineName ()
  {
    return machineName;
  }

  public void setMachineName (String machineName)
  {
    this.machineName = machineName;
  }

  @Column(name = "machineUrl", unique = true, nullable = false)
  public String getMachineUrl ()
  {
    return machineUrl;
  }

  public void setMachineUrl (String machineUrl)
  {
    this.machineUrl = machineUrl;
  }

  @Column(name = "visualizerUrl", nullable = false)
  public String getVizUrl ()
  {
    return vizUrl;
  }

  public void setVizUrl (String vizUrl)
  {
    this.vizUrl = vizUrl;
  }

  @Column(name = "state", nullable = false)
  @Enumerated(EnumType.STRING)
  public STATE getState ()
  {
    return state;
  }

  public void setState (STATE state)
  {
    this.state = state;
  }

  @Column(name = "available", nullable = false)
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
