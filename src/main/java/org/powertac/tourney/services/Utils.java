/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class Utils {
  private static Logger log = Logger.getLogger("TMLogger");

  public static boolean checkSlaveAllowed(String slaveAddress)
  {
    // TODO Only allow access to slave, defined in db.machines
    log.debug("TODO Testing checkSlaveAllowed : " + slaveAddress);

    if (slaveAddress.equals("127.0.0.1")) {
      return true;
    }

    return true;
  }

  public static boolean checkVizAllowed(String vizAddress)
  {
    // TODO Only allow access to slave, defined in db.machines
    log.debug("TODO Testing checkVizAllowed : " + vizAddress);

    if (vizAddress.equals("127.0.0.1")) {
      return true;
    }

    return true;
  }

  //<editor-fold desc="Date format">
  public static String dateFormat (Date date)
  {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }
  public static String dateFormatUTC (Date date)
  {
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }
  public static Date dateFormatUTCmilli (String date)
  {
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
      return sdf.parse(date);
    }
    catch (Exception e) {
      return null;
    }
  }
  public static String dateFormatUTCmilli (Date date)
  {
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }
  //</editor-fold>
}
