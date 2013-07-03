package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Config;
import org.powertac.tourney.beans.Location;
import org.powertac.tourney.beans.Machine;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service("memStore")
public class MemStore
{
  private static Logger log = Logger.getLogger("TMLogger");

  public static ConcurrentHashMap<String, List<String>> machineIPs;
  public static ConcurrentHashMap<String, String> vizIPs;
  public static ConcurrentHashMap<String, String> localIPs;

  public static ConcurrentHashMap<Integer, List<Long>> brokerCheckins;
  public static ConcurrentHashMap<String, Long> vizCheckins;
  public static ConcurrentHashMap<Integer, String[]> gameHeartbeats;
  public static ConcurrentHashMap<Integer, Integer> gameLengths;

  public static ConcurrentHashMap<Integer, Boolean> brokerState =
      new ConcurrentHashMap<Integer, Boolean>(50, 0.9f, 1);

  public static String indexContent;

  public static ConcurrentHashMap<Integer, String> tournamentContent =
      new ConcurrentHashMap<Integer, String>(20, 0.9f, 1);

  public static List<Location> availableLocations = new ArrayList<Location>();

  public MemStore ()
  {
    machineIPs = null;
    vizIPs = null;
    localIPs = null;

    brokerCheckins = new ConcurrentHashMap<Integer, List<Long>>(50, 0.9f, 1);
    vizCheckins = new ConcurrentHashMap<String, Long>(20, 0.9f, 1);
    gameHeartbeats = new ConcurrentHashMap<Integer, String[]>(20, 0.9f, 1);
    gameLengths = new ConcurrentHashMap<Integer, Integer>(20, 0.9f, 1);
  }

  public static void getIpAddresses ()
  {
    machineIPs = new ConcurrentHashMap<String, List<String>>(20, 0.9f, 1);
    vizIPs = new ConcurrentHashMap<String, String>(20, 0.9f, 1);
    localIPs = new ConcurrentHashMap<String, String>(20, 0.9f, 1);

    for (Machine m: Machine.getMachineList()) {
      try {
        String machineIP = InetAddress.getByName(m.getMachineUrl()).toString();
        if (machineIP.contains("/")) {
          machineIP = machineIP.split("/")[1];
        }

        List<String> machine =
            Arrays.asList(m.getMachineName(), m.getMachineId().toString());
        machineIPs.put(machineIP, machine);
      } catch (UnknownHostException ignored) {
      }

      try {
        String vizIP = InetAddress.getByName(
            m.getVizUrl().split(":")[0].split("/")[0]).toString();
        if (vizIP.contains("/")) {
          vizIP = vizIP.split("/")[1];
        }
        vizIPs.put(vizIP, m.getMachineName());
      } catch (UnknownHostException ignored) {
      }
    }

    localIPs.put("127.0.0.1", "loopback");
    try {
      for (Enumeration<NetworkInterface> en =
               NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();

        if (!intf.getName().startsWith("eth") &&
            !intf.getName().startsWith("vboxnet")) {
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

  public static boolean checkMachineAllowed (String slaveAddress)
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

  public static boolean checkVizAllowed (String vizAddress)
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

  public synchronized static void addBrokerCheckin (int brokerId)
  {
    List<Long> dates = brokerCheckins.get(brokerId);
    if (dates == null) {
      dates = new ArrayList<Long>();
    }

    dates.add(System.currentTimeMillis());

    if (dates.size() > 4) {
      dates.remove(0);
    }

    brokerCheckins.put(brokerId, dates);
  }

  public synchronized static void addVizCheckin (String machineName)
  {
    vizCheckins.put(machineName, System.currentTimeMillis());
  }

  public synchronized static void addGameHeartbeat (int gameId, String message)
  {
    gameHeartbeats.put(gameId,
        new String[]{message, System.currentTimeMillis() + ""});
  }

  public synchronized static void removeGameHeartbeat (int gameId)
  {
    if (gameHeartbeats.containsKey(gameId)) {
      gameHeartbeats.remove(gameId);
    }
  }

  public synchronized static void addGameLength (int gameId, String gameLength)
  {
    try {
      gameLengths.put(gameId, Integer.parseInt(gameLength));
    } catch (Exception ignored) {
    }
  }

  public synchronized static void removeGameLength (int gameId)
  {
    if (gameLengths.containsKey(gameId)) {
      gameLengths.remove(gameId);
    }
  }

  public static boolean getBrokerState (int brokerId)
  {
    boolean enabled = true;
    try {
      enabled = MemStore.brokerState.get(brokerId);
    } catch (Exception ignored) {
    }

    return enabled;
  }

  public static void setBrokerState (int brokerId, boolean state)
  {
    brokerState.put(brokerId, state);
  }

  public static String getIndexContent ()
  {
    if (indexContent == null || indexContent.isEmpty()) {
      indexContent = Config.getIndexContent();
      if (indexContent == null) {
        return "Error connecting to DB";
      }
    }

    return indexContent;
  }

  public static boolean setIndexContent (String newContent)
  {
    indexContent = newContent;

    return Config.setIndexContent(newContent);
  }

  public static String getTournamentContent (int tournamentId)
  {
    if (tournamentContent.get(tournamentId) == null ||
        tournamentContent.get(tournamentId).isEmpty()) {
      tournamentContent.put(tournamentId,
          Config.getTournamentContent(tournamentId));
      if (tournamentContent.get(tournamentId) == null) {
        return "Error connecting to DB";
      }
    }

    return tournamentContent.get(tournamentId);
  }

  public static boolean setTournamentContent (String newContent,
                                              int tournamentId)
  {
    tournamentContent.put(tournamentId, newContent);

    return Config.setTournamentContent(newContent, tournamentId);
  }

  public static List<Location> getAvailableLocations ()
  {
    return availableLocations;
  }
  public static void setAvailableLocations (List<Location> availableLocations)
  {
    MemStore.availableLocations = availableLocations;
  }
}