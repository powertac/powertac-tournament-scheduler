package org.powertac.tourney.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.powertac.tourney.beans.Competition;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Games;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Machines;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.beans.Tournaments;
import org.powertac.tourney.scheduling.MainScheduler;
import org.powertac.tourney.services.CreateProperties;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.RunBootstrap;
import org.powertac.tourney.services.RunGame;
import org.powertac.tourney.services.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Component("actionTournament")
@Scope("session")
public class ActionTournament
{

  @Autowired
  private Upload upload;

  @Autowired
  private Scheduler scheduler;


 

  public enum TourneyType {
    SINGLE_GAME, MULTI_GAME;
  }

  private String selectedPom;

  private Calendar initTime = Calendar.getInstance();

  private Date startTime = new Date(); // Default to current date/time
  private Date fromTime = new Date();
  private Date toTime = new Date();

  private String tournamentName;
  private int maxBrokers;
  private int maxBrokerInstances = 2;

  // private List<Integer> machines;
  private List<String> locations;
  private String pomName;
  private String bootName;
  private String propertiesName;
  private UploadedFile pom;
  private UploadedFile boot;
  private UploadedFile properties;
  private TourneyType type = TourneyType.SINGLE_GAME;

  private int size1 = 2;
  private int numberSize1 = 2;
  private int size2 = 4;
  private int numberSize2 = 4;
  private int size3 = 8;
  private int numberSize3 = 4;

  public ActionTournament ()
  {

    initTime.set(2009, 2, 3);
    fromTime.setTime(initTime.getTimeInMillis());
    initTime.set(2011, 2, 3);
    toTime.setTime(initTime.getTimeInMillis());

  }

  public void formType (ActionEvent event)
  {

    // Get submit button id
    SelectItem ls = (SelectItem) event.getSource();
    ls.getValue();

  }

  public TourneyType getMulti ()
  {
    return TourneyType.MULTI_GAME;
  }

  public TourneyType getSingle ()
  {
    return TourneyType.SINGLE_GAME;
  }

  /**
   * @return the properties
   */
  public UploadedFile getProperties ()
  {
    return properties;
  }

  /**
   * @param properties
   *          the properties to set
   */
  public void setProperties (UploadedFile properties)
  {
    this.properties = properties;
  }

  /**
   * @return the boot
   */
  public UploadedFile getBoot ()
  {
    return boot;
  }

  /**
   * @param boot
   *          the boot to set
   */
  public void setBoot (UploadedFile boot)
  {
    this.boot = boot;
  }

  /**
   * @return the pom
   */
  public UploadedFile getPom ()
  {
    return pom;
  }

  /**
   * @param pom
   *          the pom to set
   */
  public void setPom (UploadedFile pom)
  {
    this.pom = pom;
  }

  /**
   * @return the propertiesName
   */
  public String getPropertiesName ()
  {
    return propertiesName;
  }

  /**
   * @param propertiesName
   *          the propertiesName to set
   */

  public void setPropertiesName (String propertiesName)
  {

    // Generate MD5 hash
    this.propertiesName =
      DigestUtils.md5Hex(propertiesName + (new Date()).toString()
                         + Math.random());
  }

  /**
   * @return the bootName
   */
  public String getBootName ()
  {
    return bootName;
  }

