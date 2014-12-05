package org.powertac.tournament.actions;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Tournament;
import org.powertac.tournament.beans.User;
import org.powertac.tournament.constants.Constants;
import org.powertac.tournament.services.CSV;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Utils;
import org.springframework.beans.factory.InitializingBean;

import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;


@ManagedBean
public class ActionTournament implements InitializingBean
{
  private Tournament tournament;
  private List<String> tournamentInfo = new ArrayList<String>();
  private List<String> participantInfo = new ArrayList<String>();
  private List<String> csvLinks = new ArrayList<String>();

  private static boolean editing;
  private String content;

  public ActionTournament ()
  {
  }

  public void afterPropertiesSet () throws Exception
  {
    int tournamentId = getTournamentId();
    if (tournamentId < 1) {
      return;
    }

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_TOURNAMENT_BY_ID);
      query.setInteger("tournamentId", tournamentId);
      tournament = (Tournament) query.uniqueResult();

      if (tournament == null) {
        transaction.rollback();
        Utils.redirect();
        return;
      }

      loadTournamentInfo();
      loadParticipantInfo();
      loadCsvLinks();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    finally {
      session.close();
    }
  }

  private int getTournamentId ()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    try {
      return Integer.parseInt(facesContext.getExternalContext().
          getRequestParameterMap().get("tournamentId"));
    }
    catch (NumberFormatException ignored) {
      if (!FacesContext.getCurrentInstance().isPostback()) {
        Utils.redirect();
      }
      return -1;
    }
  }

  private void loadTournamentInfo ()
  {
    tournamentInfo.add("Id : " + tournament.getTournamentId());
    tournamentInfo.add("Name : " + tournament.getTournamentName());
    tournamentInfo.add("Status : " + tournament.getState());
    tournamentInfo.add("Pom Id : " + tournament.getPomId() + "<br/><br/>");

    for (Level level : tournament.getLevelMap().values()) {
      tournamentInfo.add("Level " + level.getLevelNr()
          + " : " + level.getLevelName());
      tournamentInfo.add("Rounds / winners : "
          + level.getNofRounds() + " / " + level.getNofWinners());

      for (Round round : level.getRoundMap().values()) {
        tournamentInfo.add(String.format("Round : " +
                "<a href=\"round.xhtml?roundId=%s\">%s</a>" +
                "<br/>StartTime (UTC) : %s<br/>Status : %s",
            round.getRoundId(),
            round.getRoundName(),
            round.startTimeUTC().substring(0, 16),
            round.getState()));
      }

      int last = tournamentInfo.size() - 1;
      tournamentInfo.set(last, tournamentInfo.get(last) + "<br/><br/>");
    }
  }

  private void loadParticipantInfo ()
  {
    for (Level level : tournament.getLevelMap().values()) {
      if (level.getLevelNr() != 0) {
        continue;
      }

      for (Round round : level.getRoundMap().values()) {
        for (Broker broker : round.getBrokerMap().values()) {
          User participant = broker.getUser();
          participantInfo.add(String.format("%s, %s, %s",
              broker.getBrokerName(),
              participant.getInstitution(), participant.getContactName()));
        }
      }
    }

    java.util.Collections.sort(participantInfo);
  }

  private void loadCsvLinks ()
  {
    csvLinks = CSV.getTournamentCsvLinks(tournament);
  }

  public void createCsv ()
  {
    CSV.createTournamentCsv(tournament);
  }

  public List<Broker> getAllowedBrokers ()
  {
    List<Broker> allowedBrokers = new ArrayList<Broker>();

    if (tournament == null) {
      Utils.redirect();
      return allowedBrokers;
    }

    // Check if max allowed brokers reached
    if (tournament.getLevelMap().get(0).getNofBrokers() >=
        tournament.getLevelMap().get(0).getMaxBrokers()) {
      return allowedBrokers;
    }

    // Check if we have an authorized user
    User user = User.getCurrentUser();
    if (user == null || !user.isLoggedIn()) {
      return allowedBrokers;
    }

    // Check if we have an open tournament
    if (!tournament.isOpen()) {
      return allowedBrokers;
    }

    // Find before-deadline round
    List<Round> rounds = new ArrayList<Round>();
    for (Level level : tournament.getLevelMap().values()) {
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
    for (Broker broker : user.getBrokerMap().values()) {
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

  // Registers a brokers for a tournament, and places it in one of the rounds
  public void register (Broker broker)
  {
    // Register for the tournament
    broker.registerForTournament(tournament.getTournamentId());

    // Find least filled round
    Round leastFilled = null;
    Level level = tournament.getLevelMap().get(0);
    for (Round round : level.getRoundMap().values()) {
      // This round can't accept another broker
      if (round.getBrokerMap().size() >= round.getMaxBrokers()) {
        continue;
      }

      // Pick least filled round
      if (leastFilled == null ||
          leastFilled.getBrokerMap().size() > round.getBrokerMap().size()) {
        leastFilled = round;
      }
    }

    if (leastFilled == null) {
      Utils.growlMessage("Registering failed.<br/>" +
          "Try again or contact the game master");
      return;
    }

    if (leastFilled.getBrokerMap().get(broker.getBrokerId()) != null) {
      Utils.growlMessage("Registering failed.<br/>" +
          "Already registered for this tournament");
      return;
    }

    // Register for the round (in first level)
    broker.registerForRound(leastFilled.getRoundId());

    Utils.redirect("tournament.xhtml?tournamentId=" + getTournamentId());
  }

  //<editor-fold desc="Edit content">
  public void edit ()
  {
    int tournamentId = getTournamentId();
    if (tournamentId < 1) {
      return;
    }

    if (editing) {
      if (!MemStore.setTournamentContent(content, tournamentId)) {
        Utils.growlMessage("Failed to save to DB.");
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
      return MemStore.getTournamentContent(getTournamentId());
    }
    return content;
  }

  public void setContent (String content)
  {
    this.content = content;
  }
  //</editor-fold>

  //<editor-fold desc="Setters and Getters">
  public Tournament getTournament ()
  {
    return tournament;
  }

  public List<String> getTournamentInfo ()
  {
    return tournamentInfo;
  }

  public List<String> getParticipantInfo ()
  {
    return participantInfo;
  }

  public List<String> getCsvLinks ()
  {
    return csvLinks;
  }
  //</editor-fold>
}
