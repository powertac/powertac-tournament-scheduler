package org.powertac.tournament.actions;

import org.apache.log4j.Logger;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;


@ManagedBean
@RequestScoped
public class ActionRegister
{
  private static Logger log = Utils.getLogger();

  private String username;
  private String password1;
  private String password2;
  private String institution;
  private String contactName;
  private String contactEmail;
  private String contactPhone;

  public String register ()
  {
    if (nameExists(username)) {
      return "Failure";
    }
    if (passwordMismatch(password1, password2)) {
      return "Failure";
    }

    User user = new User();
    user.setUserName(username);
    user.setContactName(contactName);
    user.setContactEmail(contactEmail);
    user.setContactPhone(contactPhone);
    user.setInstitution(institution);
    user.setPermissionBroker();
    user.setPasswordAndSalt(password1);

    log.info("Registring user " + username);
    return user.save();
  }

  private boolean nameExists (String username)
  {
    User user = User.getUserByName(username);
    if (user != null) {
      Utils.growlMessage("User Name taken, please select a new name.");
      return true;
    }
    return false;
  }

  private boolean passwordMismatch (String password1, String password2)
  {
    if (!password1.equals(password2)) {
      Utils.growlMessage("Passwords do not match.");
      return true;
    }
    return false;
  }

  //<editor-fold desc="Setters and Getters">
  public String getContactName ()
  {
    return contactName;
  }

  public void setContactName (String contactName)
  {
    this.contactName = contactName;
  }

  public String getInstitution ()
  {
    return institution;
  }

  public void setInstitution (String institution)
  {
    this.institution = institution;
  }

  public String getContactEmail ()
  {
    return contactEmail;
  }

  public void setContactEmail (String contactEmail)
  {
    this.contactEmail = contactEmail;
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

  public String getContactPhone ()
  {
    return contactPhone;
  }

  public void setContactPhone (String contactPhone)
  {
    this.contactPhone = contactPhone;
  }
  //</editor-fold>
}
