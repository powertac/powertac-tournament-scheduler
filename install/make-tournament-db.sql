DROP DATABASE IF EXISTS `powertac_tournament`;
CREATE DATABASE `powertac_tournament`;

/* Create user table*/
DROP TABLE IF EXISTS `powertac_tournament`.`users`;
CREATE TABLE `powertac_tournament`.`users` (
  `userId`       INT(11)                                                     NOT NULL AUTO_INCREMENT,
  `userName`     VARCHAR(45) UNIQUE                                          NOT NULL,
  `institution`  VARCHAR(256)                                                NULL,
  `contactName`  VARCHAR(256)                                                NULL,
  `contactEmail` VARCHAR(256)                                                NULL,
  `contactPhone` VARCHAR(256)                                                NULL,
  `salt`         VARCHAR(45)                                                 NOT NULL,
  `password`     VARCHAR(45)                                                 NOT NULL,
  `permission`   ENUM('admin', 'researcher', 'organizer', 'broker', 'guest') NOT NULL,
  PRIMARY KEY (`userId`)
)
  ENGINE = InnoDB;


/* Create broker table with key constraint on userId */
DROP TABLE IF EXISTS `powertac_tournament`.`brokers`;
CREATE TABLE `powertac_tournament`.`brokers` (
  `brokerId`    INT(11)             NOT NULL AUTO_INCREMENT,
  `userId`      INT(11)             NOT NULL,
  `brokerName`  VARCHAR(45) UNIQUE  NOT NULL,
  `brokerAuth`  VARCHAR(32) UNIQUE  NOT NULL,
  `brokerShort` VARCHAR(256)        NOT NULL,
  PRIMARY KEY (`brokerId`),
  CONSTRAINT broker_refs FOREIGN KEY (`userId`) REFERENCES `powertac_tournament`.`users` (`userId`)
)
  ENGINE = InnoDB;


