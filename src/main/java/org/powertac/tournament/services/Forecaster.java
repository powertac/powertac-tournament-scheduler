package org.powertac.tournament.services;

import org.powertac.tournament.actions.ActionTimeline;
import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Broker;
import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Level;
import org.powertac.tournament.beans.Location;
import org.powertac.tournament.beans.Machine;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Tournament;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.powertac.tournament.services.Scheduler.createGamesAgents;


public class Forecaster
{
  // TODO Calculate overhead
  // TODO Get duration from where??
  private static int overhead = 60;
  private static int duration = 5;

  // TODO Make configurable via interface ??
  private int slavesCount = Machine.getMachineList().size();

  private List<List<Game>> machines;
  private Forecast forecast;

  public Forecaster (TreeMap<Integer, Game> gamesMap)
  {
    this.forecast = new Forecast(gamesMap);
    init();
  }

  private void init ()
  {
    machines = new ArrayList<List<Game>>();
    for (int i = 0; i < slavesCount; i++) {
      machines.add(new ArrayList<Game>());
    }

    forecast.startTimes = new HashMap<Integer, Long>();
    forecast.endTimes = new HashMap<Integer, Long>();
    for (int gameId : forecast.gamesMap.keySet()) {
      forecast.startTimes.put(gameId, -1L);
      forecast.endTimes.put(gameId, -1L);
    }

    // Add the completed and running games
    int machineIndex = 0;
    for (Game game : forecast.gamesMap.values()) {
      if (game.isRunning() || game.isComplete()) {
        long timeStart = (game.getReadyTime().getTime() / 1000) - overhead;
        long timeEnd = getTimeEnd(game, timeStart);
        addJob(game, machineIndex++, timeStart, timeEnd);
      }
    }
  }

  private void createSchedule ()
  {
    // Get a list of all the games we still need to schedule
    List<Game> unfinished = new ArrayList<Game>();
    for (Game game : forecast.gamesMap.values()) {
      if (!game.isRunning() && !game.isComplete()) {
        unfinished.add(game);
      }
    }

    // Order the games according to urgency
    GamesScheduler.orderGames(unfinished);

    // Find the first available slot, and schedule game
    long timeStart = Utils.offsetDate().getTime() / 1000;

    while (unfinished.size() > 0) {
      // Find index and time of machine that comes available first
      long[] indexAndTime = getIndexAndTime();
      int machineId = (int) indexAndTime[0];
      timeStart = Math.max(timeStart, indexAndTime[1]);

      // Find first game we're able to run
      for (Game game : unfinished) {
        long timeEnd = getTimeEnd(game, timeStart);
        boolean gameRunnable = isRunnable(game, timeStart, timeEnd);
        if (gameRunnable) {
          addJob(game, machineId, timeStart, timeEnd);
          unfinished.remove(game);
          // Order the games according to urgency
          GamesScheduler.orderGames(unfinished);
          break;
        }
      }

      // Only need to check when games start or finish
      List<Long> times = forecast.getTimes(timeStart, null);
      if (times.isEmpty() || times.get(0) == timeStart) {
        timeStart += 1;
      }
      else {
        timeStart = times.get(0);
      }
    }
  }

  private void addJob (Game game, int machineId, long timeStart, long timeEnd)
  {
    if (!isRunnable(game, timeStart, timeEnd)) {
      // TODO
      System.out.println("Game " + game.getGameId() + " not runnable!");
      return;
    }

    List<Game> machine = machines.get(machineId);

    if (machine.size() > 0) {
      Game lastGame = machine.get(machine.size() - 1);
      long lastTimeEnd = forecast.endTimes.get(lastGame.getGameId());
      if (lastTimeEnd >= timeStart) {
        // TODO
        System.out.println("Game " + game.getGameId() + " not runnable!\n" +
            "Last job not yet finished!");
      }
    }

    machine.add(game);
    forecast.startTimes.put(game.getGameId(), timeStart);
    forecast.endTimes.put(game.getGameId(), timeEnd);
  }

  private boolean isRunnable (Game game, long time, long timeEnd)
  {
    if (game.getStartTime().after(new Date(time * 1000))) {
      return false;
    }

    int maxAgents = game.getRound().getLevel().getTournament().getMaxAgents();

    List<Long> times = forecast.getTimes(time, timeEnd);
    times.add(0, time);

    while (!times.isEmpty()) {
      time = times.remove(0);
      for (int brokerId : game.getAgentMap().keySet()) {
        int agentsBusy = agentsBusy(brokerId, time);
        if (agentsBusy == maxAgents) {
          return false;
        }
        else if (agentsBusy > maxAgents) {
          // TODO
          System.out.println("Too many agents busy!");
        }
      }
    }
    return true;
  }

