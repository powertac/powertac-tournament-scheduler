package org.powertac.tourney.actions;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.MemStore;
import org.powertac.tourney.services.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
@RequestScoped
public class ActionCompetition
{
  private Competition competition;
  private List<String> competitionInfo = new ArrayList<String>();
  private List<String> participantInfo = new ArrayList<String>();

  private static boolean editing;
  private String content;

  public ActionCompetition ()
  {
    loadData();
  }

  private void loadData ()
  {
    int competitionId = getCompetitionId();
    if (competitionId < 1) {
      return;
    }

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_COMPETITION_BY_ID);
      query.setInteger("competitionId", competitionId);
      competition = (Competition) query.uniqueResult();

      if (competition == null) {
        transaction.rollback();
        Utils.redirect();
        return;
      }

      loadCompetitionInfo();
      loadParticipantInfo();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    } finally {
      session.close();
    }
  }

  private int getCompetitionId ()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("competitionId"));
    } catch (NumberFormatException ignored) {
      if (!FacesContext.getCurrentInstance().isPostback()) {
        Utils.redirect();
      }
      return -1;
    }
  }

  private void loadCompetitionInfo ()
  {
    String base = "<a href=\"round.xhtml?roundId=%s\">%s</a>";

    competitionInfo.add("Id : " + competition.getCompetitionId());
    competitionInfo.add("Name : " + competition.getCompetitionName());
    competitionInfo.add("Status : " + competition.getState());
    competitionInfo.add("Pom Id : " + competition.getPomId() +"<br/><br/>");

    for (Level level : competition.getLevelMap().values()) {
      competitionInfo.add("Level " + level.getLevelNr()
          + " : " + level.getLevelName());
      competitionInfo.add("Rounds / winners : "
          + level.getNofRounds() + " / " + level.getNofWinners());

      for (Round round : level.getRoundMap().values()) {
        competitionInfo.add("Round : " +
            String.format(base,
                round.getRoundId(), round.getRoundName())
            + "<br/>StartTime (UTC) : " + round.startTimeUTC().substring(0, 16)
            + "<br/>Status : " + round.getState()
        );
      }

      int last = competitionInfo.size() - 1;
      competitionInfo.set(last, competitionInfo.get(last) + "<br/><br/>");
    }
  }

  private void loadParticipantInfo ()
  {
    for (Level level : competition.getLevelMap().values()) {
      if (level.getLevelNr() != 0) {
        continue;
      }

      for (Round round : level.getRoundMap().values()) {
        for (Broker broker: round.getBrokerMap().values()) {
          User participant = broker.getUser();
          participantInfo.add(String.format("%s, %s, %s",
              broker.getBrokerName(),
              participant.getInstitution(),participant.getContactName()));
        }
      }
    }

    java.util.Collections.sort(participantInfo);
  }

  public List<Broker> getAllowedBrokers ()
  {
    List<Broker> allowedBrokers = new ArrayList<Broker>();

    // Check if max allowed brokers reached
    if (competition.getLevelMap().get(0).getNofBrokers() >=
        competition.getLevelMap().get(0).getMaxBrokers()) {
      return allowedBrokers;
    }

    // Check if we have an authorized user
    User user = User.getCurrentUser();
    if (user == null || !user.isLoggedIn()) {
      return allowedBrokers;
    }

    // Check if we have an open competition
    // TODO Check this
    if (competition == null || !competition.isOpen()) {
      return allowedBrokers;
    }

    // Find before-deadline round
    List<Round> rounds = new ArrayList<Round>();
    for (Level level : competition.getLevelMap().values()) {
      for (Round round : level.getRoundMap().values()) {
        if (round.getStartTime().before(Utils.offsetDate(-2))) {
          continue;
        }
        rounds.add(round);
      }
    }
    // No rounds found
    if (rounds.size() == 0) {
      return allowedBrokers;
    }

    // Find non-registered brokers
    for (Broker broker: user.getBrokerMap().values()) {
      boolean brokerRegistered = false;
      for (Round round : rounds) {
        if (round.getBrokerMap().get(broker.getBrokerId()) != null) {
          brokerRegistered = true;
        }
      }
      if (!brokerRegistered) {
        allowedBrokers.add(broker);
      }
    }

    return allowedBrokers;
  }

  public void register (Broker broker)
  {
    // Find least filled round
    Round leastFilledRound = null;
    Level level = competition.getLevelMap().get(0);
    for (Round round : level.getRoundMap().values()) {
      if (leastFilledRound == null ||
          leastFilledRound.getBrokerMap().size() >
              round.getBrokerMap().size()) {
        leastFilledRound = round;
      }
    }

    if (leastFilledRound == null) {
      message(1, "Registering failed, try again or contact the game master");
      return;
    }

    if (leastFilledRound.getBrokerMap().get(broker.getBrokerId()) != null){
      message(1, "Registering failed, already registered for this competition");
      return;
    }

    broker.register(leastFilledRound.getRoundId());

    Utils.redirect("competition.xhtml?competitionId=" + getCompetitionId());
  }

  //<editor-fold desc="Edit content">
  public void edit ()
  {
    if (editing) {
      if (!MemStore.setCompetitionContent(content, getCompetitionId())) {
        message(0, "Error saving to DB");
        return;
      }
    }
    editing = !editing;
  }

  public void cancel ()
  {
    editing = false;
  }

  public boolean isEditing ()
  {
    return editing;
  }

  public String getContent ()
  {
    if (content == null) {
      return MemStore.getCompetitionContent(getCompetitionId());
    }
    return content;
  }

  public void setContent (String content)
  {
    this.content = content;
  }
  //</editor-fold>

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("contentForm", fm);
    }
    else if (field == 1) {
      FacesContext.getCurrentInstance().addMessage("registerForm", fm);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public Competition getCompetition ()
  {
    return competition;
  }

  public List<String> getCompetitionInfo ()
  {
    return competitionInfo;
  }

  public List<String> getParticipantInfo ()
  {
    return participantInfo;
  }
  //</editor-fold>
}
