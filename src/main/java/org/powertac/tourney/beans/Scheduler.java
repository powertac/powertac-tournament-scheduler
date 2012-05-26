package org.powertac.tourney.beans;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PreDestroy;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.RunBootstrap;
import org.powertac.tourney.services.RunGame;
import org.powertac.tourney.services.SpringApplicationContext;
import org.powertac.tourney.services.TournamentProperties;
import org.springframework.stereotype.Service;

@Service("scheduler")
public class Scheduler
{

  
  private TournamentProperties tournamentProperties;
  
  public static final String key = "scheduler";
  public static boolean running = false;
  public boolean multigame = false;

  public boolean bootrunning = false;

  private Timer watchDogTimer = null;
  SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");

  private HashMap<Integer, Integer> AgentIdToBrokerId =
    new HashMap<Integer, Integer>();
  private HashMap<Integer, Integer> ServerIdToMachineId =
    new HashMap<Integer, Integer>();


  private HashMap<Integer, Timer> bootToBeRun = new HashMap<Integer, Timer>();
  private HashMap<Integer, Timer> simToBeRun = new HashMap<Integer, Timer>();

  public static String getKey ()
  {
    return key;
  }
  
  public boolean isRunning(){
    return watchDogTimer != null;
  }
  

  @PreDestroy
  public void cleanUp () throws Exception
  {
    System.out
            .println("[INFO] Spring Container is destroyed! Scheduler clean up");
    if (watchDogTimer != null) {
      watchDogTimer.cancel();
    }
    for (Timer t: bootToBeRun.values()) {
      if (t != null) {
        t.cancel();
      }
    }
    for (Timer t: simToBeRun.values()) {
      if (t != null) {
        t.cancel();
      }
    }

  }

  public Scheduler ()
  {

    dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    //this.startWatchDog();
    lazyStart();
  }
  
  public void lazyStart(){
    Timer t = new Timer();
    TimerTask tt = new TimerTask() {

      @Override
      public void run ()
      {
        Scheduler.this.tournamentProperties = (TournamentProperties) SpringApplicationContext.getBean("tournamentProperties");
        Scheduler.this.startWatchDog();
      
      }
      
    };
    t.schedule(tt, 3000); 
  }

