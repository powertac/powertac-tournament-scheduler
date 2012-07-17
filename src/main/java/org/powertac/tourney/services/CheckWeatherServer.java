package org.powertac.tourney.services;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


@Service("checkWeatherServer")
public class CheckWeatherServer
{
  private String weatherServerLocation = "";
  private String status = "";

  // TODO Make this run every 5 minutes
  public void ping ()
  {
    try {
      URL url = new URL( getWeatherServerLocation() );
      URLConnection conn = url.openConnection();
      conn.getInputStream();

      int status = ((HttpURLConnection) conn).getResponseCode();
      if (status == 200) {
        this.setStatus("Server Alive and Well");
      }
      else {
        this.setStatus("Server is Down");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      this.setStatus("Server Timeout or Network Error");
    }
  }

  public String getWeatherServerLocation ()
  {
    if (weatherServerLocation.equals("")) {
      TournamentProperties properties = TournamentProperties.getProperties();
      setWeatherServerLocation(properties.getProperty("weatherServerLocation"));
    }

    return weatherServerLocation;
  }

  public void setWeatherServerLocation (String weatherServerLocation)
  {
    this.weatherServerLocation = weatherServerLocation;
  }

  public String getStatus ()
  {
    return status;
  }

  public void setStatus (String status)
  {
    this.status = status;
  }
}
