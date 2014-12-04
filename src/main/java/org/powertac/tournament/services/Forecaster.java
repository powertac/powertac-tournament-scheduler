package org.powertac.tournament.services;

import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Machine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Forecaster
{
  // TODO Calculate overhead
  // TODO Get duration from where??
  private static int overhead = 60;
  private static int duration = 5;

  // TODO Make configurable via interface ??
  private int slavesCount = Machine.getMachineList().size();

  private List<List<Game>> machines;
  private Map<Integer, Game> gamesMap;
  private Map<Integer, Long> startTimes;
  private Map<Integer, Long> endTimes;

  public Forecaster (Map<Integer, Game> gamesMap)
  {
    this.gamesMap = gamesMap;
    init();
  }

  private void init ()
  {
    machines = new ArrayList<List<Game>>();
    for (int i = 0; i < slavesCount; i++) {
      machines.add(new ArrayList<Game>());
    }

    startTimes = new HashMap<Integer, Long>();
    endTimes = new HashMap<Integer, Long>();
    for (int gameId : gamesMap.keySet()) {
      startTimes.put(gameId, -1L);
      endTimes.put(gameId, -1L);
    }

    // Add the completed and running games
    int machineIndex = 0;
    for (Game game : gamesMap.values()) {
      if (game.isRunning() || game.isComplete()) {
        long timeStart = (game.getReadyTime().getTime() / 1000) - overhead;
        long timeEnd = getTimeEnd(game, timeStart);
        addJob(game, machineIndex++, timeStart, timeEnd);
      }
    }
  }

  public void createSchedule ()
  {
    // Get a list of all the games we still need to schedule
    List<Game> unfinished = new ArrayList<Game>();
    for (Game game : gamesMap.values()) {
      if (!game.isRunning() && !game.isComplete()) {
        unfinished.add(game);
      }
    }

    // Find the first available slot, and schedule game
    long timeStart = Utils.offsetDate().getTime() / 1000 - 1;
    loop:
    while (unfinished.size() > 0) {
      timeStart += 1;

      // Find index and time of machine that comes available first
      long[] indexAndTime = getIndexAndTime();
      int machineId = (int) indexAndTime[0];
      timeStart = Math.max(timeStart, indexAndTime[1]);

      // Order the games according to urgency
      GamesScheduler.orderGames(unfinished);

      // Find first game we're able to run
      for (Game game : unfinished) {
        long timeEnd = getTimeEnd(game, timeStart);
        boolean gameRunnable = isRunnable(game, timeStart, timeEnd);
        if (gameRunnable) {
          addJob(game, machineId, timeStart, timeEnd);
          unfinished.remove(game);
          continue loop;
        }
      }
    }
  }

  private void addJob (Game game, int machineId, long timeStart, long timeEnd)
      throws ScheduleException
  {
    if (!isRunnable(game, timeStart, timeEnd)) {
      throw new ScheduleException("Game not runnable!");
    }

    List<Game> machine = machines.get(machineId);

    if (machine.size() > 0) {
      Game lastGame = machine.get(machine.size() - 1);
      long lastTimeEnd = endTimes.get(lastGame.getGameId());
      if (lastTimeEnd >= timeStart) {
        throw new ScheduleException("Last job not yet finished!");
      }
    }

    machine.add(game);
    startTimes.put(game.getGameId(), timeStart);
    endTimes.put(game.getGameId(), timeEnd);
  }

  private boolean isRunnable (Game game, long time, long timeEnd)
  {
    if (game.getStartTime().after(new Date(time * 1000))) {
      return false;
    }

    int maxAgents = game.getRound().getLevel().getTournament().getMaxAgents();

    while (time <= timeEnd) {
      for (int brokerId : game.getAgentMap().keySet()) {
        int agentsBusy = agentsBusy(brokerId, time);
        if (agentsBusy == maxAgents) {
          return false;
        }
        else if (agentsBusy > maxAgents) {
          throw new ScheduleException("Too many agents busy!");
        }
      }
      time += 1;
    }
    return true;
  }

  private int agentsBusy (int brokerId, long time)
  {
    int busy = 0;

    for (List<Game> machine : machines) {
      for (Game game : machine) {
        if (startTimes.get(game.getGameId()) <= time &&
            endTimes.get(game.getGameId()) >= time &&
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
      long timeEnd = endTimes.get(last.getGameId());
      if (max > timeEnd || max == -1) {
        max = timeEnd + 1;
        index = i;
      }
    }

    return new long[]{index, max};
  }

  public Map<Integer, Game> getGamesMap ()
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

  // TODO Subclass from different Exception
  public class ScheduleException extends UnsupportedOperationException
  {
    public ScheduleException (String message)
    {
      super(message);
    }
  }
}