package org.powertac.tournament.jobs;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.services.HibernateUtil;
import org.powertac.tournament.services.MemStore;
import org.powertac.tournament.services.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;


public class LogJob extends Thread
{
  private static Logger log = Utils.getLogger();

  private static String finalBalance =
      "server.CompetitionControlService: Final balance";
  private static String gameLength =
      "server.CompetitionControlService: game-length";
  private static String lastTick =
      "server.CompetitionControlService: Wait for tick";

  private String logLoc;
  private String fileName;
  private Game game;
  private Map<String, Double> results;
  private int BUFFER = 2048;
  private String tmpPath;

  public LogJob (String logLoc, String fileName)
  {
    this.logLoc = logLoc;
    this.fileName = fileName;
  }

  public void run ()
  {
    // Not a sim-log
    if (!fileName.contains(".tar.gz") || fileName.contains(".boot.")) {
      return;
    }

    try {
      log.debug("Extracting result from " + fileName);
      getResults();
      storeResults();
    }
    catch (Exception e) {
      System.out.println();
      System.out.println("Extracting results failed");
      e.printStackTrace();
    }

    try {
      log.debug("Merging logs to " + fileName);
      mergeLogFiles();
    }
    catch (Exception e) {
      System.out.println();
      System.out.println("Merging log files failed");
      e.printStackTrace();
    }
  }

  private void getResults ()
  {
    results = new HashMap<>();

    try (BufferedReader in = new BufferedReader(
        new InputStreamReader(
            new GZIPInputStream(new FileInputStream(logLoc + fileName))))) {
      String line;
      while ((line = in.readLine()) != null) {
        handleLine(line);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleLine (String line)
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
  private void storeResults ()
  {
    if (results.size() == 0) {
      return;
    }

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      int gameId = MemStore.getGameId(fileName.replace(".tar.gz", ""));
      game = (Game) session.get(Game.class, gameId);

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

  private void mergeLogFiles ()
  {
    File tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory("logs_").toFile();
      tmpPath = tmpDir.getAbsoluteFile() + "/";

      // Make sure the org log files are kept
      String orgFileName = fileName.replace(".tar.gz", ".org.tar.gz");
      new File(logLoc + fileName).renameTo(new File(logLoc + orgFileName));

      decompressTarGz(logLoc + orgFileName);
      decompressTarGz(logLoc + fileName.replace(".tar.gz", ".boot.tar.gz"));

      String bootLocation = Game.getBootLocation(game.getGameName());
      compressTarGz(logLoc + fileName, bootLocation);
    }
    catch (Exception e) {
      System.out.println();
      System.out.println("Error merging files");
      e.printStackTrace();
    }
    finally {
      if (tmpDir != null) {
        removeDirectory(tmpDir);
      }
    }
  }

  private void decompressTarGz (String tarGz)
  {
    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
        new GzipCompressorInputStream(
            new BufferedInputStream(new FileInputStream(tarGz))))) {

      // Make sure directories exist
      new File(tmpPath + "log").mkdirs();
      new File(tmpPath + "boot-log").mkdirs();

      TarArchiveEntry entry;
      while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
        // If the entry is a file, write to disk
        int count;
        byte[] data = new byte[BUFFER];

        FileOutputStream fos = new FileOutputStream(tmpPath + entry.getName());
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
        while ((count = tarIn.read(data, 0, BUFFER)) != -1) {
          dest.write(data, 0, count);
        }
        dest.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void compressTarGz (String tarGz, String xmlPath)
  {
    try (TarArchiveOutputStream tOut = new TarArchiveOutputStream(
        new GzipCompressorOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(new File(tarGz)))))) {
      addFileToTarGz(tOut, tmpPath + "log", "log");
      addFileToTarGz(tOut, tmpPath + "boot-log", "boot-log");
      addFileToTarGz(tOut, xmlPath, "");

      new File(tarGz).setReadable(true, false);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addFileToTarGz (TarArchiveOutputStream tOut, String path, String dir)
      throws IOException
  {
    File f = new File(path);
    if (f.isDirectory()) {
      TarArchiveEntry tarEntry = new TarArchiveEntry(f, f.getName());
      tOut.putArchiveEntry(tarEntry);
      tOut.closeArchiveEntry();

      File[] children = f.listFiles();
      if (children == null) {
        return;
      }
      for (File child : children) {
        addFileToTarGz(tOut, child.getAbsolutePath(), dir + "/");
      }
    }
    else if (f.isFile()) {
      TarArchiveEntry tarEntry = new TarArchiveEntry(f, dir + f.getName());
      tOut.putArchiveEntry(tarEntry);
      IOUtils.copy(new FileInputStream(f), tOut);
      tOut.closeArchiveEntry();
    }
  }

  private void removeDirectory (File dir)
  {
    if (dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null && files.length > 0) {
        for (File aFile : files) {
          removeDirectory(aFile);
        }
      }
    }

    dir.delete();
  }
}
