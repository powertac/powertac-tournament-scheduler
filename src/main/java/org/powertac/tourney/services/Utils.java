/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {
  public static String getTourneyUrl() {
    // TODO Get this from Properties
    String tourneyUrl = "http://%s:8080/TournamentScheduler/";
    try {
      InetAddress thisIp = InetAddress.getLocalHost();
      tourneyUrl = String.format(tourneyUrl, thisIp.getHostAddress());
    }
    catch (UnknownHostException e2) {
      e2.printStackTrace();
    }

    return tourneyUrl;
  }

  public static boolean checkClientAllowed(String clientAddress) {


    return true;
  }
}
