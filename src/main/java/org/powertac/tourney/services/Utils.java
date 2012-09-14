/**
 * Created by IntelliJ IDEA.
 * User: Govert Buijs
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Machine;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Utils {
  private static Logger log = Logger.getLogger("TMLogger");

  public static HashMap<String, List<String>> machineIPs = null;
  public static HashMap<String, String> vizIPs = null;
  public static HashMap<String, String> localIPs = null;

  public static void getIpAddresses ()
  {
    machineIPs = new HashMap<String, List<String>>();
    vizIPs = new HashMap<String, String>();
    localIPs = new HashMap<String, String>();

    for (Machine m: Machine.getMachineList()) {
      try {
        String machineIP = InetAddress.getByName(m.getMachineUrl()).toString();
        if (machineIP.contains("/")) {
          machineIP = machineIP.split("/")[1];
        }

        List<String> machine =
            Arrays.asList(m.getMachineName(), m.getMachineId().toString());
        machineIPs.put(machineIP, machine);
      }
      catch (UnknownHostException ignored) {}

      try {
        String vizIP = InetAddress.getByName(
            m.getVizUrl().split(":")[0].split("/")[0]).toString();
        if (vizIP.contains("/")) {
          vizIP = vizIP.split("/")[1];
        }
        vizIPs.put(vizIP, m.getMachineName());
      }
      catch (UnknownHostException ignored) {}
    }

    localIPs.put("127.0.0.1", "loopback");
    try {
      for (Enumeration<NetworkInterface> en =
               NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();

        if (!intf.getName().startsWith("eth")) {
          continue;
        }

        for (Enumeration<InetAddress> enumIpAddr =
                 intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          String ip = enumIpAddr.nextElement().toString();
          if (ip.contains(":")) {
            continue;
          }
          if (ip.contains("/")) {
            ip = ip.split("/")[1];
          }
          localIPs.put(ip, intf.getName());
        }
      }
    } catch (SocketException e) {
      log.error(" (error retrieving network interface list)");
    }
  }

  public static boolean checkMachineAllowed(String slaveAddress)
  {
    log.debug("Testing checkMachineAllowed : " + slaveAddress);

    if (machineIPs == null) {
      getIpAddresses();
    }

    assert localIPs != null;
    if (localIPs.containsKey(slaveAddress)) {
      log.debug("Localhost is always allowed");
      return true;
    }

    assert machineIPs != null;
    if (machineIPs.containsKey(slaveAddress)) {
      log.debug(slaveAddress + " is allowed");
      return true;
    }

    log.debug(slaveAddress + " is not allowed");
    return false;
  }

  public static boolean checkVizAllowed(String vizAddress)
  {
    log.debug("Testing checkVizAllowed : " + vizAddress);

    if (vizIPs == null) {
      getIpAddresses();
    }

    assert localIPs != null;
    if (localIPs.containsKey(vizAddress)) {
      log.debug("Localhost is always allowed");
      return true;
    }

    assert vizIPs != null;
    if (vizIPs.containsKey(vizAddress)) {
      log.debug(vizAddress + " is allowed");
      return true;
    }

    log.debug(vizAddress + " is not allowed");
    return true;
  }

  public static int getMachineId(String machineAddress)
  {
    log.debug("Getting machineName for : " + machineAddress);

    if (machineIPs == null) {
      getIpAddresses();
    }

    assert machineIPs != null;
    return Integer.parseInt(machineIPs.get(machineAddress).get(1));
  }

  public static void secondsSleep (int seconds)
  {
    try {
      Thread.sleep(seconds * 1000);
    }
    catch(Exception ignored){}
  }

  public static void sendMail (String sub, String msg, String recipient)
  {
    TournamentProperties properties = TournamentProperties.getProperties();
    final String username = properties.getProperty("gmail.username");
    final String password = properties.getProperty("gmail.password");

    if (username.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
      return;
    }

    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");

    Session session = Session.getInstance(props,
        new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
          }
        });

    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      message.setRecipients(Message.RecipientType.TO,
          InternetAddress.parse(recipient));
      message.setSubject(sub);
      message.setText(msg);

      Transport.send(message);

      log.info("Done sending mail to : " + recipient);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void redirect ()
  {
    try {
      ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
      externalContext.redirect("index.xhtml");
    }
    catch (Exception ignored) {}
  }

  private static Random queueGenerator = new Random(new Date().getTime());
  public static String createQueueName ()
  {
    return Long.toString(queueGenerator.nextLong(), 31);
  }

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

  public static Date offsetDate ()
  {
    return offsetDate(new Date());
  }

  public static Date offsetDate (Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    if (TimeZone.getDefault().inDaylightTime( date )) {
      calendar.add(Calendar.HOUR, -1);
    }
    TimeZone tz = Calendar.getInstance().getTimeZone();
    calendar.add(Calendar.MILLISECOND, -1 * tz.getRawOffset());

    return calendar.getTime();
  }
}
