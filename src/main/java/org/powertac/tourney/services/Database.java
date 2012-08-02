package org.powertac.tourney.services;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.constants.Constants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.Date;


@Component("database")
@Scope("request")
public class Database
{
  private static Logger log = Logger.getLogger("TMLogger");

  // Connection Related
  private String dbUrl = "";
  private String database = "";
  private String port = "";
  private String username = "";
  private String password = "";
  private String dbms = "";

  // Database Configurations
  private Connection conn = null;

  private Properties connectionProps = new Properties();

  public Database ()
  {
    // Database Connection related properties
    TournamentProperties properties = TournamentProperties.getProperties();
    database = properties.getProperty("db.database");
    dbms = properties.getProperty("db.dbms");
    port = properties.getProperty("db.port");
    dbUrl = properties.getProperty("db.dbUrl");
    username = properties.getProperty("db.username");
    password = properties.getProperty("db.password");
  }

  public List<org.powertac.tourney.beans.User> getAllUsers () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_USERS);

    List<org.powertac.tourney.beans.User> users = new ArrayList<org.powertac.tourney.beans.User>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      User tmp = new User();
      tmp.setUsername(rs.getString("userName"));
      tmp.setPassword(rs.getString("password"));
      tmp.setUserId(rs.getInt("userId"));
      tmp.setPermissions(rs.getInt("permissionId"));
      users.add(tmp);
    }

    rs.close();
    ps.close();

    return users;
  }

  public String getUserName (int userId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_USER_BYID);
    ps.setInt(1, userId);

    String userName = "";
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      userName = rs.getString("userName");
    }

    rs.close();
    ps.close();

    return userName;
  }

  public int addUser (String username, String password) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_USER);
    // Use a pool of entropy to secure salts
    String genSalt = DigestUtils.md5Hex(Math.random() + (new Date()).toString());
    ps.setString(1, username);
    ps.setString(2, genSalt);
    // Store hashed and salted passwords
    ps.setString(3, DigestUtils.md5Hex(password + genSalt));
    ps.setInt(4, 3); // Lowest perm level for logged in user is 3

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int[] loginUser (String username, String password) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.LOGIN_SALT);
    ps.setString(1, username);

    boolean userExist = false;
    ResultSet rs = ps.executeQuery();
    // salt and hash password
    String salt = "";
    String digest = "";
    int userId = -1;
    int permission = 99; // Lowest permission level
    if (rs.next()) {
      digest = rs.getString("password");
      salt = rs.getString("salt");
      permission = rs.getInt("permissionId");
      userId = rs.getInt("userId");
      userExist = true;
    }
    else { // Time resistant attack we need to hash something
      digest = "000000000000000000000000000=";
      salt = "00000000000=";
      userExist = false;
    }

    int[] result = {-1, -1};
    if (DigestUtils.md5Hex(password + salt).equals(digest) && userExist) {
      result[0] = permission;
      result[1] = userId;
    }

    rs.close();
    ps.close();

    return result;
  }

  public int addBroker (int userId, String brokerName, String shortDescription)
    throws SQLException
  {
    Broker b = new Broker(brokerName, shortDescription);
    
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_BROKER);
    ps.setString(1, brokerName);
    ps.setString(2, b.getBrokerAuthToken());
    ps.setString(3, shortDescription);
    ps.setInt(4, userId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }
  
  public boolean brokerNameExists(String brokerName) throws SQLException {
    PreparedStatement ps = conn.prepareStatement(Constants.NUM_BROKER_COPIES);
    ps.setString(1, brokerName);
    
    ResultSet rs = ps.executeQuery();
    
    int count = 0;
    if(rs.next()){
      count = rs.getInt("num");
    }
    rs.close();
    ps.close();
    
    return count>0;
  }

  public List<Broker> getBrokersByUserId (int userId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_BROKERS_BY_USERID);
    ps.setInt(1, userId);

    List<Broker> brokers = new ArrayList<Broker>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      brokers.add(new Broker(rs));
    }

    rs.close();
    ps.close();

    return brokers;
  }

  public int deleteBrokerByBrokerId (int brokerId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.DELETE_BROKER_BY_BROKERID);
    ps.setInt(1, brokerId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateBrokerByBrokerId (int brokerId, String brokerName,
                                     String brokerAuth, String brokerShort)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.UPDATE_BROKER_BY_BROKERID);
    
    ps.setString(1, brokerName);
    ps.setString(2, brokerAuth);
    ps.setString(3, brokerShort);
    ps.setInt(4, brokerId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public Broker getBroker (int brokerId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_BROKER_BY_BROKERID);
    ps.setInt(1, brokerId);

    Broker broker = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      broker = new Broker(rs);
    }

    rs.close();
    ps.close();

    return broker;
  }

  public List<String> getProperties (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_PROPERTIES_BY_ID);
    ps.setInt(1, gameId);

    List<String> props = new ArrayList<String>();
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      props.add(rs.getString("location"));
      props.add(rs.getString("startTime"));
      props.add(rs.getString("jmsUrl"));
    }

    rs.close();
    ps.close();

    return props;
  }

  public int addProperties (int gameId, String locationKV, String startTimeKV)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_PROPERTIES);

    ps.setString(1, locationKV);
    ps.setString(2, startTimeKV);
    ps.setInt(3, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateProperties (int gameId, String jmsUrl)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_PROPETIES);

    ps.setString(1, jmsUrl);
		ps.setInt(2, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int deletePropertiesByGameId (int gameId)
      throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.DELETE_PROPERTIES_BY_GAMEID);

    ps.setInt(1, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int addPom (String uploadingUser, String name)
		 throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_POM,
        Statement.RETURN_GENERATED_KEYS);

    ps.setString(1, uploadingUser);
    ps.setString(2, name);
    ps.executeUpdate();

    // Return id of inserted pom
    int result = -1;
    ResultSet rs = ps.getGeneratedKeys();
    if (rs != null && rs.next()) {
      result = rs.getInt(1);
    }

    try {
      rs.close();
    } catch (NullPointerException ignored) {}
    ps.close();

    return result;
  }

  public List<Pom> getPoms () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_POMS);

    List<Pom> poms = new ArrayList<Pom>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Pom tmp = new Pom();
      tmp.setName(rs.getString("name"));
      tmp.setUploadingUser(rs.getString("uploadingUser"));
      tmp.setPomId(rs.getInt("pomId"));

      poms.add(tmp);
    }

    rs.close();
    ps.close();

    return poms;
  }

  public int addTournament (String tourneyName, boolean openRegistration,
                            int maxGames, Date startTime, String type,
                            int pomId, String locations, int maxBrokers,
                            int[] gameSizes, int[] numGames)
    throws SQLException
  {
    PreparedStatement ps =conn.prepareStatement(
        Constants.ADD_TOURNAMENT, Statement.RETURN_GENERATED_KEYS);

    ps.setString(1, tourneyName);
    ps.setString(2, Utils.dateFormat(startTime));
    ps.setBoolean(3, openRegistration);
    ps.setInt(4, maxGames);
    ps.setString(5, type);
    ps.setString(6, locations);
    ps.setInt(7, maxBrokers);
    ps.setInt(8, gameSizes[0]);
    ps.setInt(9, gameSizes[1]);
    ps.setInt(10, gameSizes[2]);
    ps.setInt(11, numGames[0]);
    ps.setInt(12, numGames[1]);
    ps.setInt(13, numGames[2]);
    ps.setInt(14, pomId);
    ps.executeUpdate();

    // Return id of inserted tournament
    int result = -1;
    ResultSet rs = ps.getGeneratedKeys();
    if (rs != null && rs.next()) {
      result = rs.getInt(1);
    }

    try {
      rs.close();
    } catch (NullPointerException ignored) {}
    ps.close();

    return result;
  }

  public int deleteTournament (int tournamentId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.REMOVE_TOURNAMENT);
    ps.setInt(1, tournamentId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public List<Tournament> getTournaments (Enum status)
      throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_TOURNAMENTS);
    ps.setString(1, status.toString());

    List<Tournament> ts = new ArrayList<Tournament>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      ts.add(new Tournament(rs));
    }
    
    ps.close();
    rs.close();
    
    return ts;
  }

  public Tournament getTournamentById (int tourneyId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_TOURNAMENT_BYID);
    ps.setInt(1, tourneyId);

    Tournament ts = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      ts = new Tournament(rs);
    }
    
    ps.close();
    rs.close();
    
    return ts;
  }

  public Tournament getTournamentByType (Tournament.TYPE type) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_TOURNAMENT_BYTYPE);
    ps.setString(1, type.toString());

    Tournament ts = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      ts = new Tournament(rs);
    }

    ps.close();
    rs.close();

    return ts;
  }

  public Tournament getTournamentByGameId (int gameId) throws SQLException
  {
    PreparedStatement ps =conn.prepareStatement(
        Constants.SELECT_TOURNAMENT_BYGAMEID);
    ps.setInt(1, gameId);

    Tournament ts = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      ts = new Tournament(rs);
    }

    ps.close();
    rs.close();
    
    return ts;
  }

  public List<Tournament> getTournamentsByBrokerId (int brokerId)
      throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_TOURNAMENTS_BY_BROKERID);
    ps.setString(1, String.valueOf(brokerId));

    List<Tournament> ts = new ArrayList<Tournament>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      ts.add(new Tournament(rs));
    }

    ps.close();
    rs.close();

    return ts;
  }

  public List<Tournament> getTournamentsByName (String name)
      throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_TOURNAMENT_BYNAME);
    ps.setString(1, name);

    List<Tournament> ts = new ArrayList<Tournament>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      ts.add(new Tournament(rs));
    }

    ps.close();
    rs.close();

    return ts;
  }

  public int setTournamentStartTime (int tournamentId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.UPDATE_TOURNAMENT_STARTTIME_BYID);
    ps.setInt(2, tournamentId);
    ps.setString(1, Utils.dateFormatUTCmilli(new Date()));

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public List<Game> getGamesInTourney (int tourneyId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_GAMES_IN_TOURNEY_BYID);
    ps.setInt(1, tourneyId);

    List<Game> gs = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      gs.add(tmp);
    }
    
    rs.close();
    ps.close();

    return gs;
  }

  public List<Game> getGamesInTourney (String tourneyName) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_GAMES_IN_TOURNEY_BYNAME);
    ps.setString(1, tourneyName);

    List<Game> gs = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      gs.add(tmp);
    }

    rs.close();
    ps.close();

    return gs;
  }

  public int updateTournamentStatus (int tourneyId, Tournament.STATE state) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.UPDATE_TOURNAMENT_STATUS_BYID);
    ps.setString(1, state.toString());
    ps.setInt(2, tourneyId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int registerBroker (int tourneyId, int brokerId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.REGISTER_BROKER);
    ps.setInt(1, tourneyId);
    ps.setInt(2, brokerId);
    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int unregisterBroker (int tourneyId, int brokerId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UNREGISTER_BROKER);
    ps.setInt(1, tourneyId);
    ps.setInt(2, brokerId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public boolean isRegistered (int tourneyId, int brokerId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.REGISTERED);
    ps.setInt(1, tourneyId);
    ps.setInt(2, brokerId);

    boolean result = false;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      result = rs.getBoolean("registered");
    }

    rs.close();
    ps.close();

    return result;
  }

  public List<Broker> getBrokers () throws SQLException
  {
    List<Broker> result = new ArrayList<Broker>();

    PreparedStatement ps = conn.prepareStatement(Constants.GET_BROKERS);

    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      result.add(new Broker(rs));
    }
    
    rs.close();
    ps.close();

    return result;
  }

  public int getNumberBrokersRegistered (int tourneyId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.GET_NUMBER_REGISTERED_BYTOURNAMENTID);
    ps.setInt(1, tourneyId);

    int result = 0;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      result = rs.getInt("numRegistered");
    }

    rs.close();
    ps.close();

    return result;
  }

  public List<Game> getGames () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_GAME);

    List<Game> gs = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      gs.add(tmp);
    }

    rs.close();
    ps.close();

    return gs;
  }
  
  public List<Game> getCompleteGames () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_COMPLETE_GAMES);

    List<Game> gs = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      gs.add(tmp);
    }

    rs.close();
    ps.close();

    return gs;
  }

  public List<Game> getRunnableGames(int excludedTourneyId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.GET_RUNNABLE_GAMES_EXC);
    ps.setInt(1, excludedTourneyId);

    List<Game> games = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      games.add(tmp);
    }

    rs.close();
    ps.close();

    return games;
  }

  public List<Game> getRunnableSingleGames() throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.GET_RUNNABLE_SINGLE_GAMES);

    List<Game> games = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      games.add(tmp);
    }

    rs.close();
    ps.close();

    return games;
  }
  
  public List<Server> getServers() throws SQLException {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_SERVERS);

    List<Server> servers = new ArrayList<Server>();
    ResultSet rs = ps.executeQuery();
    while(rs.next()){
      servers.add(new Server(rs));
    }

    ps.close();
    rs.close();
    
    return servers;
  }
  
  public List<Agent> getAgents() throws SQLException {
    PreparedStatement ps = conn.prepareStatement(Constants.GET_AGENT_TYPE);

    List<Agent> agents = new ArrayList<Agent>();
    ResultSet rs = ps.executeQuery();
    while(rs.next()){
      agents.add(new Agent(rs));
    }

    ps.close();
    rs.close();
    
    return agents;
  }
  
  public List<Game> getBootableGames () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.GET_BOOTABLE_GAMES);

    List<Game> games = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Game tmp = new Game(rs);
      games.add(tmp);
    }

    rs.close();
    ps.close();

    return games;
  }

  public int addGame (String gameName, int tourneyId, int maxBrokers,
                      Date startTime) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.ADD_GAME, Statement.RETURN_GENERATED_KEYS);

    ps.setString(1, gameName);
    ps.setInt(2, tourneyId);
    ps.setInt(3, maxBrokers);
    ps.setString(4, Utils.dateFormat(startTime));
    ps.executeUpdate();

    // Return id of inserted game
    int gameId = -1;
    ResultSet rs = ps.getGeneratedKeys();
    if (rs != null && rs.next()) {
      gameId = rs.getInt(1);
    }

    try {
      rs.close();
    } catch (NullPointerException ignored) {}
    ps.close();

    return gameId;
  }

  public int deleteGame (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.REMOVE_GAME);
    ps.setInt(1, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public Game getGame (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_GAMEBYID);
    ps.setInt(1, gameId);

    Game tmp = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      tmp = new Game(rs);
    }

    ps.close();
    rs.close();

    return tmp;
  }

  public List<Game> findGamesByStatusAndMachine (Game.STATE status,
                                                 String machineName)
  throws SQLException
  {
    PreparedStatement ps =
            conn.prepareStatement(Constants.FIND_GAME_BY_STATE_AND_MACHINE);
    ps.setString(1, status.toString());
    ps.setString(2,machineName);
    ArrayList<Game> result = new ArrayList<Game>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      result.add(new Game(rs));
    }

    ps.close();
    rs.close();

    return result;
  }

  public int addBrokerToGame (int gameId, Broker b) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_BROKER_TO_GAME);
    ps.setInt(1, gameId);
    ps.setInt(2, b.getBrokerId());
    ps.setString(3, createQueueName());
    ps.setBoolean(4, false);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }
  
  public List<Broker> getBrokersInGame (int gameId) throws SQLException
  {
    PreparedStatement ps = 
            conn.prepareStatement(Constants.GET_BROKERS_IN_GAME);
    ps.setInt(1, gameId);

    List<Broker> brokers = new ArrayList<Broker>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      brokers.add(new Broker(rs));
    }

    rs.close();
    ps.close();

    return brokers;
  }

  public List<Broker> getBrokersInGameComplete (int gameId) throws SQLException
  {
    PreparedStatement ps =
        conn.prepareStatement(Constants.GET_BROKERS_IN_GAME_COMPLETE);
    ps.setInt(1, gameId);

    List<Broker> brokers = new ArrayList<Broker>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      brokers.add(new Broker(rs));
    }

    rs.close();
    ps.close();

    return brokers;
  }

  public void updateBrokerInGame (int gameId, Broker broker)
          throws SQLException
  {
    PreparedStatement ps =
            conn.prepareStatement(Constants.UDATE_BROKER_INGAME);
    ps.setBoolean(1, broker.getBrokerInGame());
    ps.setInt(2, gameId);
    ps.setInt(3, broker.getBrokerId());
    ps.executeUpdate();
    ps.close();
  }

  public List<Broker> getBrokersInTournament (int tourneyId)
    throws SQLException
  {
    PreparedStatement ps = 
            conn.prepareStatement(Constants.GET_BROKERS_INTOURNAMENT);
    ps.setInt(1, tourneyId);

    List<Broker> brokers = new ArrayList<Broker>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      brokers.add(new Broker(rs));
    }
    ps.close();
    rs.close();

    return brokers;
  }

  public String getBrokerQueueName(int gameId, int brokerId)
      throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.GET_BROKER_QUEUE);
    ps.setInt(1, gameId);
    ps.setInt(2, brokerId);

    String result = "";
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      result = rs.getString("brokerQueue");
    }
    ps.close();
    rs.close();

    return result;
  }

  public boolean isGameReady (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.GAME_READY);
    ps.setInt(1, gameId);

    boolean result = false;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      result = rs.getBoolean("ready");
    }

    rs.close();
    ps.close();

    return result;
  }

  public int updateGameStatusById (int gameId, Game.STATE status)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME);
    ps.setInt(2, gameId);
    ps.setString(1, status.toString());

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateGameBootstrapById (int gameId, boolean hasBootstrap)
      throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_BOOTSTRAP);
    ps.setInt(2, gameId);
    ps.setBoolean(1, hasBootstrap);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int clearGameReadyTime (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.CLEAR_GAME_READYTIME);
    ps.setInt(1, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int setGameReadyTime (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_READYTIME);
    ps.setInt(2, gameId);
    ps.setString(1, Utils.dateFormatUTCmilli(new Date()));

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int setGameStartTime (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_STARTTIME);
    ps.setInt(2, gameId);
    ps.setString(1, Utils.dateFormatUTCmilli(new Date()));

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateGameMachine (int gameId, int machineId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_MACHINE);
    ps.setInt(2, gameId);
    ps.setInt(1, machineId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateGameFreeMachine (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_FREE_MACHINE);
    ps.setInt(1, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateGameFreeBrokers (int gameId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_FREE_BROKERS);
    ps.setInt(1, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateGameJmsUrlById (int gameId, String jmsUrl)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_JMSURL);
    ps.setString(1, jmsUrl);
    ps.setString(2, createQueueName());
		ps.setInt(3, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int updateGameViz (int gameId, String vizUrl) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.UPDATE_GAME_VIZ);
    ps.setString(1, vizUrl);
    ps.setString(2, createQueueName());
    ps.setInt(3, gameId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public List<Machine> getMachines () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_MACHINES);

    List<Machine> machines = new ArrayList<Machine>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Machine tmp = new Machine(rs);
      machines.add(tmp);
    }

    rs.close();
    ps.close();

    return machines;
  }
  
  public Machine getMachineById(int machineId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_MACHINES_BY_ID);
    ps.setInt(1, machineId);

    Machine result = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      result = new Machine(rs);
    }

    rs.close();
    ps.close();

    return result;
  }

  public Machine getMachineByName(String machineName) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.SELECT_MACHINES_BY_NAME);
    ps.setString(1, machineName);

    Machine result = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      result = new Machine(rs);
    }

    rs.close();
    ps.close();

    return result;
  }

  public int setMachineAvailable (int machineId, boolean isAvailable)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.UPDATE_MACHINE_AVAILABILITY);
    ps.setBoolean(1, isAvailable);
    ps.setInt(2, machineId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  /**
   * Set the status of the machine : 'idle' or 'running'
   */
  public int setMachineStatus (int machineId, Machine.STATE status)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(
        Constants.UPDATE_MACHINE_STATUS_BY_ID);
    ps.setString(1, status.toString());
    ps.setInt(2, machineId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int addMachine (String machineName, String machineUrl,
                         String visualizerUrl)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_MACHINE);
    ps.setString(1, machineName);
    ps.setString(2, machineUrl);
    ps.setString(3, visualizerUrl);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int editMachine (String machineName, String machineUrl,
          				        String visualizerUrl,
          				        int machineId)
    throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.EDIT_MACHINE);
    ps.setString(1, machineName);
    ps.setString(2, machineUrl);
    ps.setString(3, visualizerUrl);
    ps.setInt(4, machineId);

    int result = ps.executeUpdate();

    ps.close();
    
    return result;
  }
  
  public int deleteMachine (int machineId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.REMOVE_MACHINE);
    ps.setInt(1, machineId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public Machine claimFreeMachine () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.FIRST_FREE_MACHINE);

    Machine result = null;
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      result = new Machine(rs);
      setMachineStatus(result.getMachineId(), Machine.STATE.running);
    }
    else {
      log.info("No free machines found.");
    }

    rs.close();
    ps.close();

    return result;
  }

  public Machine claimFreeMachine (String machineName) throws SQLException
  {
    Machine result = getMachineByName(machineName);

    if ((result != null) &&
        (result.isAvailable()) &&
        (result.stateEquals(Machine.STATE.idle))) {
      setMachineStatus(result.getMachineId(), Machine.STATE.running);
      return result;
    }

    return null;
  }

  public List<Location> getLocations () throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.SELECT_LOCATIONS);

    List<Location> locations = new ArrayList<Location>();
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      Location tmp = new Location();
      tmp.setLocationId(rs.getInt("locationId"));
      tmp.setName(rs.getString("location"));
      Calendar fromDate = Calendar.getInstance();
      fromDate.setTimeInMillis(rs.getDate("fromDate").getTime());
      tmp.setFromDate(fromDate.getTime());
      Calendar toDate = Calendar.getInstance();
      toDate.setTimeInMillis(rs.getDate("toDate").getTime());
      tmp.setToDate(toDate.getTime());

      locations.add(tmp);
    }

    rs.close();
    ps.close();

    return locations;
  }

  public int deleteLocation (int locationId) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.DELETE_LOCATION);
    ps.setInt(1, locationId);

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }

  public int addLocation (String location, Date newLocationStartTime,
                          Date newLocationEndTime) throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement(Constants.ADD_LOCATION);
    ps.setString(1, location);
    ps.setDate(2, new java.sql.Date(newLocationStartTime.getTime()));
    ps.setDate(3, new java.sql.Date(newLocationEndTime.getTime()));

    int result = ps.executeUpdate();

    ps.close();

    return result;
  }
  
  private Random queueGenerator = new Random(new Date().getTime());
  public String createQueueName ()
  {
    return Long.toString(queueGenerator.nextLong(), 31);
  }

  /***
   * Database connection methods
   */
  // TODO: Strategy Object find the correct dbms by reflection and call its
  // connection method
  private void checkDb ()
  {
    try {
      if (conn == null || conn.isClosed()) {
        if (dbms.equalsIgnoreCase("mysql")) {
          try {
            connectionProps.setProperty("user", username);
            connectionProps.setProperty("password", password);
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn =
                DriverManager.getConnection("jdbc:" + dbms + "://"
                    + dbUrl + "/"
                    + database,
                    connectionProps);
          }
          catch (Exception e) {
            log.error("Connection Error");
            e.printStackTrace();
          }
        }
        else {
          log.error("DBMS: " + dbms + " is not supported");
        }
      }
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public int startTrans () throws SQLException
  {
    openConnection();
    PreparedStatement trans = conn.prepareStatement(Constants.START_TRANS);
    trans.execute();
    return 0;
  }

  public int commitTrans ()
  {
    try {
      PreparedStatement trans = conn.prepareCall(Constants.COMMIT_TRANS);
      trans.execute();
      closeConnection();
    }
    catch (SQLException se) {
      se.printStackTrace();
    }
    return 0;
  }

  public int abortTrans ()
  {
    try {
      PreparedStatement trans = conn.prepareCall(Constants.ABORT_TRANS);
      trans.execute();
      closeConnection();
    }
    catch (SQLException se) {
      se.printStackTrace();
    }
    return 0;
  }

  public int truncateScheduler() throws SQLException
  {
    PreparedStatement trunc = conn.prepareStatement("DELETE FROM AgentAdmin;");
    trunc.executeUpdate();
    trunc.close();
    trunc = conn.prepareStatement("DELETE FROM AgentQueue;");
    trunc.executeUpdate();
    trunc.close();
    trunc = conn.prepareStatement("DELETE FROM GameArchive;");
    trunc.executeUpdate();
    trunc.close();
    trunc = conn.prepareStatement("DELETE FROM GameLog;");
    trunc.executeUpdate();
    trunc.close();
    trunc = conn.prepareStatement("DELETE FROM GameServers;");
    trunc.executeUpdate();
    trunc.close();
    return 0;
  }
  
  private void openConnection () throws SQLException
  {
    checkDb();
  }

  public void closeConnection ()
  {
    try {
      conn.close();
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }


  /***
  * Database containers
  */
  public class Pom
  {
    private String name;
    private String uploadingUser;
    private int pomId;

    public String getName ()
    {
      return name;
    }
    public void setName (String name)
    {
      this.name = name;
    }

    public String getUploadingUser ()
    {
      return uploadingUser;
    }
    public void setUploadingUser (String uploadingUser)
    {
      this.uploadingUser = uploadingUser;
    }

    public int getPomId() {
      return pomId;
    }
    public void setPomId(int pomId) {
      this.pomId = pomId;
    }
  }

  public class Server
  {
    private int serverNumber = 0;
    private boolean isPlaying = false;
    public Server(ResultSet rs)
    {
      try {
        serverNumber = rs.getInt("ServerNumber");
        isPlaying = rs.getBoolean("IsPlaying");
      }
      catch(Exception e) {
        log.error("Error making server from result set");
        e.printStackTrace();
      }
    }

    public int getServerNumber ()
    {
      return serverNumber;
    }

    public void setServerNumber (int serverNumber)
    {
      this.serverNumber = serverNumber;
    }

    public boolean getIsPlaying ()
    {
      return isPlaying;
    }

    public void setIsPlaying (boolean isPlaying)
    {
      this.isPlaying = isPlaying;
    }
  }

  public class Agent
  {
    private int InternalAgentID= 0;

    public Agent(ResultSet rs)
    {
      try {
        InternalAgentID = rs.getInt("AgentType");
      }
      catch(Exception e) {
        log.error("Error making agent from result set");
        e.printStackTrace();
      }
    }

    public int getInternalAgentID ()
    {
      return InternalAgentID;
    }

    public void setInternalAgentID (int internalAgentID)
    {
      InternalAgentID = internalAgentID;
    }
  }
}
