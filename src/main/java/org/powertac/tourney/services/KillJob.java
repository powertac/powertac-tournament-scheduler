package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Machine;

import java.net.URL;
import java.net.URLConnection;


public class KillJob
{
  private static Logger log = Logger.getLogger("TMLogger");


  public KillJob (Machine machine)
  {
    if (machine == null) {
      return;
    }
    // Get the machineName and stop the job on Jenkins
    TournamentProperties properties = TournamentProperties.getProperties();
    String machineName = machine.getMachineName();
    String stopUrl = properties.getProperty("jenkins.location")
        + "computer/" + machineName + "/executors/0/stop";
    log.info("Stop url: " + stopUrl);

    try {
      URL url = new URL(stopUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
    }
    catch (Exception ignored) {}
    log.info("Stopped job on Jenkins");

    // Actually kill the job on the slave, it doesn't always get done above
    String killUrl = properties.getProperty("jenkins.location")
        + "job/kill-server-instance/buildWithParameters?"
        + "machine=" + machineName;
    log.info("Kill url: " + killUrl);

    try {
      URL url = new URL(killUrl);
      URLConnection conn = url.openConnection();
      conn.getInputStream();
    }
    catch (Exception ignored) {}
    log.info("Killed job on slave " + machineName);
  }
}
