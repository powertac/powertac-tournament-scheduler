package org.powertac.tourney.actions;

import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Database;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.sql.SQLException;

@Component("actionLogin")
@Scope("request")
public class ActionLogin
{
  private String username;
  private String password;

  public String getUsername ()
  {
    return this.username;
  }

  public void setUsername (String username)
  {
    this.username = username;
  }

  public String getPassword ()
  {
    return password;
  }

  public void setPassword (String password)
  {
    this.password = password;
  }

  public String login ()
  {
    Database database = new Database();
    try {
      int[] perm;// = new int[2] -1;
      database.startTrans();
      if ((perm = database.loginUser(getUsername(), getPassword()))[0] >= 0) {
        User test = User.getCurrentUser();
        test.setUsername(getUsername());
        test.setUserId(perm[1]);
        test.setPermissions(perm[0]);
        test.login();
      }
      else {
        // TODO Fix array to string
        String msg = "Login Failure: ";
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg + perm, null);
        FacesContext.getCurrentInstance().addMessage("loginForm", fm);
        return "Failure";
      }
      database.commitTrans();
    }
    catch (SQLException e) {
      database.abortTrans();
      e.printStackTrace();
      String msg = "Login Exception Failure";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("loginForm", fm);
      return "Failure";
    }

    return "Success";
  }

  public String logout ()
  {
    User test = User.getCurrentUser();
    test.logout();
    return "Login";
  }

}
