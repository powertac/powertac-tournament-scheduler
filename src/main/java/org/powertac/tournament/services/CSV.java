package org.powertac.tournament.services;

import org.powertac.tournament.beans.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CSV
{
  private String logLocation;
  private String baseUrl;
  private TournamentProperties properties;
  private String separator;
  private String[] names;

  public CSV (Tournament t, Round r)
  {
    properties = TournamentProperties.getProperties();
    separator = ";" + System.getProperty("line.separator");
    logLocation = properties.getProperty("logLocation");
    baseUrl = properties.getProperty("actionIndex.logUrl",
        "download?game=%d");
    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("game"));

    if (t != null) {
      String name = "%stournament.%s.csv";
      String levels = "%srounds.%s.csv";
      names = new String[] {
          String.format(name, logLocation, t.getTournamentName().replaceAll(" ", "_")),
          String.format(name, logLocation, t.getTournamentId()),
          String.format(levels, logLocation, t.getTournamentName().replaceAll(" ", "_")),
          String.format(levels, logLocation, t.getTournamentId())
      };
    }
    else if (r != null) {
      String name = "%sround.%s.csv";
      String games = "%sgames.%s.csv";
      names = new String[] {
          String.format(name, logLocation, r.getRoundName().replaceAll(" ", "_")),
          String.format(name, logLocation, r.getRoundId()),
          String.format(games, logLocation, r.getRoundName().replaceAll(" ", "_")),
          String.format(games, logLocation, r.getRoundId())
      };
    }
  }

  public static void createTournamentCsv (Tournament tournament)
  {
    CSV csv = new CSV(tournament, null);

    csv.tournamentCsv(tournament);
    csv.levelsCsv(tournament);
  }

  public static void createRoundCsv (Round round)
  {
    CSV csv = new CSV(null, round);

    csv.roundCsv(round);
    csv.gamesCsv(round);
  }

  public static List<String> getTournamentCsvLinks (Tournament tournament)
  {
    CSV csv = new CSV(tournament, null);
    return csv.getTournamentCsvLinks();
  }

  public static List<String> getRoundCsvLinks (Round round)
  {
    CSV csv = new CSV(null, round);
    return csv.getRoundCsvLinks();
  }

  private void tournamentCsv (Tournament tournament)
  {
    File tournamentCSV = new File(names[0]);

    if (tournamentCSV.isFile() && tournamentCSV.canRead()) {
      tournamentCSV.delete();
    }

    // Create new CSVs
    try {
      tournamentCSV.createNewFile();

      FileWriter fw = new FileWriter(tournamentCSV.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      bw.write("tournamentId;" + tournament.getTournamentId() + separator);
      bw.write("tournamentName;" + tournament.getTournamentName() + separator);
      bw.write("status;" + tournament.getState() + separator);
      bw.write("pomId;" + tournament.getPomId() + separator);
      bw.write("MaxAgents;" + tournament.getMaxAgents() + separator);

      bw.write(separator);

      bw.write("levelId;levelName;levelNr;nofRounds;nofWinners;startTime"
          + separator);
      for (Level level: tournament.getLevelMap().values()) {
        bw.write(String.format("%s;%s;%s;%s;%s;%s%s",
            level.getLevelId(),
            level.getLevelName(),
            level.getLevelNr(),
            level.getNofRounds(),
            level.getNofWinners(),
            Utils.dateToStringFull(level.getStartTime()),
            separator));
      }

      bw.close();

      copyFile(tournamentCSV, names[1]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void levelsCsv (Tournament tournament)
  {
    File levelsCSV = new File(names[2]);

    if (levelsCSV.isFile() && levelsCSV.canRead()) {
      levelsCSV.delete();
    }

    try {
      levelsCSV.createNewFile();

      FileWriter fw = new FileWriter(levelsCSV.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      for (Level level: tournament.getLevelMap().values()) {
        bw.write("levelId;" + level.getLevelId() + separator);
        bw.write("levelName;" + level.getLevelName() + separator);
        bw.write("levelNr;" + level.getLevelNr() + separator);
        bw.write("nofWinners;" + level.getNofWinners() + separator);
        bw.write("startTime;" + Utils.dateToStringFull(level.getStartTime()) + separator);
        bw.write(separator);
        for (Round round: level.getRoundMap().values()) {
          Map<Broker, double[]> resultMap = round.determineWinner();
          singleRound (bw, ";", round, resultMap);
          bw.write(separator);
        }
      }

      bw.close();

      copyFile(levelsCSV, names[3]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void roundCsv (Round round)
  {
    File roundCSV = new File(names[0]);

    if (roundCSV.isFile() && roundCSV.canRead()) {
      roundCSV.delete();
    }

    Map<Broker, double[]> resultMap = round.determineWinner();
    if (resultMap.size() == 0) {
      return;
    }

    // Create new CSVs
    try {
      roundCSV.createNewFile();

      FileWriter fw = new FileWriter(roundCSV.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      singleRound (bw, "", round, resultMap);

      bw.close();

      copyFile(roundCSV, names[1]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void gamesCsv (Round round)
  {
    File gamesCSV = new File(names[2]);

    if (gamesCSV.isFile() && gamesCSV.canRead()) {
      gamesCSV.delete();
    }

    if (round.getSize() == 0) {
      return;
    }

    try {
      gamesCSV.createNewFile();

      FileWriter fw = new FileWriter(gamesCSV.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      bw.write(
          "gameId;gameName;status;gameSize;gameLength;lastTick;" +
              "weatherLocation;weatherDate;logUrl;brokerId;brokerBalance;"
              + separator);

      String tourneyUrl = properties.getProperty("tourneyUrl");
      String baseUrl = properties.getProperty("actionIndex.logUrl",
          "download?game=%d");
      for (Game game: round.getGameMap().values()) {
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
            game.getSize(), game.getGameLength(), game.getLastTick(),
            game.getLocation(), game.getSimStartTime(), logUrl);
        for (Agent agent: game.getAgentMap().values()) {
          content = String.format("%s%d;%f;", content,
              agent.getBrokerId(), agent.getBalance());
        }

        bw.write(content + separator);
      }

      bw.close();

      copyFile(gamesCSV, names[3]);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void singleRound (BufferedWriter bw, String prefix, Round round,
                            Map<Broker, double[]> resultMap)
      throws IOException
  {
    bw.write(prefix + "roundId;" + round.getRoundId() + separator);
    bw.write(prefix + "roundName;" + round.getRoundName() + separator);
    bw.write(prefix + "status;" + round.getState() + separator);

    bw.write(prefix + "StartTime;" + Utils.dateToStringFull(round.getStartTime()) + separator);
    bw.write(prefix + "Date from;" + Utils.dateToStringFull(round.getDateFrom()) + separator);
    bw.write(prefix + "Date to;" + Utils.dateToStringFull(round.getDateTo()) + separator);

    bw.write(prefix + "MaxBrokers;" + round.getMaxBrokers() + separator);
    bw.write(prefix + "Registered Brokers;" + round.getBrokerMap().size() + separator);

    bw.write(prefix + "size1;" + round.getSize1() + separator);
    bw.write(prefix + "multiplier1;" + round.getMultiplier1() + separator);
    bw.write(prefix + "size2;" + round.getSize2() + separator);
    bw.write(prefix + "multiplier2;" + round.getMultiplier2() + separator);
    bw.write(prefix + "size3;" + round.getSize3() + separator);
    bw.write(prefix + "multiplier3;" + round.getMultiplier3() + separator);

    bw.write(prefix + "pomId;" + round.getPomId() + separator);
    bw.write(prefix + "Locations;" + round.getLocations() + separator);

    double[] avgsAndSDs = round.getAvgsAndSDsArray(resultMap);
    if (resultMap == null || resultMap.size() == 0 || avgsAndSDs == null) {
      return;
    }

    bw.write(separator);

    bw.write(prefix + "Average type 1;" + avgsAndSDs[0] + separator);
    bw.write(prefix + "Average type 2;" + avgsAndSDs[1] + separator);
    bw.write(prefix + "Average type 3;" + avgsAndSDs[2] + separator);

    bw.write(prefix + "Standard deviation type 1;" + avgsAndSDs[3] + separator);
    bw.write(prefix + "Standard deviation type 2;" + avgsAndSDs[4] + separator);
    bw.write(prefix + "Standard deviation type 3;" + avgsAndSDs[5] + separator);
    bw.write(separator);

    bw.write(prefix + "brokerId;brokerName;Size 1;Size 2;Size 3;" +
        "Total (not normalized);Size 1;Size 2;Size3;Total (normalized)" +
        separator);

    for (Map.Entry<Broker, double[]> entry: resultMap.entrySet()) {
      double[] results = entry.getValue();
      bw.write(String.format("%s%s;%s;%f;%f;%f;%f;%f;%f;%f;%f%s",
          prefix,
          entry.getKey().getBrokerId(), entry.getKey().getBrokerName(),
          results[0], results[1], results[2], results[3],
          results[10], results[11], results[12], results[13],
          separator));
    }
  }

  private void copyFile (File sourceFile, String name)
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

  private List<String> getTournamentCsvLinks ()
  {
    List<String> csvLinks = new ArrayList<String>();

    String tournamentCsv = names[0].replace(logLocation, "");
    String roundsCsv = names[2].replace(logLocation, "");

    File tournamentFile = new File(names[0]);
    File roundsFile = new File(names[2]);

    if (tournamentFile.exists()) {
      if (baseUrl.endsWith("?")) {
        tournamentCsv = "csv=" + tournamentCsv.replace(".csv", "");
      }
      else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      csvLinks.add(String.format(
          "Tournament csv : <a href=\"%s\">link</a>", baseUrl + tournamentCsv));
    }
    if (roundsFile.exists()) {
      if (baseUrl.endsWith("?")) {
        roundsCsv = "csv=" + roundsCsv.replace(".csv", "");
      }
      else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      csvLinks.add(String.format(
          "Rounds csv : <a href=\"%s\">link</a>", baseUrl + roundsCsv));
    }
    return csvLinks;
  }

  private List<String> getRoundCsvLinks ()
  {
    List<String> csvLinks = new ArrayList<String>();

    String roundCsv = names[0].replace(logLocation, "");
    String gamesCsv = names[2].replace(logLocation, "");

    File roundFile = new File(names[0]);
    File gamesFile = new File(names[2]);

    if (roundFile.exists()) {
      if (baseUrl.endsWith("?")) {
        roundCsv = "csv=" + roundCsv.replace(".csv", "");
      }
      else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      csvLinks.add(String.format(
          "Round csv : <a href=\"%s\">link</a>", baseUrl + roundCsv));
    }
    if (gamesFile.exists()) {
      if (baseUrl.endsWith("?")) {
        gamesCsv = "csv=" + gamesCsv.replace(".csv", "");
      }
      else if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      csvLinks.add(String.format(
          "Games csv : <a href=\"%s\">link</a>", baseUrl + gamesCsv));
    }
    return csvLinks;
  }
}