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

import java.io.IOException;
import java.util.Properties;

/**
 * Central source of Properties read from tournament.properties
 * @author John Collins
 */
@Service("tournamentProperties")
public class TournamentProperties
{
  private Properties properties = new Properties();
  private boolean loaded = false;
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
      }
      catch (IOException e) {
        System.out.println("[ERROR] Failed to load " + resourceName);
      }
    }
  }
}