  private int agentsBusy (int brokerId, long time)
  {
    int busy = 0;

    for (List<Game> machine : machines) {
      for (Game game : machine) {
        if (forecast.startTimes.get(game.getGameId()) <= time &&
            forecast.endTimes.get(game.getGameId()) >= time &&
            game.getAgentMap().keySet().contains(brokerId)) {
          busy += 1;
        }
      }
    }

    return busy;
  }

  private long getTimeEnd (Game game, long timeStart)
  {
    return timeStart + game.getGameLength() * duration + overhead;
  }

  // Find the index of the 'machine' that will be available first
  private long[] getIndexAndTime ()
  {
    long max = -1;
    int index = -1;
    for (int i = 0; i < machines.size(); i++) {
      List<Game> machine = machines.get(i);

      // The machine hasn't been used so far
      if (machine.size() == 0) {
        return new long[]{i, 0};
      }

      // Let's look at the last time the machine is being used
      Game last = machine.get(machine.size() - 1);
      long timeEnd = forecast.endTimes.get(last.getGameId());
      if (max > timeEnd || max == -1) {
        max = timeEnd + 1;
        index = i;
      }
    }

    return new long[]{index, max};
  }

  public static Forecast createForRunning ()
  {
    // Get the games of all running rounds
    TreeMap<Integer, Game> gamesMap = new TreeMap<Integer, Game>();
    Scheduler scheduler = Scheduler.getScheduler();
    for (Round round : scheduler.getRunningRounds()) {
      gamesMap.putAll(round.getGameMap());
    }

    Forecaster forecaster = new Forecaster(gamesMap);
    forecaster.createSchedule();

    forecaster.forecast.writeSchedule("running");
    return forecaster.forecast;
  }

  public static Forecast createForRound (Integer roundId, String paramsString,
                                         String date, String name)
  {
    String[] params = paramsString.split("_");
    int maxBrokers = Integer.valueOf(params[0]);
    int maxAgents = Integer.valueOf(params[1]);
    int[] gameTypes = new int[]{Integer.valueOf(params[2]),
        Integer.valueOf(params[4]), Integer.valueOf(params[6])};
    int[] multipliers = new int[]{Integer.valueOf(params[3]),
        Integer.valueOf(params[5]), Integer.valueOf(params[7])};

    Tournament tournament = new Tournament();
    tournament.setTournamentId(1);
    tournament.setMaxAgents(maxAgents);
    Level level = new Level();
    level.setLevelId(1);
    level.setTournament(tournament);
    tournament.getLevelMap().put(level.getLevelId(), level);

    List<Broker> brokers = new ArrayList<Broker>();
    for (int i = 0; i < maxBrokers; i++) {
      Broker broker = new Broker();
      broker.setBrokerId(i + 1);
      brokers.add(broker);
    }

    // Get the games for all the rounds
    TreeMap<Integer, Game> gamesMap = new TreeMap<Integer, Game>();
    getGames(gamesMap, level, brokers, roundId, gameTypes, multipliers,
        Utils.stringToDateMedium(date), name);

    if (gamesMap.size() > 500) {
      return null;
    }

    Forecaster forecaster = new Forecaster(gamesMap);
    forecaster.createSchedule();

    forecaster.forecast.writeSchedule(name);
    return forecaster.forecast;
  }

  public static Map<Integer, Forecast> createForLevel (Level orgLevel)
  {
    // Create dummy tournament and level
    Tournament tournament = new Tournament();
    tournament.setTournamentId(1);
    tournament.setMaxAgents(orgLevel.getTournament().getMaxAgents());
    Level level = new Level();
    level.setLevelId(1);
    level.setTournament(tournament);
    tournament.getLevelMap().put(level.getLevelId(), level);

    // Get the games for all the rounds
    TreeMap<Integer, Game> gamesMap = new TreeMap<Integer, Game>();

    for (Round round : orgLevel.getRoundMap().values()) {
      int[] gameTypes = new int[]{
          round.getSize1(), round.getSize2(), round.getSize3() };
      int[] multipliers = new int[]{
          round.getMultiplier1(), round.getMultiplier2(), round.getMultiplier3()};

      List<Broker> brokers = new ArrayList<Broker>();
      for (Broker orgBroker : round.getBrokerMap().values()) {
        Broker broker = new Broker();
        broker.setBrokerId(orgBroker.getBrokerId());
        brokers.add(broker);
      }

      getGames(gamesMap, level, brokers, round.getRoundId(),
          gameTypes, multipliers, round.getStartTime(), round.getRoundName());
    }

    if (gamesMap.size() > 500) {
      return null;
    }

    Forecaster forecaster = new Forecaster(gamesMap);
    forecaster.createSchedule();

    Forecast forecastTotal = forecaster.forecast;
    ActionTimeline.setForecast(forecastTotal, "Level " + orgLevel.getLevelName());

    // Split in a separate forecast for each round
    Map<Integer, Forecast> forecastMap = new HashMap<Integer, Forecast>();
    for (int roundId : level.getRoundMap().keySet()) {
      forecastMap.put(roundId, new Forecast(new TreeMap<Integer, Game>()));
    }
    for (Game game : forecastTotal.gamesMap.values()) {
      int gameId = game.getGameId();
      int roundId = game.getRound().getRoundId();

      Forecast tmp = forecastMap.get(roundId);
      tmp.gamesMap.put(gameId, game);
      tmp.startTimes.put(gameId, forecastTotal.startTimes.get(gameId));
      tmp.endTimes.put(gameId, forecastTotal.endTimes.get(gameId));
    }

    return forecastMap;
  }

