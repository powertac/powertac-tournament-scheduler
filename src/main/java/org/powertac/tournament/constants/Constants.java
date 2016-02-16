package org.powertac.tournament.constants;

import org.powertac.tournament.beans.Game;
import org.powertac.tournament.beans.Round;
import org.powertac.tournament.beans.Tournament;


public class Constants
{
  public static class Props
  {
    public static final String weatherServerURL =
        "server.weatherService.serverUrl = ";
    public static final String weatherLocation =
        "server.weatherService.weatherLocation = ";
    public static final String startTime =
        "common.competition.simulationBaseTime = ";
    public static final String jms =
        "server.jmsManagementService.jmsBrokerUrl = ";
    public static final String serverFirstTimeout =
        "server.competitionControlService.firstLoginTimeout = ";
    public static final String serverTimeout =
        "server.competitionControlService.loginTimeout = ";
    public static final String remote =
        "server.visualizerProxyService.remoteVisualizer = ";
    public static final String vizQ =
        "server.visualizerProxyService.visualizerQueueName = ";
    public static final String minTimeslot =
        "common.competition.minimumTimeslotCount = ";
    public static final String expectedTimeslot =
        "common.competition.expectedTimeslotCount = ";
    public static final String timezoneOffset =
        "common.competition.timezoneOffset = ";
  }

  public static class Rest
  {
    // Possible Rest Parameters for Broker Login
    public static final String REQ_PARAM_AUTH_TOKEN = "authToken";
    public static final String REQ_PARAM_JOIN = "requestJoin";
    public static final String REQ_PARAM_TYPE = "type";

    // Possible Rest Parameters for Viz Login
    public static final String REQ_PARAM_MACHINE_NAME = "machineName";
    public static final String REQ_PARAM_MACHINE_LOAD = "machineLoad";

    // Possible Rest Paramenters for Server Interface
    public static final String REQ_PARAM_STATUS = "status";
    public static final String REQ_PARAM_GAMEID = "gameId";
    public static final String REQ_PARAM_ACTION = "action";
    public static final String REQ_PARAM_FILENAME = "fileName";
    public static final String REQ_PARAM_MESSAGE = "message";
    public static final String REQ_PARAM_BOOT = "boot";
    public static final String REQ_PARAM_HEARTBEAT = "heartbeat";
    public static final String REQ_PARAM_GAMERESULTS = "gameresults";
    public static final String REQ_PARAM_GAMELENGTH = "gameLength";
    public static final String REQ_PARAM_STANDINGS = "standings";
    public static final String REQ_PARAM_ELAPSED_TIME = "elapsedTime";

    // Possible Rest Parameters for pom service
    public static final String REQ_PARAM_POM_ID = "pomId";
  }

  public static class HQL
  {
    public static final String GET_USERS =
        "FROM User AS user ";

    public static final String GET_USER_BY_NAME =
        "FROM User AS user "
            + "LEFT JOIN FETCH user.brokerMap AS brokerMap "
            + "LEFT JOIN FETCH brokerMap.roundMap AS roundMap "
            + "LEFT JOIN FETCH brokerMap.tournamentMap AS tournamentMap "
            + "LEFT JOIN FETCH tournamentMap.levelMap as levelMap "
            + "LEFT JOIN FETCH brokerMap.agentMap AS agentMap "
            + "WHERE user.userName =:userName ";

    public static final String GET_POMS =
        "FROM Pom AS pom "
            + "ORDER BY pom.pomId DESC ";

    public static final String GET_LOCATIONS =
        "FROM Location AS location ";

    public static final String GET_LOCATION_BY_NAME =
        "FROM Location AS location WHERE location.location =:locationName ";

    public static final String GET_MACHINES =
        "FROM Machine AS machine ";

    public static final String GET_TOURNAMENT_BY_ID =
        "FROM Tournament AS tournament "
            + "LEFT JOIN FETCH tournament.levelMap AS levelMap "
            + "LEFT JOIN FETCH levelMap.roundMap AS roundMap "
            + "LEFT JOIN FETCH roundMap.gameMap AS gameMap "
            + "LEFT JOIN FETCH gameMap.agentMap as agentMap "
            + "LEFT JOIN FETCH roundMap.brokerMap AS brokerMap "
            + "WHERE tournament.tournamentId =:tournamentId";

    public static final String GET_TOURNAMENT_BY_NAME =
        "FROM Tournament AS tournament "
            + "LEFT JOIN FETCH tournament.levelMap AS levelMap "
            + "LEFT JOIN FETCH levelMap.roundMap AS roundMap "
            + "LEFT JOIN FETCH tournament.brokerMap AS brokerMap "
            + "LEFT JOIN FETCH brokerMap.user AS user "
            + "WHERE tournament.tournamentName =:tournamentName";

    public static final String GET_TOURNAMENT_NOT_COMPLETE =
        "FROM Tournament AS tournament "
            + "LEFT JOIN FETCH tournament.levelMap AS levelMap "
            + "LEFT JOIN FETCH levelMap.roundMap AS roundMap "
            + "LEFT JOIN FETCH roundMap.gameMap as gameMap "
            + "LEFT JOIN FETCH gameMap.agentMap as agentMap "
            + "LEFT JOIN FETCH roundMap.brokerMap as brokerMap "
            + "LEFT JOIN FETCH brokerMap.user as user "
            + "WHERE NOT tournament.state='" + Tournament.getStateComplete() + "'";

