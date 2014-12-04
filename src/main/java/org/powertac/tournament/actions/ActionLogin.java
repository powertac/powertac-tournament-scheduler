package org.powertac.tournament.actions;

import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;


@ManagedBean
@RequestScoped
public class ActionLogin
{
  private String userName;
  private String password;

  public String getUserName ()
  {
    return this.userName;
  }

  public void setUserName (String userName)
  {
    this.userName = userName;
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
    boolean success = User.loginUser(getUserName(), getPassword());
    if (!success) {
      Utils.growlMessage("Login Failure");
      return "Failure";
    }
    return "Success";
  }

  public String logout ()
  {
    User.getCurrentUser().logout();
    return "Login";
  }
}
