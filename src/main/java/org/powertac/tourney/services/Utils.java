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


  public static String dateFormat(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return sdf.format(date);
  }
  public static String dateFormatUTC(Date date) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    // TODO Find out about the triple M
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    return sdf.format(date);
  }
  public static Date dateFormatUTCmilli(String date) throws ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    // TODO Find out about the double quotes
    return sdf.parse((date));
  }


  // TODO Get all these from config?
  public static String getInterfacePath() {
    return "faces/serverInterface.jsp";
  }
  public static String getBootUrl(int gameId) {
    return getTourneyUrl() + getInterfacePath() + "?action=boot&gameId=" + gameId;
  }
  public static String getBootUrl(String gameId) {
    return getTourneyUrl() + getInterfacePath() + "?action=boot&gameId=" + gameId;
  }
  public static String getPropertiesUrl(int gameId) {
    return getTourneyUrl() + "faces/properties.jsp?gameId=" + gameId;
  }
  public static String getPropertiesUrl(String gameId) {
    return getTourneyUrl() + "faces/properties.jsp?gameId=" + gameId;
  }
  public static String getPomUrl(int pomId) {
    return getTourneyUrl() + "faces/pom.jsp?pomId=" + pomId;
  }
  public static String getPomUrl(String pomId) {
    return getTourneyUrl() + "faces/pom.jsp?pomId=" + pomId;
  }
}
