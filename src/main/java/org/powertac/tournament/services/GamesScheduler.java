package org.powertac.tournament.services;

import org.powertac.tournament.beans.Agent;
import org.powertac.tournament.beans.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GamesScheduler
{
  public static List<Game> getBootableGames (List<Integer> runningRoundIds,
                                             List<Game> notCompleteGames)
  {
    List<Game> games = new ArrayList<Game>();

    for (Game game : notCompleteGames) {
      if (!game.isBootPending()) {
        continue;
      }
      if (runningRoundIds != null && runningRoundIds.size() > 0 &&
          !runningRoundIds.contains(game.getRound().getRoundId())) {
        continue;
      }
      games.add(game);
    }

    orderGames(games);

    return games;
  }

  /*
 * This function returns a list of all startable games in order of urgency.
 * the urgency of a game is the sum of the startable games of all brokers in
 * that game. This favors the bigger games (more brokers) since they'll have
 * a higher sum, and it favors the games with brokers that still have a
 * lot of games to do.
 */
  @SuppressWarnings("unchecked")
  public static List<Game> getStartableGames (List<Integer> runningRoundIds,
                                              List<Game> notCompleteGames)
  {
    List<Game> games = new ArrayList<Game>();

    if (runningRoundIds == null || runningRoundIds.size() == 0) {
      return games;
    }

    for (Game game : notCompleteGames) {
      if (game.isBootComplete() &&
          game.getStartTime().before(Utils.offsetDate()) &&
          runningRoundIds.contains(game.getRound().getRoundId())) {
        games.add(game);
      }
    }

    orderGames(games);

    return games;
  }

  public static void orderGames (List<Game> games)
  {
    // count the appearances of each broker in the runnable games of
    // each tournament
    Map<Integer, Map<Integer, Integer>> appearances = countAppearances(games);

    // calculate the urgencies of the games as the total number of runnable
    // games its brokers are playing in
    setUrgencies(games, appearances);

    // sort all games based on their urgency
    Collections.sort(games, new CustomGameComparator());
  }

  /*
   * Given a list of games (in practice all runnable games), this function
   * creates a double Map that shows how many runnable games each broker has in
   * any running tournament.
   * The result is in this form:
   * Map<(tournamentID), Map<(brokerID), (number of appearances)>>
   */
  @SuppressWarnings("unchecked")
  private static Map<Integer, Map<Integer, Integer>> countAppearances (List<Game> games)
  {
    Map<Integer, Map<Integer, Integer>> appearances = new HashMap<Integer, Map<Integer, Integer>>();

    for (Game game : games) {
      int tournamentId = game.getRound().getTournamentId();
      Map<Integer, Integer> innerMap = appearances.get(tournamentId);
      if (innerMap == null) {
        innerMap = new HashMap<Integer, Integer>();
      }

      for (Agent agent : game.getAgentMap().values()) {
        int brokerId = agent.getBrokerId();
        if (innerMap.get(brokerId) == null) {
          innerMap.put(brokerId, 1);
        }
        else {
          innerMap.put(brokerId, innerMap.get(brokerId) + 1);
        }
      }

      appearances.put(tournamentId, innerMap);
    }

    return appearances;
  }

  /*
   * This function calculates the urgencies of the given games.
   * The input is a list of games for which the urgency needs to be calculated
   * and a double map 'appearances' that stores for every
   * tournament/broker-combination how often it appears in the currently
   * startable games. The urgency is the sum of the appearances of all
   * brokers in that tournament.
   */
  @SuppressWarnings("unchecked")
  private static void setUrgencies (List<Game> games,
                                    Map<Integer, Map<Integer, Integer>> appearances)
  {
    for (Game game : games) {
      game.setUrgency(0);
      for (Agent agent : game.getAgentMap().values()) {
        game.setUrgency(game.getUrgency() +
            appearances.get(game.getRound().getTournamentId())
                .get(agent.getBrokerId()));
      }
    }
  }

  private static class CustomGameComparator implements Comparator<Game>
  {
    public int compare (Game game1, Game game2)
    {
      if (game1.getUrgency() > game2.getUrgency()) {
        return -1;
      }
      if (game1.getUrgency() < game2.getUrgency()) {
        return 1;
      }

      // If urgency is equal, take the largest game
      if (game1.getAgentMap().size() > game2.getAgentMap().size()) {
        return -1;
      }
      if (game1.getAgentMap().size() < game2.getAgentMap().size()) {
        return 1;
      }

      return 0;
    }
  }
}