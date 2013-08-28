package org.powertac.tournament.beans;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@SessionScoped
@ManagedBean
@Entity
@Table(name = "users")
public class User
{
  private int userId = -1;
  private String userName = "Guest";
  private String institution;
  private String contactName;
  private String contactEmail;
  private String contactPhone;
  private String salt;
  private String password = "";
  private PERMISSION permission = PERMISSION.guest;

  private Map<Integer, Broker> brokerMap = new HashMap<Integer, Broker>();

  private boolean isEditingBroker;
  private boolean isEditingDetails;

  private static enum PERMISSION {
    admin,
    researcher,
    organizer,
    broker,
    guest;

    public PERMISSION getDecreased() {
      return this.ordinal() < PERMISSION.values().length - 1
          ? PERMISSION.values()[this.ordinal() + 1]
          : this;
    }

    public PERMISSION getIncreased() {
      return this.ordinal() == 0
          ? this
          : PERMISSION.values()[this.ordinal() - 1];
    }
  }

  public User ()
  {
  }

  public void increasePermission ()
  {
    permission = permission.getIncreased();
  }

  public void decreasePermission ()
  {
    permission = permission.getDecreased();
  }

  public static User getCurrentUser ()
  {
    return (User) FacesContext.getCurrentInstance().getExternalContext()
        .getSessionMap().get("user");
  }

  public static User getUserByName (String userName)
  {
    User user = null;

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_USER_BY_NAME);
      query.setString("userName", userName);
      user = (User) query.uniqueResult();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return user;
  }

  public static boolean loginUser (String userName, String password)
  {
    User user = getUserByName(userName);
    // User doesn't exist
    if (user == null) {
      return false;
    }
    // Password is incorrect
    if (!DigestUtils.md5Hex(password + user.salt).equals(user.password)) {
      return false;
    }

    FacesContext.getCurrentInstance().getExternalContext()
        .getSessionMap().put("user", user);

    return true;
  }

  public static void reloadUser (User user)
  {
    User newUser = User.getUserByName(user.getUserName());
    FacesContext.getCurrentInstance().getExternalContext()
        .getSessionMap().put("user", newUser);
  }

  @Transient
  public void logout ()
  {
    // There is probably a better way to do this
    brokerMap = new HashMap<Integer, Broker>();
    userId = -1;
    userName = "Guest";
    password = "";
    permission = PERMISSION.guest;
  }

  public String save ()
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.saveOrUpdate(this);
      transaction.commit();
      return "Success";
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      return "Failure";
    }
    finally {
      session.close();
    }
  }

  public void setPasswordAndSalt (String password)
  {
    String genSalt = DigestUtils.md5Hex(Math.random() + (new Date()).toString());

    setPassword(DigestUtils.md5Hex(password + genSalt));
    setSalt(genSalt);
  }

  //<editor-fold desc="Collections">
  @OneToMany
  @JoinColumn(name = "userId")
  @MapKey(name = "brokerId")
  public Map<Integer, Broker> getBrokerMap ()
  {
    return brokerMap;
  }
  public void setBrokerMap (Map<Integer, Broker> brokerMap)
  {
    this.brokerMap = brokerMap;
  }

  @SuppressWarnings("unchecked")
  public static List<User> getUserList ()
  {
    List<User> users = new ArrayList<User>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_USERS);
      users = (List<User>) query.list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return users;
  }
  //</editor-fold>

  //<editor-fold desc="Bean Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "userId", unique = true, nullable = false)
  public int getUserId ()
  {
    return userId;
  }
  public void setUserId (int userId)
  {
    this.userId = userId;
  }

  @Column(name = "userName", nullable = false)
  public String getUserName ()
  {
    return userName;
  }
  public void setUserName (String userName)
  {
    this.userName = userName;
  }

  @Column(name = "institution")
  public String getInstitution ()
  {
    return institution;
  }
  public void setInstitution (String institution)
  {
    this.institution = institution;
  }

  @Column(name = "contactName")
  public String getContactName ()
  {
    return contactName;
  }
  public void setContactName (String contactName)
  {
    this.contactName = contactName;
  }

  @Column(name = "contactEmail")
  public String getContactEmail ()
  {
    return contactEmail;
  }
  public void setContactEmail (String contactEmail)
  {
    this.contactEmail = contactEmail;
  }

  @Column(name = "contactPhone")
  public String getContactPhone ()
  {
    return contactPhone;
  }
  public void setContactPhone (String contactPhone)
  {
    this.contactPhone = contactPhone;
  }

  @Column(name = "salt", nullable = false)
  public String getSalt ()
  {
    return salt;
  }
  public void setSalt (String salt)
  {
    this.salt = salt;
  }

  @Column(name = "password", nullable = false)
  public String getPassword ()
  {
    return password;
  }
  public void setPassword (String password)
  {
    this.password = password;
  }

  @Column(name = "permission", nullable = false)
  @Enumerated(EnumType.STRING)
  public PERMISSION getPermission ()
  {
    return permission;
  }
  public void setPermission (PERMISSION permission)
  {
    this.permission = permission;
  }
  //</editor-fold>

  //<editor-fold desc="Web Setters and Getters">
  @Transient
  public boolean isEditingBroker ()
  {
    return isEditingBroker;
  }
  public void setEditingBroker (boolean editing)
  {
    isEditingBroker = editing;
  }

  @Transient
  public boolean isEditingDetails ()
  {
    return isEditingDetails;
  }
  public void setEditingDetails (boolean editingDetails)
  {
    isEditingDetails = editingDetails;
  }

  @Transient
  public boolean isGuest ()
  {
    return permission.compareTo(PERMISSION.broker) > 0 || userId == -1;
  }

  @Transient
  public boolean isLoggedIn ()
  {
    return permission.compareTo(PERMISSION.broker) <= 0 && userId != -1;
  }

  @Transient
  public boolean isAdmin ()
  {
    return permission == PERMISSION.admin && userId != -1;
  }

  public void setPermissionBroker ()
  {
    permission = PERMISSION.broker;
  }
  //</editor-fold>
}