/* Create poms list */
DROP TABLE IF EXISTS `powertac_tournament`.`poms`;
CREATE TABLE `powertac_tournament`.`poms` (
  `pomId`   INT(11)     NOT NULL AUTO_INCREMENT,
  `pomName` VARCHAR(45) NOT NULL,
  `userId`  INT(11)     NOT NULL,
  PRIMARY KEY (`pomId`),
  CONSTRAINT pom_refs FOREIGN KEY (`userId`) REFERENCES `powertac_tournament`.`users` (`userId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`tournaments`;
CREATE TABLE `powertac_tournament`.`tournaments` (
  `tournamentId`   INT(11)                                                                                                                                            NOT NULL AUTO_INCREMENT,
  `tournamentName` VARCHAR(256) UNIQUE                                                                                                                                NOT NULL,
  `state`          ENUM('open', 'closed', 'scheduled0', 'completed0', 'scheduled1', 'completed1', 'scheduled2', 'completed2', 'scheduled3', 'completed3', 'complete') NOT NULL,
  `pomId`          INT(11)                                                                                                                                            NOT NULL,
  `maxAgents`      INT(11)                                                                                                                                            NOT NULL DEFAULT 2,
  PRIMARY KEY (`tournamentId`),
  CONSTRAINT tournament_refs FOREIGN KEY (`pomId`) REFERENCES `powertac_tournament`.`poms` (`pomId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`levels`;
CREATE TABLE `powertac_tournament`.`levels` (
  `levelId`      INT(11)      NOT NULL AUTO_INCREMENT,
  `levelName`    VARCHAR(256) NOT NULL,
  `tournamentId` INT(11)      NOT NULL,
  `levelNr`      INT(11)      NOT NULL,
  `nofRounds`    INT(11)      NOT NULL,
  `nofWinners`   INT(11)      NOT NULL,
  `startTime`    DATETIME     NOT NULL,
  PRIMARY KEY (`levelId`),
  CONSTRAINT level_refs FOREIGN KEY (`tournamentId`) REFERENCES `powertac_tournament`.`tournaments` (`tournamentId`)
)
  ENGINE = InnoDB;


/* Create top level round list */
DROP TABLE IF EXISTS `powertac_tournament`.`rounds`;
CREATE TABLE `powertac_tournament`.`rounds` (
  `roundId`     INT(11)                                    NOT NULL AUTO_INCREMENT,
  `roundName`   VARCHAR(256) UNIQUE                        NOT NULL,
  `levelId`     INT(11) DEFAULT NULL,
  `startTime`   DATETIME                                   NOT NULL,
  `dateFrom`    DATETIME                                   NOT NULL,
  `dateTo`      DATETIME                                   NOT NULL,
  `maxBrokers`  INT(11)                                    NOT NULL,
  `maxAgents`   INT(11)                                    NOT NULL DEFAULT 2,
  `gameSize1`   INT(11)                                    NOT NULL,
  `gameSize2`   INT(11)                                    NOT NULL,
  `gameSize3`   INT(11)                                    NOT NULL,
  `multiplier1` INT(11)                                    NOT NULL,
  `multiplier2` INT(11)                                    NOT NULL,
  `multiplier3` INT(11)                                    NOT NULL,
  `pomId`       INT(11)                                    NOT NULL,
  `locations`   VARCHAR(256)                               NOT NULL,
  `state`       ENUM('pending', 'in_progress', 'complete') NOT NULL,
  PRIMARY KEY (`roundId`),
  CONSTRAINT round_refs1 FOREIGN KEY (`pomId`) REFERENCES `powertac_tournament`.`poms` (`pomId`),
  CONSTRAINT round_refs2 FOREIGN KEY (`levelId`) REFERENCES `powertac_tournament`.`levels` (`levelId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`round_brokers`;
CREATE TABLE `powertac_tournament`.`round_brokers` (
  `roundBrokerId` INT(11) NOT NULL AUTO_INCREMENT,
  `roundId`       INT(11) NOT NULL,
  `brokerId`      INT(11) NOT NULL,
  PRIMARY KEY (`roundBrokerId`),
  CONSTRAINT round_broker_refs1 FOREIGN KEY (`roundId`) REFERENCES `powertac_tournament`.`rounds` (`roundId`),
  CONSTRAINT round_broker_refs2 FOREIGN KEY (`brokerId`) REFERENCES `powertac_tournament`.`brokers` (`brokerId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`tournament_brokers`;
CREATE TABLE `powertac_tournament`.`tournament_brokers` (
  `tournamentBrokerId` INT(11) NOT NULL AUTO_INCREMENT,
  `tournamentId`       INT(11) NOT NULL,
  `brokerId`           INT(11) NOT NULL,
  PRIMARY KEY (tournamentBrokerId),
  CONSTRAINT tournament_broker_refs1 FOREIGN KEY (`tournamentId`) REFERENCES `powertac_tournament`.`tournaments` (`tournamentId`),
  CONSTRAINT tournament_broker_refs2 FOREIGN KEY (`brokerId`) REFERENCES `powertac_tournament`.`brokers` (`brokerId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`machines`;
CREATE TABLE `powertac_tournament`.`machines` (
  `machineId`     INT(11)                 NOT NULL AUTO_INCREMENT,
  `machineName`   VARCHAR(32)             NOT NULL,
  `machineUrl`    VARCHAR(256)            NOT NULL, /* Hostname of the machine */
  `visualizerUrl` VARCHAR(256)            NOT NULL,
  `state`         ENUM('idle', 'running') NOT NULL, /* Indicates wether a game is running on this machine or not, either "running" or "idle" */
  `available`     BOOLEAN                 NOT NULL,
  PRIMARY KEY (`machineId`)
)
  ENGINE = InnoDB;


/* Create top level games list */
DROP TABLE IF EXISTS `powertac_tournament`.`games`;
CREATE TABLE `powertac_tournament`.`games` (
  `gameId`          INT(11)                                                                                                                                                    NOT NULL AUTO_INCREMENT,
  `gameName`        VARCHAR(256)                                                                                                                                               NOT NULL,
  `roundId`         INT(11)                                                                                                                                                    NOT NULL,
  `machineId`       INT(11)                                                                                                                                                    NULL,
  `state`           ENUM('boot_pending', 'boot_in_progress', 'boot_complete', 'boot_failed', 'game_pending', 'game_ready', 'game_in_progress', 'game_complete', 'game_failed') NOT NULL,
  `startTime`       DATETIME                                                                                                                                                   NOT NULL,
  `readyTime`       DATETIME                                                                                                                                                   NULL,
  `visualizerQueue` VARCHAR(256)                                                                                                                                               NOT NULL, /* name of visualizer output queue */
  `serverQueue`     VARCHAR(256)                                                                                                                                               NOT NULL, /* name of server input queue */
  `location`        VARCHAR(256)                                                                                                                                               NOT NULL,
  `simStartDate`    VARCHAR(256)                                                                                                                                               NOT NULL,
  `gameLength`      INT(11) DEFAULT NULL,
  `lastTick`        INT(11) DEFAULT NULL,
  PRIMARY KEY (`gameId`),
  CONSTRAINT game_refs1 FOREIGN KEY (`roundId`) REFERENCES `powertac_tournament`.`rounds` (`roundId`),
  CONSTRAINT game_refs2 FOREIGN KEY (`machineId`) REFERENCES `powertac_tournament`.`machines` (`machineId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`locations`;
CREATE TABLE `powertac_tournament`.`locations` (
  `locationId` INT(11)      NOT NULL AUTO_INCREMENT,
  `location`   VARCHAR(256) NOT NULL,
  `timezone`   INT(11)      NOT NULL,
  `fromDate`   DATETIME     NOT NULL,
  `toDate`     DATETIME     NOT NULL,
  PRIMARY KEY (`locationId`)
)
  ENGINE = InnoDB;

INSERT INTO `powertac_tournament`.`locations`
(`locationId`, `location`, `timezone`, `fromDate`, `toDate`) VALUES
  (1, 'rotterdam', 1, '2009-01-01 00:00:00', '2011-06-01 00:00:00');


DROP TABLE IF EXISTS `powertac_tournament`.`agents`;
CREATE TABLE `powertac_tournament`.`agents` (
  `agentId`     INT(11)                                    NOT NULL AUTO_INCREMENT,
  `gameId`      INT(11)                                    NOT NULL,
  `brokerId`    INT(11)                                    NOT NULL,
  `brokerQueue` VARCHAR(64)                                NULL,
  `state`       ENUM('pending', 'in_progress', 'complete') NOT NULL,
  `balance`     DOUBLE                                     NOT NULL DEFAULT '0',
  PRIMARY KEY (`agentId`),
  CONSTRAINT agent_refs1 FOREIGN KEY (`brokerId`) REFERENCES `powertac_tournament`.`brokers` (`brokerId`),
  CONSTRAINT agent_refs2 FOREIGN KEY (`gameId`) REFERENCES `powertac_tournament`.`games` (`gameId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `powertac_tournament`.`config`;
CREATE TABLE IF NOT EXISTS `powertac_tournament`.`config` (
  `configId`    INT(11)             NOT NULL AUTO_INCREMENT,
  `configKey`   VARCHAR(256) UNIQUE NOT NULL,
  `configValue` LONGTEXT,
  PRIMARY KEY (`configId`)
)
  ENGINE = InnoDB;
