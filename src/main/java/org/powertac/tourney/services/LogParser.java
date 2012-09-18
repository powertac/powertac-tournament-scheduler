/**
 * Created by IntelliJ IDEA.
 * User: Govert Buijs
 * Date: 8/21/12
 * Time: 12:04 PM
 */
package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.powertac.tourney.beans.Agent;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class LogParser implements Runnable
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int gameId;
  String logLocation;
  String fileName;

  public LogParser (String logLocation, String fileName)
  {
    gameId = Integer.parseInt(fileName.split("-")[1]);
    this.logLocation = logLocation;
    this.fileName = fileName;
  }

  public void run() {
    String copyCmd = String.format("cp %s%s /tmp/%s",
        logLocation, fileName, fileName);
    String untarCmd = "tar -C /tmp/ -xzvf /tmp/" + fileName;

    Runtime runtime = Runtime.getRuntime();
    Process proc;

    // Copy tar to /tmp
    try {
      proc = runtime.exec(copyCmd);
      proc.waitFor();
      log.debug("Done copying");
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    // Extract tar, files will be in /tmp/log
    try {
      proc = runtime.exec(untarCmd);
      proc.waitFor();
      log.debug("Done untarring");
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    // Extract final balances from '.trace', and store results
    try {
      HashMap<String, Double> results =
          extractResults("/tmp/log/powertac-sim-" + gameId + ".trace");
      log.debug("Done extracting");
      storeResults(results);
      log.debug("Done storing");
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    // Cleanup
    File f = new File("/tmp/log/powertac-sim-" + gameId + ".trace");
    f.delete();
    f = new File("/tmp/log/powertac-sim-" + gameId + ".state");
    f.delete();
    f = new File("/tmp/" + fileName);
    f.delete();
    log.debug("Done cleaning");
  }

  public HashMap<String, Double> extractResults (String fileName) throws Exception
  {
    HashMap<String, Double> results = new HashMap<String, Double>();
    String finalBalance = "server.CompetitionControlService: Final balance";
    FileInputStream fstream = new FileInputStream(fileName);
    DataInputStream in = new DataInputStream(fstream);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));

    String strLine;
    while ((strLine = br.readLine()) != null) {
      if (strLine.contains(finalBalance)) {
        String balances = strLine.split("\\[")[1].split("\\]")[0].trim();

        for (String result: balances.split(" \"")) {
          Double balance = Double.parseDouble(result.split(":")[1]);
          String name = result.split(":")[0];
          if (name.startsWith("\"")) {
            name = name.substring(1);
          }
          if (name.endsWith("\"")) {
            name = name.substring(0, name.length()-1);
          }

          if (name.equals("default broker")) {
            continue;
          }

          results.put(name, balance);
        }
      }
    }
    fstream.close();
    in.close();
    br.close();

    return results;
  }

  @SuppressWarnings("unchecked")
  public void storeResults (HashMap<String, Double> results)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Game game = (Game) session.get(Game.class, gameId);

      for (Map.Entry<String, Double> entry: results.entrySet()) {
        Broker broker = (Broker) session
            .createCriteria(Broker.class)
            .add(Restrictions.eq("brokerName", entry.getKey())).uniqueResult();

        Agent agent = (Agent) session.createCriteria(Agent.class)
            .add(Restrictions.eq("broker", broker))
            .add(Restrictions.eq("game", game)).uniqueResult();

        // Apperantly the end-of-game message has already been recieved
        if (agent.getBalance() != -1) {
          transaction.rollback();
          return;
        }

        agent.setBalance(entry.getValue());
        session.update(agent);
      }
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