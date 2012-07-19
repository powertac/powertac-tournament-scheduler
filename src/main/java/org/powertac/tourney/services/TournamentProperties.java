/*
 * Copyright (c) 2012 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.tourney.services;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import static org.powertac.tourney.services.Utils.log;

/**
 * Central source of Properties read from tournament.properties
 * @author John Collins
 */
@Service("tournamentProperties")
public class TournamentProperties
{
  private Properties properties = new Properties();
  private boolean loaded = false;
  private List<String> messages = new ArrayList<String>();
  private String resourceName = "/tournament.properties";

  // delegate to props
  public String getProperty (String key)
  {
    loadIfNecessary();
    return properties.getProperty(key);
  }

  public String getProperty (String key, String defaultValue)
  {
    loadIfNecessary();
    return properties.getProperty(key, defaultValue);
  }

  // lazy loader
  private void loadIfNecessary ()
  {
    if (!loaded) {
      try {
        properties.load(TournamentProperties.class.getClassLoader()
                   .getResourceAsStream(resourceName));
        loaded = true;

        checkProperties();
      }
      catch (IOException e) {
        log("[ERROR] Failed to load {0}" + resourceName);
      }
    }
  }

  public List<String> getConfigErrors ()
  {
    return messages;
  }

  /**
   * Check if given properties are correct (file locations) and add some more
   */
  private void checkProperties() {
    properties.put("tourneyUrl", getTourneyUrl());
    properties.put("jenkinsUrl", getJenkinsUrl());

    String fallBack = System.getProperty("catalina.base", "") + "/";
    if (fallBack.equals("/")) {
      fallBack = "/tmp/"; // Not started via tomcat
    }

    // Check if the filelocations exist and are writeable, else replace
    checkFileLocation("pomLocation", fallBack);
    checkFileLocation("bootLocation", fallBack);
    checkFileLocation("logLocation", fallBack);
  }

  private static String getTourneyUrl ()
  {
    // TODO Get these from tournament.properties ??
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
    // TODO Get this from tournament.properties ??
    String jenkinsUrl = "http://localhost:8080/jenkins/";

    return jenkinsUrl;
  }

  /**
   * Make sure filelocation exists, fall back to catalina dir, we know that exists
   */
  private void checkFileLocation(String name, String catalinaBase) {
    String directory = properties.getProperty(name);

    if (!directory.endsWith(File.separator)) {
      directory += File.separator;
      properties.setProperty(name, directory);
    }

    File test = new File(directory);
    if (! test.exists()) {
      String msg = String.format("%s '%s' doesn't exist<br/>falling back on : %s",
          name, directory, catalinaBase);
      log("[ERROR] {0}", msg);
      messages.add(msg);
      properties.setProperty(name, catalinaBase);
    }
    else if (! test.canWrite()) {
      String msg = String.format("%s '%s' isn't writeable<br/>falling back on : %s",
          name, directory, catalinaBase);
      log("[ERROR] {0}", msg);
      messages.add(msg);
      properties.setProperty(name, catalinaBase);
    }
  }

  public static TournamentProperties getProperties() {
    return (TournamentProperties) SpringApplicationContext
        .getBean("tournamentProperties");
  }
}
