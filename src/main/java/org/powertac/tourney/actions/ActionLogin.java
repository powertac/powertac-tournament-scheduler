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
    Database db = new Database();
    try {
      int[] perm;// = new int[2] -1;
      db.startTrans();
      if ((perm = db.loginUser(getUsername(), getPassword()))[0] >= 0) {
        User test = User.getCurrentUser();
        test.setUsername(getUsername());
        test.setUserId(perm[1]);
        test.setPermissions(perm[0]);
        test.login();
      }
      else {
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Login Failure", null);
        FacesContext.getCurrentInstance().addMessage("loginForm", fm);
        db.abortTrans();
        return "Failure";
      }
      db.commitTrans();
    }
    catch (SQLException e) {
      db.abortTrans();
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
