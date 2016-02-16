package org.powertac.tournament.runners;

import org.apache.log4j.Logger;
import org.powertac.tournament.services.JenkinsConnector;
import org.powertac.tournament.services.TournamentProperties;
import org.powertac.tournament.services.Utils;


public class RunAbort
{
  private static Logger log = Utils.getLogger();

  private String machineName;

  public RunAbort (String machineName)
  {
    this.machineName = machineName;
  }

  public void run ()
  {
    if (machineName.isEmpty()) {
      return;
    }

    TournamentProperties properties = TournamentProperties.getProperties();

    // Abort the job on the slave
    String abortUrl = properties.getProperty("jenkins.location")
        + "job/abort-server-instance/buildWithParameters?"
        + "machine=" + machineName;
    log.info("Abort url: " + abortUrl);

    try {
      JenkinsConnector.sendJob(abortUrl);

      log.info("Aborted job on slave " + machineName);
    }
    catch (Exception ignored) {
      log.error("Failed to aborted job on slave " + machineName);
    }
  }
}
