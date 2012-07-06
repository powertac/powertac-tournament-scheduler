/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {
  public static String getTourneyUrl() {
    // TODO Get this from Properties ??
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

  public static String getJenkinsUrl() {
    // TODO Get this from Properties ??
    String jenkinsUrl = "http://localhost:8080/jenkins/";

    return jenkinsUrl;
  }

  public static boolean checkClientAllowed(String clientAddress) {


    return true;
  }


  public static String dateFormat(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return sdf.format(date);
  }
  public static String dateFormatUTC(Date date) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return sdf.format(date);
  }
  public static Date dateFormatUTCmilli(String date) throws ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    return sdf.parse(date);
  }
}
