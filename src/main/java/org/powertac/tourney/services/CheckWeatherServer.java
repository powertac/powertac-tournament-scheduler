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

  public CheckWeatherServer ()
  {
    TournamentProperties properties = new TournamentProperties();
    setWeatherServerLocation(properties.getProperty("weatherServerLocation"));
  }

  // TODO Make this run every 5 minutes
  public void ping ()
  {
    try {
      URL url = new URL(this.getWeatherServerLocation());
      URLConnection conn = url.openConnection();
      // Get the response
      InputStream input = conn.getInputStream();

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
