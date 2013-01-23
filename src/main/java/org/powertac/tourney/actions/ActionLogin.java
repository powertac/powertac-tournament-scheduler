package org.powertac.tourney.actions;

import org.powertac.tourney.beans.User;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

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
      message(0, "Login Failure");
      return "Failure";
    }
    return "Success";
  }

  public String logout ()
  {
    User.getCurrentUser().logout();
    return "Login";
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("loginForm", fm);
    }
  }
}
