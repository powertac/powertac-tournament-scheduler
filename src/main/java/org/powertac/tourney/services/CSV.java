/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 2/28/13
 * Time: 12:29 PM
 */

package org.powertac.tourney.services;

import org.powertac.tourney.beans.Agent;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

public class CSV
{
  public static void createCsv (Tournament tournament)
  {
    TournamentProperties properties = TournamentProperties.getProperties();
    String logLocation = properties.getProperty("logLocation");
    String name1 = logLocation + "%s.csv";
    String name2 = logLocation + "%s.games.csv";

    createTournamentCsv(tournament, name1);
    createGamesCsv(tournament, name2, properties);
  }

  private static void createTournamentCsv (Tournament t, String name)
  {
    String sep = ";" + System.getProperty("line.separator");
    File tournamentCSV = new File(String.format(name, t.getTournamentName()));

    if (tournamentCSV.isFile() && tournamentCSV.canRead()) {
      tournamentCSV.delete();
    }

    // Create new CSVs
    try {
      tournamentCSV.createNewFile();

      FileWriter fw = new FileWriter(tournamentCSV.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      bw.write("tournamentId;" + t.getTournamentId() + sep);
      bw.write("tournamentName;" + t.getTournamentName() + sep);
      bw.write("status;" + t.getState() + sep);

      bw.write("StartTime;" + Utils.dateToStringFull(t.getStartTime()) + sep);
      bw.write("Date from;" + Utils.dateToStringFull(t.getDateFrom()) + sep);
      bw.write("Date to;" + Utils.dateToStringFull(t.getDateTo()) + sep);

      bw.write("MaxBrokers;" + t.getMaxBrokers() + sep);
      bw.write("Registered Brokers;" + t.getBrokerMap().size() + sep);
      bw.write("MaxAgents;" + t.getMaxAgents() + sep);

      bw.write("type;" + t.getType() + sep);
      if (t.isMulti()) {
        bw.write("size1;" + t.getSize1() + sep);
        bw.write("multiplier1;" + t.getMultiplier1() + sep);
        bw.write("size2;" + t.getSize2() + sep);
        bw.write("multiplier2;" + t.getMultiplier2() + sep);
        bw.write("size3;" + t.getSize3() + sep);
        bw.write("multiplier3;" + t.getMultiplier3() + sep);
      }

      bw.write("pomId;" + t.getPomId() + sep);
      bw.write("Locations;" + t.getLocations() + sep);
      bw.write(sep);

      Map<String, Double[]> resultMap = t.determineWinner();
      if (t.isMulti()) {
        List<Double> avgsAndSDs = t.getAvgsAndSDs(resultMap);
        bw.write("Average type 1;" + avgsAndSDs.get(0) + sep);
        bw.write("Average type 2;" + avgsAndSDs.get(1) + sep);
        bw.write("Average type 3;" + avgsAndSDs.get(2) + sep);

        bw.write("Standard deviation type 1;" + avgsAndSDs.get(3) + sep);
        bw.write("Standard deviation type 2;" + avgsAndSDs.get(4) + sep);
        bw.write("Standard deviation type 3;" + avgsAndSDs.get(5) + sep);
        bw.write(sep);

        bw.write("brokerId;brokerName;Size 1;Size 2;Size 3;" +
            "Total (not normalized);Size 1;Size 2;Size3;Total (normalized)" +
            sep);

        for (Map.Entry<String, Double[]> entry: resultMap.entrySet()) {
          Double[] results = entry.getValue();
          bw.write(String.format("%s;%s;%f;%f;%f;%f;%f;%f;%f;%f%s",
              entry.getKey().split(",")[0], entry.getKey().split(",")[1],
              results[0], results[1], results[2], results[3],
              results[10], results[11], results[12], results[13],
              sep));
        }
      } else {
        for (Map.Entry<String, Double[]> entry: resultMap.entrySet()) {
          bw.write("brokerId;Total" + sep);
          Double[] results = entry.getValue();
          bw.write(String.format("%s;%f;%s",
              entry.getKey(), results[0], sep));
        }
      }

      bw.close();

      copyFile(tournamentCSV, String.format(name, t.getTournamentId()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void createGamesCsv (Tournament t, String name,
                                      TournamentProperties properties)
  {
    String sep = System.getProperty("line.separator");
    File gamesCSV = new File(String.format(name, t.getTournamentName()));

    if (gamesCSV.isFile() && gamesCSV.canRead()) {
      gamesCSV.delete();
    }

    try {
      gamesCSV.createNewFile();

      FileWriter fw = new FileWriter(gamesCSV.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      bw.write(
          "gameId;gameName;status;gameSize;gameLength;lastTick;" +
              "weatherLocation;weatherDate;logUrl;brokerId;brokerBalance;"
              + sep);

      String tourneyUrl = properties.getProperty("tourneyUrl");
      String baseUrl = properties.getProperty("actionIndex.logUrl",
          "download?game=%d");
      for (Game game: t.getGameMap().values()) {
        String logUrl = "";
        if (game.isComplete()) {
          if (baseUrl.startsWith("http://")) {
            logUrl = String.format(baseUrl, game.getGameId());
          } else {
            logUrl = tourneyUrl + String.format(baseUrl, game.getGameId());
          }
        }

        String content = String.format("%d;%s;%s;%d;%d;%d;%s;%s;%s;",
            game.getGameId(), game.getGameName(), game.getState(),
            game.getAgentMap().size(), game.getGameLength(), game.getLastTick(),
            game.getLocation(), game.getSimStartTime(), logUrl);
        for (Agent agent: game.getAgentMap().values()) {
          content = String.format("%s%d;%f;", content,
              agent.getBrokerId(), agent.getBalance());
        }

        bw.write(content + sep);
      }

      bw.close();

      copyFile(gamesCSV, String.format(name, t.getTournamentId()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void copyFile (File sourceFile, String name)
      throws IOException
  {
    File targetFile = new File(name);

    if(!targetFile.exists()) {
      targetFile.createNewFile();
    }

    FileChannel source = null;
    FileChannel destination = null;

    try {
      source = new FileInputStream(sourceFile).getChannel();
      destination = new FileOutputStream(targetFile).getChannel();
      destination.transferFrom(source, 0, source.size());
    }
    finally {
      if (source != null) {
        source.close();
      }
      if (destination != null) {
        destination.close();
      }
    }
  }
}