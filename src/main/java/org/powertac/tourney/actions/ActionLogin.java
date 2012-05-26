package org.powertac.tourney.actions;

import java.sql.SQLException;


import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.Database;
import org.powertac.tourney.services.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("actionLogin")
@Scope("request")
public class ActionLogin
{

  // Database should not be as service, sharing information causes issues
  // @Autowired
  // private Database database;

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
      database.openConnection();
      if ((perm = database.loginUser(getUsername(), getPassword()))[0] >= 0) {
        User test =
          (User) FacesContext.getCurrentInstance().getExternalContext()
                  .getSessionMap().get(User.getKey());
        test.setUsername(getUsername());
        test.setUserId(perm[1]);
        test.setPermissions(perm[0]);
        test.login();
      }
      else {
        FacesContext.getCurrentInstance()
                .addMessage("loginForm",
                            new FacesMessage(FacesMessage.SEVERITY_INFO,
                                             "Login Failure: " + perm, null));
        return "Failure";
      }
      database.closeConnection();

    }
    catch (SQLException e) {
      database.closeConnection();
      FacesContext.getCurrentInstance()
              .addMessage("loginForm",
                          new FacesMessage(FacesMessage.SEVERITY_INFO,
                                           "Login Exception Failure", null));
      e.printStackTrace();

      return "Failure";

    }

    return "Success";
  }

  public String logout ()
  {

    User test =
      (User) FacesContext.getCurrentInstance().getExternalContext()
              .getSessionMap().get(User.getKey());

    test.logout();

    return "Login";
  }

}