  public synchronized void startWatchDog ()
  {
    if (!running) {
      running = true;
      
      Timer t = new Timer();
      TimerTask watchDog = new TimerTask() {
        Database db;
        
        @Override
        public void run ()
        {
          // Run watchDog
          db = new Database();
          checkForSims();
          checkForBoots();
        }

        public void checkForSims ()
        {
          System.out.println("[INFO] " + dateFormatUTC.format(new Date())
                             + " : WatchDogTimer Looking for Games To Start..");
          // Check Database for startable games
          try {
            //db.openConnection();
            db.startTrans();
            List<Game> games = db.getStartableGames();
            System.out.println("[INFO] WatchDogTimer reports " + games.size()
                               + " game(s) are ready to start");

            String hostip = "http://";

            try {
              InetAddress thisIp = InetAddress.getLocalHost();
              hostip += thisIp.getHostAddress() + ":8080";
            }
            catch (UnknownHostException e2) {
              e2.printStackTrace();
            }

            for (Game g: games) {
              Tournament t = db.getTournamentByGameId(g.getGameId());
              System.out.println("[INFO] " + dateFormatUTC.format(new Date())
                                 + " : Game: " + g.getGameId()
                                 + " will be started...");
              Scheduler.this
                      .runSimTimer(g.getGameId(),
                                   new RunGame(
                                               g.getGameId(),
                                               hostip + "/TournamentScheduler/",
                                               t.getPomUrl(),
                                               tournamentProperties.getProperty("destination")),
                                   new Date());
            }
            db.commitTrans();
          }
          catch (SQLException e) {
            db.abortTrans();
            this.cancel();
            e.printStackTrace();
          }
        }

        public void checkForBoots ()
        {

          if (!bootrunning) {
            
            System.out
                    .println("[INFO] "
                             + dateFormatUTC.format(new Date())
                             + " : WatchDogTimer Looking for Bootstraps To Start..");
            // Check Database for startable games
            try {
              db.startTrans();
              List<Game> games = db.getBootableGames();
              System.out.println("[INFO] WatchDogTimer reports " + games.size()
                                 + " boots are ready to start");

              String hostip = "http://";

              try {
                InetAddress thisIp = InetAddress.getLocalHost();
                hostip += thisIp.getHostAddress() + ":8080";
              }
              catch (UnknownHostException e2) {
                e2.printStackTrace();
              }
              
              if (games.size()>0){
                bootrunning = true;
                Game g = games.get(0);
                
                Tournament t = db.getTournamentByGameId(g.getGameId());
                //db.closeConnection();
                
  
                System.out.println("[INFO] " + dateFormatUTC.format(new Date())
                                   + " : Boot: " + g.getGameId()
                                   + " will be started...");
  
                Scheduler.this.runBootTimer(g.getGameId(),
                              new RunBootstrap(
                                               g.getGameId(),
                                               hostip + "/TournamentScheduler/",
                                               t.getPomUrl(),
                                               tournamentProperties.getProperty("destination"),
                                               tournamentProperties.getProperty("bootserverName")),
                              new Date());
                
                
                
                  
              }
              db.commitTrans();
            }
            catch (SQLException e) {
              this.cancel();
              db.abortTrans();
              e.printStackTrace();
            }

          }
          else {
            System.out.println("[INFO] " + dateFormatUTC.format(new Date())
                               + " : WatchDogTimer Reports a boot is running");

            Database db = new Database();
            List<Game> games = new ArrayList<Game>();
            try {
              db.startTrans();
              games = db.getBootableGames();
              //db.closeConnection();
            }
            catch (SQLException e) {
              db.abortTrans();
              e.printStackTrace();
            }
            System.out.println("[INFO] WatchDogTimer reports " + games.size()
                               + " boot(s) are ready to start");

          }
        }
      };

      System.out.println("[INFO] " + dateFormatUTC.format(new Date())
                         + " : Starting WatchDog...");
      
     
      long watchDogInt = Integer.parseInt(tournamentProperties.getProperty("scheduler.watchDogInterval", "120000"));
      
      t.schedule(watchDog, new Date(), watchDogInt);

      this.watchDogTimer = t;

    }
    else {
      System.out.println("[WARN] Watchdog already running");
    }
  }

  public void restartWatchDog ()
  {
    this.stopWatchDog();
    this.startWatchDog();

  }

  public void stopWatchDog ()
  {
    if (watchDogTimer != null) {
      watchDogTimer.cancel();
      running = false;
      System.out.println("[INFO] " + dateFormatUTC.format(new Date())
                         + " : Stopping WatchDog...");
    }
    else {
      System.out.println("[WARN] " + dateFormatUTC.format(new Date())
                         + " : WatchDogTimer Already Stopped");
    }
  }

  public void runBootTimer (int gameId, TimerTask t, Date time)
  {
    Timer timer = new Timer();
    timer.schedule(t, time);
    bootToBeRun.put(gameId, timer);
  }

  public void runSimTimer (int gameId, TimerTask t, Date time)
  {
    Timer timer = new Timer();
    timer.schedule(t, time);
    simToBeRun.put(gameId, timer);
  }

  public void deleteSimTimer (int gameId)
  {
    Timer t = simToBeRun.get(gameId);
    if (t != null) {
      t.cancel();
      simToBeRun.remove(gameId);
    }
    else {
      System.out.println("Timer thread is null for game: " + gameId);
    }
  }

  public void deleteBootTimer (int gameId)
  {
    Timer t = bootToBeRun.get(gameId);
    if (t != null) {
      t.cancel();
      bootToBeRun.remove(gameId);
    }
    else {
      System.out.println("Timer thread is null for game: " + gameId);
    }
  }

}
