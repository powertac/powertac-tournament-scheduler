package org.powertac.tournament.jobs;

import org.apache.log4j.Logger;
import org.powertac.tournament.services.JenkinsConnector;
import org.powertac.tournament.services.Properties;
import org.powertac.tournament.services.Utils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class RunKill
{
  private static Logger log = Utils.getLogger();

  private String machineName;

  public RunKill (String machineName)
  {
    this.machineName = machineName;
  }

  public void run ()
  {
    if (machineName.isEmpty()) {
      return;
    }

    Properties properties = Properties.getProperties();

    // Stop the job on Jenkins, only work on when host = localhost
    for (int count = 0; count < 2; count++) {
      try {
        NodeList nList = JenkinsConnector.getExecutorList(machineName, count);
        Node nNode = nList.item(0);
        boolean idle = Boolean.parseBoolean(nNode.getTextContent());

        if (!idle) {
          String stopUrl = properties.getProperty("jenkins.location")
              + "computer/" + machineName + "/executors/" + count + "/stop";
          log.info("Stop url: " + stopUrl);

          try {
            JenkinsConnector.sendJob(stopUrl);
          }
          catch (Exception ignored) {
          }
          log.info("Stopped job on Jenkins");
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Actually kill the job on the slave, it doesn't always get done above
    String killUrl = properties.getProperty("jenkins.location")
        + "job/kill-server-agent/buildWithParameters?"
        + "machine=" + machineName;
    log.info("Kill url: " + killUrl);

    try {
      JenkinsConnector.sendJob(killUrl);
    }
    catch (Exception ignored) {
    }
    log.info("Killed job on slave " + machineName);
  }
}
