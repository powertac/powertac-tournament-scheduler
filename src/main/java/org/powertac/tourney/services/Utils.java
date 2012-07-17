/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class Utils {
  public static boolean checkClientAllowed (String clientAddress)
  {
    // TODO Only allow access to slave, defined in db.machines
    System.out.println("[DEBUG] Testing checkClientAllowed");
    System.out.println(clientAddress);

    if (clientAddress.equals("127.0.0.1")) {
      return true;
    }

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

    String catalinaBase = System.getProperty("catalina.base");
    if (!catalinaBase.endsWith(File.separator)) {
      catalinaBase += File.separator;
    }
    String tmLogFile = catalinaBase + "logs" + File.separator + "tournament.out";

    try {
      FileWriter fstream = new FileWriter(tmLogFile, true);
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
