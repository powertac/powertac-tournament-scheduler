package org.powertac.tourney.scheduling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import org.powertac.tourney.services.Database;


public class DbConnection
{
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

  private Properties prop = new Properties();

  public DbConnection ()
  {
    try {
      prop.load(Database.class.getClassLoader()
              .getResourceAsStream("/tournament.properties"));
      // System.out.println(prop);
      // Database Connection related properties
      this.database = (prop.getProperty("db.database"));
      this.dbms = (prop.getProperty("db.dbms"));
      this.port = (prop.getProperty("db.port"));
      this.serverip = (prop.getProperty("db.dbUrl"));
      this.username = (prop.getProperty("db.username"));
      this.passwd = (prop.getProperty("db.password"));

      // System.out.println("Successfully instantiated Database bean!");

    }
    catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


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
