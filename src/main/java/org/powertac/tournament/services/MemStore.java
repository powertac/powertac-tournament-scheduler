package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.powertac.tournament.beans.Config;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.Machine;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.powertac.tournament.services.Forecaster.Forecast;


@Component("memStore")
public class MemStore
{
  private static Logger log = Utils.getLogger();

  private static ConcurrentHashMap<String, List<String>> machineIPs;
  private static ConcurrentHashMap<String, String> vizIPs;
  private static ConcurrentHashMap<String, String> localIPs;

  private static ConcurrentHashMap<Integer, List<Long>> brokerCheckins;
  private static ConcurrentHashMap<String, Long> vizCheckins;
  private static ConcurrentHashMap<String, String> machineLoads;
  private static ConcurrentHashMap<Integer, String[]> gameHeartbeats;
  private static ConcurrentHashMap<Integer, Integer> gameLengths;
  private static ConcurrentHashMap<Integer, Long> elapsedTimes;

  private static ConcurrentHashMap<Integer, Boolean> brokerState;
  private static boolean hideInactiveBrokers = true;
  private static boolean hideInactiveGames = true;
  private static List<Location> availableLocations;

  private static String indexContent;
  private static ConcurrentHashMap<Integer, String> tournamentContent;

  private static ConcurrentHashMap<Integer, Forecast> forecasts;

  public MemStore ()
  {
    machineIPs = null;
    vizIPs = null;
    localIPs = null;

    brokerCheckins = new ConcurrentHashMap<Integer, List<Long>>(50, 0.9f, 1);
    vizCheckins = new ConcurrentHashMap<String, Long>(20, 0.9f, 1);
    machineLoads = new ConcurrentHashMap<String, String>(20, 0.9f, 1);
    gameHeartbeats = new ConcurrentHashMap<Integer, String[]>(20, 0.9f, 1);
    gameLengths = new ConcurrentHashMap<Integer, Integer>(20, 0.9f, 1);
    elapsedTimes = new ConcurrentHashMap<Integer, Long>();

    brokerState = new ConcurrentHashMap<Integer, Boolean>(50, 0.9f, 1);
    availableLocations = new ArrayList<Location>();

    tournamentContent = new ConcurrentHashMap<Integer, String>(20, 0.9f, 1);

    forecasts = new ConcurrentHashMap<Integer, Forecast>(20, 0.9f, 1);
  }

