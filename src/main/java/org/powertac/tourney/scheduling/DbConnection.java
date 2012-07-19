package org.powertac.tourney.scheduling;

import org.powertac.tourney.services.SpringApplicationContext;
import org.powertac.tourney.services.TournamentProperties;

import java.sql.*;

import static org.powertac.tourney.services.Utils.log;

public class DbConnection
{
  /*
   * configuration parameters
   */
  private boolean debug = true;
  private String serverip = "";
  private String database = "tourney";
  private String username = "";
  private String passwd = "";
  private String dbms = "mysql";
  private String port = "";
  private Connection myconnection = null;
  private Statement statement = null;

  public DbConnection ()
  {
    TournamentProperties tournamentProperties =
        (TournamentProperties) SpringApplicationContext.getBean("tournamentProperties");

    // Database Connection related properties
    database = (tournamentProperties.getProperty("db.database"));
    dbms = (tournamentProperties.getProperty("db.dbms"));
    port = (tournamentProperties.getProperty("db.port"));
    serverip = (tournamentProperties.getProperty("db.dbUrl"));
    username = (tournamentProperties.getProperty("db.username"));
    passwd = (tournamentProperties.getProperty("db.password"));
  }

  public void startTransaction () throws SQLException
  {
    myconnection.setAutoCommit(false);
  }

  public void commitTransction () throws SQLException
  {
    myconnection.commit();
    myconnection.setAutoCommit(true);
  }

  public void rollbackTransction () throws SQLException
  {
    myconnection.rollback();
    myconnection.setAutoCommit(true);
  }

  public void Setup () throws Exception
  {
    // TODO Actually use dbms + port
    String setupstring = "jdbc:mysql://" + serverip + "/" + database
                         + "?user=" + username + "&password=" + passwd;
    if (debug) {
      log(setupstring);
    }
    myconnection = DriverManager.getConnection(setupstring);
    statement = myconnection.createStatement();
  }

  public void Close () throws Exception
  {
    myconnection.close();
  }

  public void PrintResultSet (ResultSet rs, String[] input) throws Exception
  {
    for (String token: input) {
      log(token + ": ");
    }
    log("");
    while (rs.next()) {
      for (String token: input) {
        log(rs.getString(token) + ", ");
      }
      log("");
    }
  }

  public ResultSet SetQuery (String sql, String flag) throws SQLException
  {
    ResultSet rs = null;
    if (flag.equals("read")) {
      rs = statement.executeQuery(sql);
    }
    else if (flag.equals("update")) {
      statement.executeUpdate(sql);
    }
    return rs;
  }

  public ResultSet SetQuery (String sql) throws SQLException
  {
    ResultSet rs = null;
    rs = statement.executeQuery(sql);
    return rs;
  }
}
