package org.powertac.tourney.services;

import org.apache.log4j.Logger;


public class RunAbort {
  private static Logger log = Logger.getLogger("TMLogger");

  public RunAbort(String machineName) {
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
    } catch (Exception ignored) {
      log.error("Failed to aborted job on slave " + machineName);
    }
  }
}
