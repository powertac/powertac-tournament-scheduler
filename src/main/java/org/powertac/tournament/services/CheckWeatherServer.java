package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.powertac.tournament.beans.Location;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


@ManagedBean
@Service("checkWeatherServer")
public class CheckWeatherServer implements InitializingBean
{
  private static Logger log = Utils.getLogger();

  @Autowired
  private Properties properties;

  private String weatherServerLocation = "";
  private static String status = "";
  private Timer weatherServerCheckerTimer = null;
  private boolean mailed;

  public CheckWeatherServer ()
  {
    super();
  }

  public void afterPropertiesSet () throws Exception
  {
    lazyStart();
  }

  private void lazyStart ()
  {
    loadExtraLocations();

    // Check the status of the weather server every 15 minutes
    TimerTask weatherServerChecker = new TimerTask()
    {
      @Override
      public void run ()
      {
        ping();
      }
    };

    weatherServerCheckerTimer = new Timer();
    weatherServerCheckerTimer.schedule(weatherServerChecker, new Date(), 900000);
  }

  private void ping ()
  {
    log.info("Checking WeatherService");
    InputStream is = null;
    try {
      HttpURLConnection conn = getConnection("");
      is = conn.getInputStream();

      int status = conn.getResponseCode();
      if (status == 200) {
        setStatus("Server Alive and Well");
        log.info("Server Alive and Well");
        mailed = true;
      }
      else {
        setStatus("Server is Down");
        log.info("Server is Down");

        if (!mailed) {
          String msg = "It seems the WeatherServer is down";
          Utils.sendMail("WeatherServer is Down", msg,
              properties.getProperty("scheduler.mailRecipient"));
          mailed = true;
        }
      }
    }
    catch (Exception e) {
      setStatus("Server Timeout or Network Error");
      log.info("Server Timeout or Network Error");

      if (!mailed) {
        String msg = "Server Timeour or Network Error during Weather Server ping";
        Utils.sendMail("WeatherServer Timeout or Network Error", msg,
            properties.getProperty("scheduler.mailRecipient"));
        mailed = true;
      }
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void loadExtraLocations ()
  {
    // Check the available weather locations at startup
    try {
      HttpURLConnection conn = getConnection("?type=showLocations");

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder docB = dbf.newDocumentBuilder();
      Document doc = docB.parse(conn.getInputStream());
      NodeList nodeList = doc.getElementsByTagName("location");

      List<Location> availableLocations = new ArrayList<>();
      for (int i = 0; i < nodeList.getLength(); i++) {
        try {
          Element element = (Element) nodeList.item(i);
          Location location = new Location();
          location.setLocation(element.getAttribute("name"));
          location.setDateFrom(
              Utils.stringToDateMedium(element.getAttribute("minDate")));
          location.setDateTo(
              Utils.stringToDateMedium(element.getAttribute("maxDate")));
          availableLocations.add(location);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      MemStore.setAvailableLocations(availableLocations);
    }
    catch (Exception e) {
      log.info("Server Timeout or Network Error");
    }
  }

  private HttpURLConnection getConnection (String path) throws IOException
  {
    URL url = new URL(getWeatherServerLocation() + path);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    HttpURLConnection.setFollowRedirects(false);
    conn.setConnectTimeout(10 * 1000);
    conn.connect();

    return conn;
  }

  @PreDestroy
  private void cleanUp () throws Exception
  {
    log.info("Spring Container is destroyed! CheckWeatherServer clean up");

    if (weatherServerCheckerTimer != null) {
      weatherServerCheckerTimer.cancel();
      weatherServerCheckerTimer.purge();
      weatherServerCheckerTimer = null;
      log.info("Stopping weatherServerCheckerTimer ...");
    }
  }

  //<editor-fold desc="Setters and Getters">
  public String getWeatherServerLocation ()
  {
    if (weatherServerLocation.equals("")) {
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

  public void setStatus (String newStatus)
  {
    status = newStatus;
  }
  //</editor-fold>
}