    public static final String GET_LEVELS_NOT_COMPLETE =
        "FROM Level AS level "
            + "LEFT JOIN FETCH level.tournament as tournament "
            + "LEFT JOIN FETCH level.roundMap as roundMap "
            + "LEFT JOIN FETCH roundMap.gameMap as gameMap "
            + "LEFT JOIN FETCH gameMap.agentMap AS agentMap "
            + "LEFT JOIN FETCH gameMap.machine "
            + "LEFT JOIN FETCH agentMap.broker "
            + "LEFT JOIN FETCH agentMap.game "
            + "LEFT JOIN FETCH roundMap.brokerMap AS brokerMap "
            + "LEFT JOIN FETCH brokerMap.user "
            + "WHERE EXISTS ELEMENTS(level.roundMap) "
            + "AND NOT tournament.state='" + Tournament.getStateComplete() + "'";

    public static final String GET_ROUND_BY_ID =
        "FROM Round AS round "
            + "LEFT JOIN FETCH round.gameMap AS gameMap "
            + "LEFT JOIN FETCH round.brokerMap AS brokerMap "
            + "LEFT JOIN FETCH brokerMap.user "
            + "LEFT JOIN FETCH gameMap.agentMap AS agentMap "
            + "LEFT JOIN FETCH round.level AS level "
            + "LEFT JOIN FETCH level.tournament AS tournament "
            + "LEFT JOIN FETCH tournament.levelMap AS levelMap "
            + "LEFT JOIN FETCH levelMap.roundMap AS roundMap "
            + "LEFT JOIN FETCH roundMap.gameMap "
            + "WHERE round.roundId =:roundId";

    public static final String GET_ROUNDS_NOT_COMPLETE =
        "FROM Round AS round "
            + "LEFT JOIN FETCH round.level as level "
            + "LEFT JOIN FETCH level.roundMap as roundMap "
            + "LEFT JOIN FETCH round.gameMap as gameMap "
            + "LEFT JOIN FETCH gameMap.agentMap AS agentMap "
            + "LEFT JOIN FETCH gameMap.machine "
            + "LEFT JOIN FETCH agentMap.broker "
            + "LEFT JOIN FETCH agentMap.game "
            + "LEFT JOIN FETCH round.brokerMap AS brokerMap "
            + "LEFT JOIN FETCH brokerMap.user "
            + "WHERE NOT round.state='" + Round.getStateComplete() + "'";

    public static final String GET_GAME_BY_ID =
        "FROM Game AS game "
            + "LEFT JOIN FETCH game.round "
            + "LEFT JOIN FETCH game.machine "
            + "LEFT JOIN FETCH game.agentMap as agentMap "
            + "LEFT JOIN FETCH agentMap.broker as broker "
            + "LEFT JOIN FETCH broker.user "
            + "WHERE game.gameId =:gameId";

    public static final String GET_GAMES_NOT_COMPLETE =
        "FROM Game AS game "
            + "LEFT JOIN FETCH game.round "
            + "LEFT JOIN FETCH game.machine "
            + "LEFT JOIN FETCH game.agentMap agentMap "
            + "LEFT JOIN FETCH agentMap.broker as broker "
            + "LEFT JOIN FETCH broker.user "
            + "WHERE NOT game.state='" + Game.GameState.game_complete + "' ";

    public static final String GET_GAMES_READY =
        "FROM Game AS game "
            + "LEFT JOIN FETCH game.round "
            + "LEFT JOIN FETCH game.machine "
            + "LEFT JOIN FETCH game.agentMap agentMap "
            + "LEFT JOIN FETCH agentMap.broker as broker "
            + "WHERE game.state='" + Game.GameState.game_ready + "' ";

    public static final String GET_GAMES_COMPLETE =
        "FROM Game AS game "
            + "LEFT JOIN FETCH game.round "
            + "LEFT JOIN FETCH game.machine "
            + "LEFT JOIN FETCH game.agentMap agentMap "
            + "LEFT JOIN FETCH agentMap.broker as broker "
            + "LEFT JOIN FETCH broker.user "
            + "WHERE game.state='" + Game.GameState.game_complete + "' ";

    public static final String GET_BROKERS =
        "FROM Broker AS broker "
            + "LEFT JOIN FETCH broker.user "
            + "LEFT JOIN FETCH broker.roundMap as roundMap "
            + "LEFT JOIN FETCH broker.tournamentMap as tournamentMap "
            + "LEFT JOIN FETCH tournamentMap.levelMap as levelMap "
            + "LEFT JOIN FETCH levelMap.roundMap as roundMap ";

    public static final String GET_BROKER_BY_ID =
        "FROM Broker AS broker "
            + "LEFT JOIN FETCH broker.user "
            + "LEFT JOIN FETCH broker.roundMap "
            + "LEFT JOIN FETCH broker.agentMap as agentMap "
            + "LEFT JOIN FETCH agentMap.game "
            + "LEFT JOIN FETCH agentMap.broker as broker2 "
            + "LEFT JOIN FETCH broker2.user "
            + "WHERE broker.brokerId =:brokerId ";

    public static final String GET_BROKER_BY_NAME =
        "FROM Broker AS broker "
            + "LEFT JOIN FETCH broker.user "
            + "WHERE brokerName =:brokerName ";

    public static final String GET_BROKER_BY_BROKERAUTH =
        "FROM Broker AS broker "
            + "LEFT JOIN FETCH broker.user "
            + "LEFT JOIN FETCH broker.roundMap as roundMap "
            + "LEFT JOIN FETCH roundMap.level "
            + "LEFT JOIN FETCH broker.agentMap as agentMap "
            + "LEFT JOIN FETCH agentMap.game as game "
            + "LEFT JOIN FETCH game.round "
            + "LEFT JOIN FETCH game.machine "
            + "LEFT JOIN FETCH agentMap.broker as broker2 "
            + "LEFT JOIN FETCH broker2.user "
            + "WHERE broker.brokerAuth =:brokerAuth ";

    public static final String GET_CONFIG =
        "FROM Config AS config "
            + "WHERE config.configKey =:configKey ";
  }
}
