package org.powertac.tournament.beans;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.HibernateUtil;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "poms")
public class Pom implements Serializable
{
  private int pomId;
  private String pomName;

  public Pom ()
  {
  }

  @SuppressWarnings("unchecked")
  public static List<Pom> getPomList ()
  {
    List<Pom> poms = new ArrayList<Pom>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_POMS);
      poms = (List<Pom>) query.list();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return poms;
  }

  @Transient
  public String pomFileName ()
  {
    return "pom." + pomId + ".xml";
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "pomId", unique = true, nullable = false)
  public int getPomId ()
  {
    return pomId;
  }

  public void setPomId (int pomId)
  {
    this.pomId = pomId;
  }

  @Column(name = "pomName", unique = true, nullable = false)
  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    this.pomName = pomName;
  }
}
