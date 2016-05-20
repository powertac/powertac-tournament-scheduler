package org.powertac.tournament.jobs;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;


public class ParserSimlog extends Thread
{
  private static Logger log = Utils.getLogger();

  private static String finalBalance =
      "server.CompetitionControlService: Final balance";
  private static String gameLength =
      "server.CompetitionControlService: game-length";
  private static String lastTick =
      "server.CompetitionControlService: Wait for tick";

  private String pathString;

  public ParserSimlog (String pathString)
  {
    this.pathString = pathString;
  }

  public void run ()
  {
    // Not a sim-log
    if (!pathString.contains("sim.tar.gz")) {
      return;
    }

    log.debug("Extracting result from " + pathString);
    HashMap<String, Double> results = getResults();
    storeResults(results);
  }

  private HashMap<String, Double> getResults ()
  {
    HashMap<String, Double> results = new HashMap<>();

    try (
      InputStream is = new GZIPInputStream(new FileInputStream(pathString));
      BufferedReader in = new BufferedReader(new InputStreamReader(is));
    ) {

      String line;
      while ((line = in.readLine()) != null) {
        handleLine(results, line);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return results;
  }

  private void handleLine (HashMap<String, Double> results, String line)
  {
    if (line.contains(gameLength)) {
      String length = line.split("game-length ")[1].split("\\(")[0];
      length = length.replace("fixed: ", "");
      results.put("gameLength###", Double.parseDouble(length));
    }

    else if (line.contains(lastTick)) {
      String tick = line.split("Wait for tick ")[1].split("\\(")[0];
      results.put("lastTick###", Double.parseDouble(tick));
    }

    else if (line.contains(finalBalance)) {
      String balances = line.split("\\[")[1].split("\\]")[0].trim();

      for (String result : balances.split(" \"")) {
        Double balance = Double.parseDouble(result.split(":")[1]);
        String name = result.split(":")[0];
        if (name.startsWith("\"")) {
          name = name.substring(1);
        }
        if (name.endsWith("\"")) {
          name = name.substring(0, name.length() - 1);
        }

        if (name.equals("default broker")) {
          continue;
        }

        results.put(name, balance);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void storeResults (HashMap<String, Double> results)
  {
    if (results.size() == 0) {
      return;
    }

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      int gameId = Integer.parseInt(pathString.split("-")[1]);
      Game game = (Game) session.get(Game.class, gameId);

      Double gameLength = results.get("gameLength###");
      if (game.getGameLength() == 0 && gameLength != null) {
        log.debug("Setting gameLength to " + gameLength.intValue());
        game.setGameLength(gameLength.intValue());
      }

      Double lastTick = results.get("lastTick###");
      if (game.getLastTick() == 0 && lastTick != null) {
        log.debug("Setting lastTick to " + lastTick.intValue());
        game.setLastTick(lastTick.intValue());
      }

      for (Agent agent : game.getAgentMap().values()) {
        Double balance = results.get(agent.getBroker().getBrokerName());
        if (agent.getBalance() == 0 && balance != null) {
          log.debug("Setting balance to " + balance + " for broker " +
              agent.getBroker().getBrokerName());
          agent.setBalance(balance);
          session.update(agent);
        }
      }

      session.update(game);
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
}