package org.powertac.tourney.actions;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.User;
import org.powertac.tourney.services.HibernateUtil;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.Date;


@ManagedBean
@RequestScoped
public class ActionRegister
{
  private static Logger log = Logger.getLogger("TMLogger");

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

    String genSalt = DigestUtils.md5Hex(Math.random() + (new Date()).toString());

    User user = new User();
    user.setUserName(username);
    user.setContactName(contactName);
    user.setContactEmail(contactEmail);
    user.setContactPhone(contactPhone);
    user.setInstitution(institution);
    user.setPermission(User.PERMISSION.broker);
    user.setPassword(DigestUtils.md5Hex(password1 + genSalt));
    user.setSalt(genSalt);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      log.info("Registring user " + username);
      session.save(user);
      transaction.commit();
      return "Success";
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Failure";
    } finally {
      session.close();
    }
  }

  private boolean nameExists (String username)
  {
    User user = User.getUserByName(username);
    if (user != null) {
      message(0, "User Name taken, please select a new name");
      return true;
    }
    return false;
  }

  private boolean passwordMismatch (String password1, String password2)
  {
    if (!password1.equals(password2)) {
      message(0, "Passwords do not match");
      return true;
    }
    return false;
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("registerForm", fm);
    }
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
