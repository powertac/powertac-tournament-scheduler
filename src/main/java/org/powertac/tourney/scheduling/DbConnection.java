package org.powertac.tourney.scheduling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.powertac.tourney.services.TournamentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DbConnection
{
  @Autowired
  private TournamentProperties tournamentProperties;
  
  /*
   * configuration parameters
   */
  private boolean debug = true;
  private String serverip = "127.0.0.1";
  private String database = "PowerTAC";
  private String username = "root";
  private String passwd = "H8G01Kae";
  private String dbms = "mysql";
  private String port = "";
  private Connection myconnection = null;
  private Statement statement = null;

  public DbConnection ()
  {
    // Database Connection related properties
    this.database = (tournamentProperties.getProperty("db.database"));
    this.dbms = (tournamentProperties.getProperty("db.dbms"));
    this.port = (tournamentProperties.getProperty("db.port"));
    this.serverip = (tournamentProperties.getProperty("db.dbUrl"));
    this.username = (tournamentProperties.getProperty("db.username"));
    this.passwd = (tournamentProperties.getProperty("db.password"));
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
    String setupstring =
      "jdbc:mysql://" + serverip + "/" + database + "?user=" + username
              + "&password=" + passwd;
    ;
    if (debug) {
      System.out.println(setupstring);
    }
    myconnection = DriverManager.getConnection(setupstring);
    statement = myconnection.createStatement();
    // return statement;
  }

  public void Close () throws Exception
  {
    myconnection.close();
  }

  public void PrintResultSet (ResultSet rs, String[] input) throws Exception
  {

    int i;
    String token;

    for (i = 0; i < input.length; i++) {
      token = input[i];
      System.out.print(token + ": ");
    }
    System.out.println("");
    while (rs.next()) {
      for (i = 0; i < input.length; i++) {
        token = input[i];
        System.out.print(rs.getString(token) + ", ");
      }
      System.out.println("");
    }

  }

  public ResultSet SetQuery (String sql, String flag) throws SQLException
  {
    ResultSet rs = null;
    if (flag == "read")
      rs = statement.executeQuery(sql);
    else if (flag == "update") {
      // System.out.println(sql);
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
