package org.powertac.tourney.actions;

import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@RequestScoped
public class ActionGame
{
  private Game game = null;

  public ActionGame()
  {
  }

  private void getGame (int gameId)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    session.beginTransaction();
    try {
      game = (Game) session.get(Game.class, gameId);
      session.getTransaction().commit();
    }
    catch (ConstraintViolationException e) {
      e.printStackTrace();
      session.getTransaction().rollback();
    }
  }

  public String getGameName (int gameId)
  {
    if (game == null) {
      getGame(gameId);
    }

    try {
      return game.getGameName();
    }
    catch (Exception ignored) {
      redirect();
      return "";
    }
  }

  public List<String> getGameInfo(int gameId)
  {
    List<String> results = new ArrayList<String>();

    if (game == null) {
      getGame(gameId);
    }

    if (game == null) {
      redirect();
      return results;
    }

    Tournament tournament = null;
    Machine machine = null;
    Session session = HibernateUtil.getSessionFactory().openSession();
    session.beginTransaction();
    try {
      tournament = (Tournament)
          session.get(Tournament.class, game.getTourneyId());
      if (game.getMachineId() != null) {
        machine = (Machine) session.get(Machine.class, game.getMachineId());
      }
      session.getTransaction().commit();
    }
    catch (Exception e) {
      e.printStackTrace();
      session.getTransaction().rollback();
    }

    results.add("Status : " + game.getStatus());
    if (tournament != null) {
      results.add("Tournament : <a href=\"tournament.xhtml?tournamentId=" +
          + tournament.getTournamentId() + "\">"
          + tournament.getTournamentName() + "</a>");
    }
    if (machine != null) {
      results.add("Running on : " + machine.getName());
    }
    results.add("StartTime : " + Utils.dateFormat(game.getStartTime()));
    results.add("Real StartTime : " + Utils.dateFormat(game.getReadyTime()));

    return results;
  }

  public List<Map.Entry<String, Double>> getBrokerMap(int gameId)
  {
    List<Map.Entry<String, Double>> results =
        new ArrayList<Map.Entry<String, Double>>();

    if (game == null) {
      getGame(gameId);
    }

    if (game == null) {
      redirect();
      return results;
    }

    Map<String, Double> temp = new HashMap<String, Double>();
    for (Broker b: game.getBrokersInGame()) {
      Agent a = b.getAgent(game.getGameId());

      if (a.getBalance() < 0) {
        continue;
      }

      if (temp.get(b.getBrokerName()) != null) {
        temp.put(b.getBrokerName(), (temp.get(b.getBrokerName()) + a.getBalance()));
      } else {
        temp.put(b.getBrokerName(), a.getBalance());
      }
    }

    for (Map.Entry<String, Double> entry: temp.entrySet()) {
      entry.setValue(Math.floor(entry.getValue() * 10000)/10000);
      results.add(entry);
    }

    return results;
  }

  public void redirect ()
  {
    try {
      ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
      externalContext.redirect("index.xhtml");
    }
    catch (Exception ignored) {}
  }
}