  private static void getGames (TreeMap<Integer, Game> gamesMap,
                                Level level, List<Broker> brokers, int roundId,
                                int[] gameTypes, int[] multipliers,
                                Date startTime, String name)
  {
    Round round = new Round();
    round.setRoundId(roundId);
    round.setStartTime(startTime);
    round.setRoundName(name);
    round.setLocations(new Location().getLocation());
    round.setLevel(level);
    level.getRoundMap().put(roundId, round);

    List<Game> games = new ArrayList<Game>();
    for (int i = 0; i < (gameTypes.length); i++) {
      for (int j = 0; j < multipliers[i]; j++) {
        createGamesAgents(round, brokers, i, gameTypes[i], j, games);
      }
    }

    // Set ids and game lengths
    int gameIdx = gamesMap.size() + 1;
    int agentIdx = 1;
    for (Game game : games) {
      for (Agent agent : game.getAgentMap().values()) {
        agent.setAgentId(agentIdx++);
        agent.setBrokerId(agent.getBroker().getBrokerId());
      }

      game.setGameId(gameIdx++);
      round.getGameMap().put(game.getGameId(), game);
      game.setRound(round);
      game.setGameLength(game.computeGameLength(name));

      gamesMap.put(game.getGameId(), game);
    }
  }

  // Data object for Forecaster
  public static class Forecast
  {
    private TreeMap<Integer, Game> gamesMap;
    private Map<Integer, Long> startTimes;
    private Map<Integer, Long> endTimes;

    public Forecast (TreeMap<Integer, Game> gamesMap)
    {
      this.gamesMap = gamesMap;
      this.startTimes = new HashMap<Integer, Long>();
      this.endTimes = new HashMap<Integer, Long>();
    }

    public TreeMap<Integer, Game> getGamesMap ()
    {
      return gamesMap;
    }

    public Map<Integer, Long> getStartTimes ()
    {
      return startTimes;
    }

    public Map<Integer, Long> getEndTimes ()
    {
      return endTimes;
    }

    // Find the end of the schedule
    private long getScheduleStart ()
    {
      long min = -1;
      for (long startTime : startTimes.values()) {
        if (startTime < min || min == -1) {
          min = startTime;
        }
      }

      return min;
    }

    // Find the end of the schedule
    public long getScheduleEnd ()
    {
      long max = -1;
      for (long endTime : endTimes.values()) {
        if (endTime > max || max == -1) {
          max = endTime + 1;
        }
      }

      return max;
    }

    public List<Long> getTimes (long timeStart, Long timeEnd)
    {
      List<Long> times = new ArrayList<Long>();

      for (long time : startTimes.values()) {
        if (time >= timeStart) {
          if (timeEnd != null && time > timeEnd) {
            continue;
          }
          times.add(time);
        }
      }

      for (long time : endTimes.values()) {
        if (time >= timeStart) {
          if (timeEnd != null && time > timeEnd) {
            continue;
          }
          times.add(time);
          times.add(time + 1);
        }
      }

      return times;
    }

    public String getForecastString ()
    {
      Date timeStart = new Date(1000 * getScheduleStart());
      Date timeEnd = new Date(1000 * getScheduleEnd());
      long seconds = (timeEnd.getTime() - timeStart.getTime()) / 1000;
      return String.format("Start : %s<br/>End   : %s<br/>Total : %d:%02d:%02d",
          Utils.dateToStringMedium(timeStart), Utils.dateToStringMedium(timeEnd),
          seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    // TODO Only for development
    public void writeSchedule (String name)
    {
      if (gamesMap.isEmpty()) {
        return;
      }

      try {
        String now = Utils.dateToStringMedium(new Date());
        String fileName = String.format(
            "/home/govert/Projects/powertac/files/schedules/%s_%s.txt",
            now.replace(" ", "_").replace(":", "-"), name);
        PrintWriter writer = new PrintWriter(fileName);

        for (Game game : gamesMap.values()) {
          int gameId = game.getGameId();
          Date start = Utils.dateFromLong(startTimes.get(gameId));
          Date end = Utils.dateFromLong(endTimes.get(gameId));

          writer.println(String.format("%4d %s %s",
              gameId, Utils.dateToStringFull(start), Utils.dateToStringFull(end)));
        }

        writer.close();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
