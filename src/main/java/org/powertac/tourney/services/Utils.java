/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;


public class Utils {
  private static String tmLogFile =
      System.getProperty("catalina.base") + "/logs/tournament.out";

  public static String getTourneyUrl ()
  {
    // TODO Get these from Properties ??
    String tourneyUrl = "http://%s:8080/TournamentScheduler/";
    String address = "127.0.0.1";
    try {
      Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
      while (n.hasMoreElements()) {
        NetworkInterface e = n.nextElement();
        if (e.getName().startsWith("lo")) {
          continue;
        }

        Enumeration<InetAddress> a = e.getInetAddresses();
        while (a.hasMoreElements()) {
          InetAddress addr = a.nextElement();
          if (addr.getClass().getName().equals("java.net.Inet4Address")) {
            address = addr.getHostAddress();
          }
        }
      }
    }
    catch (Exception ignored) {}

    return String.format(tourneyUrl, address);
  }

  public static String getJenkinsUrl ()
  {
    // TODO Get this from Properties ??
    String jenkinsUrl = "http://localhost:8080/jenkins/";

    return jenkinsUrl;
  }

  public static boolean checkClientAllowed (String clientAddress)
  {
    // TODO Only allow access to slave, defined in db.machines

    return true;
  }


  //<editor-fold desc="Date format">
  public static String dateFormat (Date date)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return sdf.format(date);
  }
  public static String dateFormatUTC (Date date)
  {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return sdf.format(date);
  }
  public static Date dateFormatUTCmilli (String date) throws ParseException
  {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    return sdf.parse(date);
  }
  //</editor-fold>

  //<editor-fold desc="Logging">
  public static void log (String base)
  {
    System.out.println(base);

    try {
      FileWriter fstream = new FileWriter(Utils.tmLogFile, true);
      BufferedWriter out = new BufferedWriter(fstream);
      out.write(base + "\n");
      out.close();
    }
    catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }
  public static void log (String base, Object arg0)
  {
    Object[] args = {arg0};
    MessageFormat fmt = new MessageFormat(base);

    log(fmt.format(args));
  }
  public static void log (String base, Object arg0, Object arg1)
  {
    Object[] args = {arg0, arg1};
    MessageFormat fmt = new MessageFormat(base);

    log(fmt.format(args));
  }
  public static void log (String base, Object[] args)
  {
    MessageFormat fmt = new MessageFormat(base);
    log(fmt.format(args));
  }
  //</editor-fold>
}