  //<editor-fold desc="IP stuff">
  public static void getIpAddresses ()
  {
    machineIPs = new ConcurrentHashMap<String, List<String>>(20, 0.9f, 1);
    vizIPs = new ConcurrentHashMap<String, String>(20, 0.9f, 1);
    localIPs = new ConcurrentHashMap<String, String>(20, 0.9f, 1);

    for (Machine m : Machine.getMachineList()) {
      try {
        String machineIP = InetAddress.getByName(m.getMachineUrl()).toString();
        if (machineIP.contains("/")) {
          machineIP = machineIP.split("/")[1];
        }

        List<String> machine =
            Arrays.asList(m.getMachineName(), m.getMachineId().toString());
        machineIPs.put(machineIP, machine);
      }
      catch (UnknownHostException ignored) {
      }

      try {
        String vizIP = InetAddress.getByName(
            m.getVizUrl().split(":")[0].split("/")[0]).toString();
        if (vizIP.contains("/")) {
          vizIP = vizIP.split("/")[1];
        }
        vizIPs.put(vizIP, m.getMachineName());
      }
      catch (UnknownHostException ignored) {
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
    }
    catch (SocketException e) {
      log.error(" (error retrieving network interface list)");
    }
  }

  public static void resetMachineIPs ()
  {
    machineIPs = null;
  }

  public static boolean checkMachineAllowed (String slaveAddress)
  {
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

  public static void resetVizIPs ()
  {
    vizIPs = null;
  }

  public static boolean checkVizAllowed (String vizAddress)
  {
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
  //</editor-fold>

  //<editor-fold desc="Checkin stuff">
  public static ConcurrentHashMap<Integer, List<Long>> getBrokerCheckins ()
  {
    return brokerCheckins;
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

  public static void removeBrokerCheckin (int brokerId, long checkin)
  {
    brokerCheckins.get(brokerId).remove(checkin);
  }

  public static ConcurrentHashMap<String, Long> getVizCheckins ()
  {
    return vizCheckins;
  }

  public synchronized static void addVizCheckin (String machineName)
  {
    vizCheckins.put(machineName, System.currentTimeMillis());
  }

  public static void removeVizCheckin (String machineName)
  {
    vizCheckins.remove(machineName);
  }

  public static ConcurrentHashMap<String, String> getMachineLoads ()
  {
    return machineLoads;
  }

  public synchronized static void addMachineLoad (String machineName,
                                                  String load)
  {
    machineLoads.put(machineName, load);
  }

  public static void removeMachineLoad (String machineName)
  {
    machineLoads.remove(machineName);
  }

  public static ConcurrentHashMap<Integer, String[]> getGameHeartbeats ()
  {
    return gameHeartbeats;
  }

  public synchronized static void addGameHeartbeat (int gameId, String message)
  {
    gameHeartbeats.put(gameId,
        new String[]{message, System.currentTimeMillis() + ""});
  }

  public static ConcurrentHashMap<Integer, Long> getElapsedTimes ()
  {
    return elapsedTimes;
  }

  public synchronized static void addElapsedTime (int gameId, long elapsedtime)
  {
    elapsedTimes.put(gameId, elapsedtime);
  }

  public static ConcurrentHashMap<Integer, Integer> getGameLengths ()
  {
    return gameLengths;
  }

  public synchronized static void addGameLength (int gameId, String gameLength)
  {
    try {
      gameLengths.put(gameId, Integer.parseInt(gameLength) +
          TournamentProperties.getProperties().getPropertyInt("bootLength"));
    }
    catch (Exception ignored) {
    }
  }

  public synchronized static void removeGameInfo (int gameId)
  {
    gameHeartbeats.remove(gameId);
    gameLengths.remove(gameId);
    elapsedTimes.remove(gameId);
  }
  //</editor-fold>

  //<editor-fold desc="Interface stuff">
  public static boolean getBrokerState (int brokerId)
  {
    boolean enabled = true;
    try {
      enabled = MemStore.brokerState.get(brokerId);
    }
    catch (Exception ignored) {
    }

    return enabled;
  }
  public static void setBrokerState (int brokerId, boolean state)
  {
    brokerState.put(brokerId, state);
  }

  public static boolean isHideInactiveBrokers ()
  {
    return hideInactiveBrokers;
  }
  public static void setHideInactiveBrokers (boolean hideInactiveBrokers)
  {
    MemStore.hideInactiveBrokers = hideInactiveBrokers;
  }

  public static boolean isHideInactiveGames ()
  {
    return hideInactiveGames;
  }
  public static void setHideInactiveGames (boolean hideInactiveGames)
  {
    MemStore.hideInactiveGames = hideInactiveGames;
  }

  public static List<Location> getAvailableLocations ()
  {
    return availableLocations;
  }
  public static void setAvailableLocations (List<Location> availableLocations)
  {
    MemStore.availableLocations = availableLocations;
  }
  //</editor-fold>

  //<editor-fold desc="Content stuff">
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
  //</editor-fold>

  //<editor-fold desc="Forecaster stuff">
  public static List<Integer> getForecastLengths (int roundId)
  {
    Forecast forecast = forecasts.get(roundId);

    List<Integer> result = new ArrayList<Integer>();
    for (Game game : forecast.getGamesMap().values()) {
      result.add(game.getGameLength());
    }

    return result;
  }

  public static Forecast getForecast (int roundId)
  {
    return forecasts.get(roundId);
  }

  public static void setForecast (int roundId, Forecast forecast)
  {
    forecasts.put(roundId, forecast);
  }
  //</editor-fold>
}