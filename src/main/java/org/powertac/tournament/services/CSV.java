package org.powertac.tournament.services;

import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Round.Result;
import org.powertac.tournament.beans.Tournament;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CSV
{
  private String logLocation;
  private String baseUrl;
  private TournamentProperties properties;
  private String sep;
  private String[] names;

  public CSV (Tournament t, Round r)
  {
    properties = TournamentProperties.getProperties();
    sep = ";" + System.getProperty("line.separator");
    logLocation = properties.getProperty("logLocation");
    baseUrl = properties.getProperty("actionIndex.logUrl",
        "download?game=%d");
    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("game"));

    if (t != null) {
      String name = "%stournament.%s.csv";
      String levels = "%srounds.%s.csv";
      names = new String[]{
          String.format(name, logLocation, t.getTournamentName().replaceAll(" ", "_")),
          String.format(name, logLocation, t.getTournamentId()),
          String.format(levels, logLocation, t.getTournamentName().replaceAll(" ", "_")),
          String.format(levels, logLocation, t.getTournamentId())
      };
    }
    else if (r != null) {
      String name = "%sround.%s.csv";
      String games = "%sgames.%s.csv";
      names = new String[]{
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

      PrintWriter writer = new PrintWriter(tournamentCSV, "UTF-8");

      writer.println("tournamentId;" + tournament.getTournamentId() + sep);
      writer.println("tournamentName;" + tournament.getTournamentName() + sep);
      writer.println("status;" + tournament.getState() + sep);
      writer.println("pomId;" + tournament.getPomId() + sep);
      writer.println("MaxAgents;" + tournament.getMaxAgents() + sep);
      writer.println();

      writer.println("levelId;levelName;levelNr;nofRounds;nofWinners;startTime");
      for (Level level : tournament.getLevelMap().values()) {
        writer.println(String.format("%s;%s;%s;%s;%s;%s",
            level.getLevelId(),
            level.getLevelName(),
            level.getLevelNr(),
            level.getNofRounds(),
            level.getNofWinners(),
            Utils.dateToStringFull(level.getStartTime())));
      }

      writer.close();
      // provide file by tournamentId and name
      copyFile(tournamentCSV, names[1]);
    }
    catch (Exception e) {
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

      PrintWriter writer = new PrintWriter(levelsCSV, "UTF-8");
      for (Level level : tournament.getLevelMap().values()) {
        writer.println("levelId;" + level.getLevelId());
        writer.println("levelName;" + level.getLevelName());
        writer.println("levelNr;" + level.getLevelNr());
        writer.println("nofWinners;" + level.getNofWinners());
        writer.println("startTime;" + Utils.dateToStringFull(level.getStartTime()));
        writer.println();
        for (Round round : level.getRoundMap().values()) {
          Map<Broker, Result> resultMap = round.getResultMap();
          singleRound(writer, ";", round, resultMap);
          writer.println();
        }
      }
      writer.close();
      // provide file by levelId and name
      copyFile(levelsCSV, names[3]);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void roundCsv (Round round)
  {
    File roundCSV = new File(names[0]);
    if (roundCSV.isFile() && roundCSV.canRead()) {
      roundCSV.delete();
    }

    Map<Broker, Result> resultMap = round.getResultMap();
    if (resultMap.size() == 0) {
      return;
    }

    // Create new CSVs
    try {
      roundCSV.createNewFile();
      PrintWriter writer = new PrintWriter(roundCSV, "UTF-8");
      singleRound(writer, "", round, resultMap);
      writer.close();
      // provide file by roundId and name
      copyFile(roundCSV, names[1]);
    }
    catch (Exception e) {
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

      PrintWriter writer = new PrintWriter(gamesCSV, "UTF-8");

      writer.print("gameId;gameName;status;gameSize;gameLength;lastTick;");
      writer.println("weatherLocation;weatherDate;logUrl;brokerId;brokerBalance;");

      String tourneyUrl = properties.getProperty("tourneyUrl");
      String baseUrl = properties.getProperty("actionIndex.logUrl",
          "download?game=%d");
      for (Game game : round.getGameMap().values()) {
        String logUrl = "";
        if (game.getState().isComplete()) {
          if (baseUrl.startsWith("http://")) {
            logUrl = String.format(baseUrl, game.getGameId());
          }
          else {
            logUrl = tourneyUrl + String.format(baseUrl, game.getGameId());
          }
        }

        String content = String.format("%d;%s;%s;%d;%d;%d;%s;%s;%s;",
            game.getGameId(), game.getGameName(), game.getState(),
            game.getSize(), game.getGameLength(), game.getLastTick(),
            game.getLocation(), game.getSimStartTime(), logUrl);
        for (Agent agent : game.getAgentMap().values()) {
          content = String.format("%s%d;%f;", content,
              agent.getBrokerId(), agent.getBalance());
        }

        writer.println(content);
      }

      writer.close();

      // provide file by gameId and name
      copyFile(gamesCSV, names[3]);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void singleRound (PrintWriter writer, String prefix,
                            Round round, Map<Broker, Result> resultMap)
      throws IOException
  {
    writeRoundParams (writer, prefix, round);

    if (resultMap == null || resultMap.size() == 0) {
      return;
    }

    writeRoundBrokers (writer, prefix, round, resultMap);
  }

  private void writeRoundParams (PrintWriter writer, String prefix, Round round)
  {
    writer.println(prefix + "roundId;" + round.getRoundId());
    writer.println(prefix + "roundName;" + round.getRoundName());
    writer.println(prefix + "status;" + round.getState());

    writer.println(prefix + "StartTime;" + Utils.dateToStringFull(round.getStartTime()));
    writer.println(prefix + "Date from;" + Utils.dateToStringFull(round.getDateFrom()));
    writer.println(prefix + "Date to;" + Utils.dateToStringFull(round.getDateTo()));

    writer.println(prefix + "MaxBrokers;" + round.getMaxBrokers());
    writer.println(prefix + "Registered Brokers;" + round.getBrokerMap().size());

    writer.println(prefix + "size1;" + round.getSize1());
    writer.println(prefix + "multiplier1;" + round.getMultiplier1());
    writer.println(prefix + "size2;" + round.getSize2());
    writer.println(prefix + "multiplier2;" + round.getMultiplier2());
    writer.println(prefix + "size3;" + round.getSize3());
    writer.println(prefix + "multiplier3;" + round.getMultiplier3());

    writer.println(prefix + "pomId;" + round.getPomId());
    writer.println(prefix + "Locations;" + round.getLocations());
    writer.println();
  }

  private void writeRoundBrokers (PrintWriter writer, String prefix,
                                  Round round, Map<Broker, Result> resultMap)
  {
    Result roundResults = resultMap.remove(null);
    if (roundResults == null) {
      return;
    }

    double[] means = roundResults.getArray0();
    double[] sdevs = roundResults.getArray1();

    writer.println(prefix + "Average type 1;" + means[0]);
    writer.println(prefix + "Average type 2;" + means[1]);
    writer.println(prefix + "Average type 3;" + means[2]);

    writer.println(prefix + "Standard deviation type 1;" + sdevs[0]);
    writer.println(prefix + "Standard deviation type 2;" + sdevs[1]);
    writer.println(prefix + "Standard deviation type 3;" + sdevs[2]);
    writer.println();

    writer.println(prefix + "brokerId;brokerName;Size 1;Size 2;Size 3;" +
        "Total (not normalized);Size 1;Size 2;Size3;Total (normalized)");

    for (Broker broker : round.rankList()) {
      Result result = resultMap.get(broker);
      double[] notNorm = result.getArray0();
      double[] norm = result.getArray0();
      double[] totals = result.getArray2();

      writer.println(String.format("%s%s;%s;%f;%f;%f;%f;%f;%f;%f;%f",
          prefix, broker.getBrokerId(), broker.getBrokerName(),
          notNorm[0], notNorm[1], notNorm[2], totals[0],
          norm[0], norm[1], norm[2], totals[1]));
    }
  }

  private void copyFile (File sourceFile, String name)
      throws IOException
  {
    File targetFile = new File(name);

    if (!targetFile.exists()) {
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
    List<String> csvLinks = new ArrayList<>();

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
    List<String> csvLinks = new ArrayList<>();

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