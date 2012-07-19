package org.powertac.tourney.actions;

import org.powertac.tourney.services.Database;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.sql.SQLException;


@Component("actionRegister")
@Scope("request")
public class ActionRegister
{
  private String name;
  private String group;
  private String email;
  private String phone;
  private String username;
  private String password1;
  private String password2;

  public String register ()
  {
    Database db = new Database();
    try {
      db.startTrans();
      if (password1.equals(password2)) {
        db.addUser(this.getUsername(), this.getPassword1());
        db.commitTrans();
        return "Success";
      }
      else {
        db.abortTrans();
        String msg = "Passwords do not match";
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
        FacesContext.getCurrentInstance().addMessage("registerForm", fm);
        return "Failure";
      }
    }
    catch (SQLException e) {
      db.abortTrans();
      e.printStackTrace();
      return "Failure";
    }
  }

  //<editor-fold desc="Setters and Getters">
  public String getName ()
  {
    return name;
  }

  public void setName (String name)
  {
    this.name = name;
  }

  public String getGroup ()
  {
    return group;
  }

  public void setGroup (String group)
  {
    this.group = group;
  }

  public String getEmail ()
  {
    return email;
  }

  public void setEmail (String email)
  {
    this.email = email;
  }

  public String getUsername ()
  {
    return username;
  }

  public void setUsername (String username)
  {
    this.username = username;
  }

  public String getPassword1 ()
  {
    return password1;
  }

  public void setPassword1 (String password1)
  {
    this.password1 = password1;
  }

  public String getPassword2 ()
  {
    return password2;
  }

  public void setPassword2 (String password2)
  {
    this.password2 = password2;
  }

  public String getPhone ()
  {
    return phone;
  }

  public void setPhone (String phone)
  {
    this.phone = phone;
  }
  //</editor-fold>
}
