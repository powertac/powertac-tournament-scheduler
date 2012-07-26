/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

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
  public static Date dateFormatUTCmilli (String date) throws ParseException
  {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    return sdf.parse(date);
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
