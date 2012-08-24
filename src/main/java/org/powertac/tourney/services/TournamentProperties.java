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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;


/**
 * Central source of Properties read from tournament.properties
 * @author John Collins
 */
@Service("tournamentProperties")
public class TournamentProperties
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String resourceName = "/tournament.properties";
  private Properties properties = new Properties();
  private boolean loaded = false;
  private List<String> messages = new ArrayList<String>();

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
        appendExtraProperties();
        loaded = true;
      }
      catch (IOException e) {
        log.error("Failed to load " + resourceName);
      }
    }
  }

  public void addErrorMessage (String message)
  {
    if (!messages.contains(message)) {
      messages.add(message);
    }
  }

  public void removeErrorMessage (String message)
  {
    if (messages.contains(message)) {
      messages.remove(message.indexOf(message));
    }
  }

  public List<String> getErrorMessages()
  {
    // We can't do this during startup, it fails due to race conditions
    checkJenkinsLocation();

    return messages;
  }

  /**
   * Check if given properties are correct (file locations) and add some more
   */
  private void appendExtraProperties() {
    properties.setProperty("tourneyUrl", getTourneyUrl());

    String jenkinsLocation = properties.getProperty("jenkins.location",
        "http://localhost:8080/jenkins/");
    if (!jenkinsLocation.endsWith("/")) {
      properties.setProperty("jenkins.location", jenkinsLocation + "/");
    }

    String fallBack = System.getProperty("catalina.base", "") + "/";
    if (fallBack.equals("/")) {
      fallBack = "/tmp/"; // Not started via tomcat
    }

    // Check if the filelocations exist and are writeable, else replace
    checkFileLocation("pomLocation", fallBack);
    checkFileLocation("bootLocation", fallBack);
    checkFileLocation("logLocation", fallBack);
  }

  private String getTourneyUrl ()
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
    catch (Exception e) {
      e.printStackTrace();
      messages.add("Error getting Tournament Location!");
    }

    return String.format(tourneyUrl, address);
  }

  private void checkJenkinsLocation()
  {
    try {
      URL url = new URL(properties.getProperty("jenkins.location"));
      URLConnection conn = url.openConnection();
      if (conn.getInputStream() == null) {
        throw new Exception("Couldn't open Jenkins Location");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      String msg = "Jenkins Location could not be reached!";
      if (!messages.contains(msg)) {
        messages.add(msg);
      }
    }
  }

  /**
   * Make sure filelocation exists, fall back to catalina dir, we know that exists
   */
  private void checkFileLocation (String name, String catalinaBase)
  {
    String directory = properties.getProperty(name);

    if (!directory.endsWith(File.separator)) {
      directory += File.separator;
      properties.setProperty(name, directory);
    }

    File test = new File(directory);
    if (! test.exists()) {
      String msg = String.format("%s '%s' doesn't exist<br/>falling back on : %s",
          name, directory, catalinaBase);
      log.error(msg);
      messages.add(msg);
      properties.setProperty(name, catalinaBase);
    }
    else if (! test.canWrite()) {
      String msg = String.format("%s '%s' isn't writeable<br/>falling back on : %s",
          name, directory, catalinaBase);
      log.error(msg);
      messages.add(msg);
      properties.setProperty(name, catalinaBase);
    }
  }

  public static TournamentProperties getProperties ()
  {
    return (TournamentProperties) SpringApplicationContext
        .getBean("tournamentProperties");
  }
}
