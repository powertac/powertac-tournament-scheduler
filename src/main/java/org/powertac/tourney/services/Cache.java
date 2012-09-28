/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 9/25/12
 * Time: 9:46 AM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;


@Service("cache")
public class Cache {
  private static Logger log = Logger.getLogger("TMLogger");

  public static HashMap<String, List<String>> machineIPs;
  public static HashMap<String, String> vizIPs;
  public static HashMap<String, String> localIPs;

  public static HashMap<Integer, List<Long>> brokerLogins;
  public static HashMap<Integer, String[]> gameHeartbeats;

  private static List<Integer> checkedBootstraps;
  private static List<Integer> checkedSims;

  public Cache ()
  {
    brokerLogins = new HashMap<Integer, List<Long>>();
    gameHeartbeats = new HashMap<Integer, String[]>();

    machineIPs = null;
    vizIPs = null;
    localIPs = null;

    checkedBootstraps = new ArrayList<Integer>();
    checkedSims = new ArrayList<Integer>();
  }

  public synchronized static void addBrokerLogin (int brokerId)
  {
    List<Long> dates = brokerLogins.get(brokerId);
    if (dates == null) {
      dates = new ArrayList<Long>();
    }

    dates.add(System.currentTimeMillis());

    if (dates.size() > 4) {
      dates.remove(0);
    }

    brokerLogins.put(brokerId, dates);
  }

  public synchronized static void removeBrokerLogin (int brokerId)
  {
    if (brokerLogins.containsKey(brokerId)) {
      brokerLogins.remove(brokerId);
    }
  }

  public synchronized static void addGameHeartbeat (int gameId, String message)
  {
    gameHeartbeats.put(gameId,
        new String[] {message, System.currentTimeMillis()+""});
  }

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
    //log.debug("Testing checkMachineAllowed : " + slaveAddress);

    if (machineIPs == null) {
      getIpAddresses();
    }

    assert localIPs != null;
    if (localIPs.containsKey(slaveAddress)) {
      //log.debug("Localhost is always allowed");
      return true;
    }

    assert machineIPs != null;
    if (machineIPs.containsKey(slaveAddress)) {
      //log.debug(slaveAddress + " is allowed");
      return true;
    }

    log.debug(slaveAddress + " is not allowed");
    return false;
  }

  public static boolean checkVizAllowed(String vizAddress)
  {
    //log.debug("Testing checkVizAllowed : " + vizAddress);

    if (vizIPs == null) {
      getIpAddresses();
    }

    assert localIPs != null;
    if (localIPs.containsKey(vizAddress)) {
      //log.debug("Localhost is always allowed");
      return true;
    }

    assert vizIPs != null;
    if (vizIPs.containsKey(vizAddress)) {
      //log.debug(vizAddress + " is allowed");
      return true;
    }

    log.debug(vizAddress + " is not allowed");
    return true;
  }

  public static void addBootstrap (Game game) {

    // Make sure no more than 1 email per wedged boot
    if (checkedBootstraps.contains(game.getGameId())) {
      return;
    }

    TournamentProperties properties = TournamentProperties.getProperties();

    long wedgedDeadline = Integer.parseInt(
        properties.getProperty("scheduler.bootstrapWedged", "900000"));
    long nowStamp = Utils.offsetDate().getTime();

    long diff = nowStamp - game.getReadyTime().getTime();
    if (diff > wedgedDeadline) {
      checkedBootstraps.add(game.getGameId());

      String msg = String.format(
          "Bootstrapping of game %s seems to take too long : %s seconds",
          game.getGameId(), (diff / 1000));
      log.error(msg);
      Utils.sendMail("Bootstrap seems stuck", msg,
          properties.getProperty("scheduler.mailRecipient"));
      properties.addErrorMessage(msg);
    }
  }

  public static void removeBootstrap (int gameId) {
    if (checkedBootstraps.contains(gameId)) {
      int index = checkedBootstraps.indexOf(gameId);
      checkedBootstraps.remove(index);
    }
  }

  public static void addSim (Game game) {

    // Make sure no more than 1 email per wedged sim
    if (checkedSims.contains(game.getGameId())) {
      return;
    }

    TournamentProperties properties = TournamentProperties.getProperties();

    long wedgedSimDeadline = Integer.parseInt(
        properties.getProperty("scheduler.simWedged", "10800000"));
    long wedgedTestDeadline = Integer.parseInt(
        properties.getProperty("scheduler.simTestWedged", "2700000"));
    long nowStamp = Utils.offsetDate().getTime();

    long wedgedDeadline;
    if (game.getTournament().getTournamentName().toLowerCase().contains("test")) {
      wedgedDeadline = wedgedTestDeadline;
    } else {
      wedgedDeadline = wedgedSimDeadline;
    }

    long diff = nowStamp - game.getReadyTime().getTime();
    if (diff > wedgedDeadline) {
      checkedSims.add(game.getGameId());

      String msg = String.format(
          "Sim of game %s seems to take too long : %s seconds",
          game.getGameId(), (diff / 1000));
      log.error(msg);
      Utils.sendMail("Sim seems stuck", msg,
          properties.getProperty("scheduler.mailRecipient"));
      properties.addErrorMessage(msg);
    }
  }

  public static void removeSim (int gameId) {
    if (checkedSims.contains(gameId)) {
      int index = checkedSims.indexOf(gameId);
      checkedSims.remove(index);
    }
  }
}