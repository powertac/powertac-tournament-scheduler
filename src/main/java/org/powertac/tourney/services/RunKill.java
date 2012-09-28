package org.powertac.tourney.services;

import org.apache.log4j.Logger;


public class RunKill
{
  private static Logger log = Logger.getLogger("TMLogger");

  public RunKill(String machineName)
  {
    if (machineName.isEmpty()) {
      return;
    }

    TournamentProperties properties = TournamentProperties.getProperties();

    // Stop the job on Jenkins
    String stopUrl = properties.getProperty("jenkins.location")
        + "computer/" + machineName + "/executors/0/stop";
    log.info("Stop url: " + stopUrl);

    try {
      JenkinsConnector.sendJob(stopUrl);
    }
    catch (Exception ignored) {}
    log.info("Stopped job on Jenkins");

    // Actually kill the job on the slave, it doesn't always get done above
    String killUrl = properties.getProperty("jenkins.location")
        + "job/kill-server-instance/buildWithParameters?"
        + "machine=" + machineName;
    log.info("Kill url: " + killUrl);

    try {
      JenkinsConnector.sendJob(killUrl);
    }
    catch (Exception ignored) {}
    log.info("Killed job on slave " + machineName);
  }
}
