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
    String base = "<a href=\"tournament.xhtml?tournamentId=%s\">%s</a>";

    competitionInfo.add("Id : " + competition.getCompetitionId());
    competitionInfo.add("Name : " + competition.getCompetitionName());
    competitionInfo.add("Status : " + competition.getState());
    competitionInfo.add("Pom Id : " + competition.getPomId() +"<br/><br/>");

    for (CompetitionRound round: competition.getRoundMap().values()) {
      competitionInfo.add("Round " + round.getRoundNr()
          + " : " + round.getRoundName());
      competitionInfo.add("Tournaments / winners : "
          + round.getNofTournaments() + " / " + round.getNofWinners());

      for (Tournament tournament: round.getTournamentMap().values()) {
        competitionInfo.add("Tournament : " +
            String.format(base,
                tournament.getTournamentId(), tournament.getTournamentName())
            + "<br/>StartTime : " + tournament.getStartTime()
            + "<br/>Status : " + tournament.getState()
        );
      }

      int last = competitionInfo.size() - 1;
      competitionInfo.set(last, competitionInfo.get(last) + "<br/><br/>");
    }
  }

  public List<Broker> getAllowedBrokers ()
  {
    List<Broker> allowedBrokers = new ArrayList<Broker>();

    // Check if we have an authorized user
    User user = User.getCurrentUser();
    if (user == null || !user.isLoggedIn()) {
      return allowedBrokers;
    }

    // Check if we have an open competition
    if (competition == null || !competition.isOpen()) {
      return allowedBrokers;
    }

    // Find before-deadline tournaments
    List<Tournament> tournaments = new ArrayList<Tournament>();
    for (CompetitionRound round: competition.getRoundMap().values()) {
      for (Tournament tournament: round.getTournamentMap().values()) {
        if (tournament.getStartTime().before(Utils.offsetDate(-2))) {
          continue;
        }
        tournaments.add(tournament);
      }
    }
    // No tournaments found
    if (tournaments.size() == 0) {
      return allowedBrokers;
    }

    // Find non-registered brokers
    for (Broker broker: user.getBrokerMap().values()) {
      boolean brokerRegistered = false;
      for (Tournament tournament: tournaments) {
        if (tournament.getBrokerMap().get(broker.getBrokerId()) != null) {
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
    // Find least filled tournament
    Tournament leastFilledTournament = null;
    CompetitionRound round = competition.getRoundMap().get(0);
    for (Tournament tournament: round.getTournamentMap().values()) {
      if (leastFilledTournament == null ||
          leastFilledTournament.getBrokerMap().size() <
              tournament.getBrokerMap().size()) {
        leastFilledTournament = tournament;
      }
    }

    if (leastFilledTournament == null) {
      message(1, "Registering failed, try again or contact the game master");
      return;
    }

    if (leastFilledTournament.getBrokerMap().get(broker.getBrokerId()) != null){
      message(1, "Registering failed, already registered for this competition");
      return;
    }

    broker.register(leastFilledTournament.getTournamentId());

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
  //</editor-fold>
}