  /**
   * @param bootName
   *          the bootName to set
   */
  public void setBootName (String bootName)
  {
    // Generate MD5 hash

    this.bootName =
      DigestUtils.md5Hex(bootName + (new Date()).toString() + Math.random());

  }

  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    // Generate MD5 hash
    this.pomName =
      DigestUtils.md5Hex(pomName + (new Date()).toString() + Math.random());
  }

  // Method to list the type enumeration in the jsf select Item component
  public SelectItem[] getTypes ()
  {
    SelectItem[] items = new SelectItem[TourneyType.values().length];
    int i = 0;
    for (TourneyType t: TourneyType.values()) {
      items[i++] = new SelectItem(t, t.name());
    }
    return items;
  }

  public TourneyType getType ()
  {
    return type;
  }

  public void setType (TourneyType type)
  {
    this.type = type;
  }

  public Date getStartTime ()
  {
    return startTime;
  }

  public void setStartTime (Date startTime)
  {
    this.startTime = startTime;
  }

  public int getMaxBrokers ()
  {
    return maxBrokers;
  }

  public void setMaxBrokers (int maxBrokers)
  {
    this.maxBrokers = maxBrokers;
  }

  public String getTournamentName ()
  {
    return tournamentName;
  }

  public void setTournamentName (String tournamentName)
  {
    this.tournamentName = tournamentName;
  }

  public String createTournament ()
  {
    // Create a tournament and insert it into the application context
    Tournament newTourney = new Tournament();
    String allLocations = "";
    for (String s: locations) {
      allLocations += s + ",";
    }
    
    if (type == TourneyType.SINGLE_GAME) {

      /*
       * this.setPomName(pom.getName());
       * upload.setUploadedFile(getPom());
       * String finalFile = upload.submit(this.getPomName());
       */
      newTourney.setPomName(selectedPom);

      String hostip = "http://";

      try {
        InetAddress thisIp = InetAddress.getLocalHost();
        hostip += thisIp.getHostAddress() + ":8080";
      }
      catch (UnknownHostException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }

      Database db = new Database();

      newTourney.setPomUrl(hostip
                           + "/TournamentScheduler/faces/pom.jsp?location="
                           + newTourney.getPomName());
      newTourney.setMaxBrokers(getMaxBrokers());
      newTourney.setStartTime(getStartTime());
      newTourney.setTournamentName(getTournamentName());
     

      try {
        int tourneyId = 0;
        int gameId = 0;
        // Starts new transaction to prevent race conditions
        System.out.println("Starting transaction");
        db.startTrans();
        // Adds new tournament to the database
        System.out.println("Adding tourney");
        db.addTournament(newTourney.getTournamentName(), true, size1,
                         newTourney.getStartTime(), "SINGLE_GAME",
                         newTourney.getPomUrl(), allLocations, maxBrokers);
        // Grabs the tourney Id

        System.out.println("Getting tourneyId");
        tourneyId = db.getMaxTourneyId();
        // Adds a new game to the database

        System.out.println("Adding game");

        db.addGame(newTourney.getTournamentName(), tourneyId, size1, startTime);
        // Grabs the game id
        System.out.println("Getting gameId");
        gameId = db.getMaxGameId();
        System.out.println("Creating game: " + gameId + " properties");
        CreateProperties.genProperties(gameId, locations, fromTime, toTime);

        // Sets the url for the properties file based on the game id.
        // Properties are created at random withing specified parameters
        System.out.println("Updating properties game: " + gameId);
        db.updateGamePropertiesById(gameId);

        System.out.println("Committing transaction");
        db.commitTrans();
        db.closeConnection();
        
      }
      catch (SQLException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

     

    }
    else if (type == TourneyType.MULTI_GAME) {
      
      int tourneyId = 0;
      int gameId = 0;
      
      

      int noofagents = maxBrokers;
      int noofcopies = maxBrokerInstances;
      int noofservers = 7;
      int iteration = 1, num;
      int[] gtypes = { size1, size2, size3 };
      int[] mxs = { numberSize1, numberSize1, numberSize1 };

      try {
        MainScheduler gamescheduler = new MainScheduler(noofagents,noofcopies,noofservers, gtypes, mxs);
        gamescheduler.initializeAgentsDB(noofagents, noofcopies);
        gamescheduler.initGameCube(gtypes, mxs);
        int numberOfGames = gamescheduler.getGamesEstimate();
       
        System.out.println("No. of games: "+numberOfGames);
        gamescheduler.resetCube();
        
        Database db = new Database();
        
        // Add the number of games to a new tournament
        // Starts new transaction to prevent race conditions
        System.out.println("[INFO] Starting transaction");
        db.startTrans();
        // Adds new tournament to the database
        System.out.println("[INFO] Creating New tourney");
        db.addTournament(newTourney.getTournamentName(), true, size1,
                         newTourney.getStartTime(), "SINGLE_GAME",
                         newTourney.getPomUrl(), allLocations, maxBrokers);
        // Grabs the tourney Id

        System.out.println("[INFO] Getting tourneyId");
        tourneyId = db.getMaxTourneyId();
        
        
        // Adds a new game to the database
        for(int i=0; i< numberOfGames;i++){
          System.out.println("[INFO] Adding game");
  
          db.addGame(newTourney.getTournamentName(), tourneyId, size1, startTime);
          
          gameId = db.getMaxGameId();
          System.out.println("[INFO] Creating game: " + gameId + " properties");
          CreateProperties.genProperties(gameId, locations, fromTime, toTime);
  
          // Sets the url for the properties file based on the game id.
          // Properties are created at random within specified parameters
          System.out.println("[INFO] Updating properties game: " + gameId);
          db.updateGamePropertiesById(gameId);
        }
        System.out.println("[INFO] Committing transaction");
        
        db.commitTrans();
        db.closeConnection();
        
        
        
        
        

      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        System.out.println("[ERROR] Scheduling exception!");
        e.printStackTrace();
      }


    }
    else {
      
      //WHat?

    }

   
    return "Success";

  }

  public List<Database.Pom> getPomList ()
  {
    List<Database.Pom> poms = new ArrayList<Database.Pom>();

    Database db = new Database();

    try {
      poms = db.getPoms();
      db.closeConnection();
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return poms;

  }

  public List<Machine> getAvailableMachineList ()
  {
    List<Machine> machines = new ArrayList<Machine>();

    Database db = new Database();
    try {
      List<Machine> all = db.getMachines();
      for (Machine m: all) {
        if (m.isAvailable()) {
          machines.add(m);
        }
      }
      db.closeConnection();

    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return machines;
  }

  public Date getFromTime ()
  {
    return fromTime;
  }

  public void setFromTime (Date fromTime)
  {
    this.fromTime = fromTime;
  }

  public Date getToTime ()
  {
    return toTime;
  }

  public void setToTime (Date toTime)
  {
    this.toTime = toTime;
  }

  public List<String> getLocations ()
  {
    return locations;
  }

  public void setLocations (List<String> locations)
  {
    this.locations = locations;
  }

  public String getSelectedPom ()
  {
    return selectedPom;
  }

  public void setSelectedPom (String selectedPom)
  {
    this.selectedPom = selectedPom;
  }

  public int getSize1 ()
  {
    return size1;
  }

  public void setSize1 (int size1)
  {
    this.size1 = size1;
  }

  public int getNumberSize1 ()
  {
    return numberSize1;
  }

  public void setNumberSize1 (int numberSize1)
  {
    this.numberSize1 = numberSize1;
  }

  public int getSize2 ()
  {
    return size2;
  }

  public void setSize2 (int size2)
  {
    this.size2 = size2;
  }

  public int getNumberSize2 ()
  {
    return numberSize2;
  }

  public void setNumberSize2 (int numberSize2)
  {
    this.numberSize2 = numberSize2;
  }

  public int getSize3 ()
  {
    return size3;
  }

  public void setSize3 (int size3)
  {
    this.size3 = size3;
  }

  public int getNumberSize3 ()
  {
    return numberSize3;
  }

  public void setNumberSize3 (int numberSize3)
  {
    this.numberSize3 = numberSize3;
  }

  public int getMaxBrokerInstances ()
  {
    return maxBrokerInstances;
  }

  public void setMaxBrokerInstances (int maxBrokerInstances)
  {
    this.maxBrokerInstances = maxBrokerInstances;
  }

}
