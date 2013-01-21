package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


@ManagedBean
@Service("checkWeatherServer")
public class CheckWeatherServer implements InitializingBean
{
  private static Logger log = Logger.getLogger("TMLogger");

  @Autowired
  private TournamentProperties properties;

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

  public void ping ()
  {
    log.info("Checking WeatherService");
    InputStream is = null;
    try {
      URL url = new URL(getWeatherServerLocation());
      URLConnection conn = url.openConnection();
      is = conn.getInputStream();

      int status = ((HttpURLConnection) conn).getResponseCode();
      if (status == 200) {
        setStatus("Server Alive and Well");
        log.info("Server Alive and Well");
        mailed = true;
      } else {
        setStatus("Server is Down");
        log.info("Server is Down");

        if (!mailed) {
          String msg = "It seems the WeatherServer is down";
          Utils.sendMail("WeatherServer is Down", msg,
              properties.getProperty("scheduler.mailRecipient"));
          mailed = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      setStatus("Server Timeout or Network Error");
      log.info("Server Timeout or Network Error");

      if (!mailed) {
        String msg = "Server Timeour or Network Error during Weather Server ping";
        Utils.sendMail("WeatherServer Timeout or Network Error", msg,
            properties.getProperty("scheduler.mailRecipient"));
        mailed = true;
      }
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  @PreDestroy
  private void cleanUp () throws Exception
  {
    log.info("Spring Container is destroyed! CheckWeatherServer clean up");

    if (weatherServerCheckerTimer != null) {
      weatherServerCheckerTimer.cancel();
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
