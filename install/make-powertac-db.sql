DROP DATABASE IF EXISTS `tourney`;
CREATE DATABASE `tourney`;

/* Create user table*/
DROP TABLE IF EXISTS `tourney`.`users`;
CREATE TABLE `tourney`.`users` (
  `userId`       BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userName`     VARCHAR(45) COLLATE latin1_bin NOT NULL,
  `institution`  VARCHAR(256)        NULL,
  `contactName`  VARCHAR(256)        NULL,
  `contactEmail` VARCHAR(256)        NULL,
  `contactPhone` VARCHAR(256)        NULL,
  `salt`         VARCHAR(45)         NOT NULL,
  `password`     VARCHAR(45)         NOT NULL,
  `permissionId` BIGINT(20) UNSIGNED NOT NULL,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `userName` (`userName`)
)
  ENGINE = InnoDB;


/* Create broker table with key constraint on userId */
DROP TABLE IF EXISTS `tourney`.`brokers`;
CREATE TABLE `tourney`.`brokers` (
  `brokerId`    BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId`      BIGINT(20) UNSIGNED NOT NULL,
  `brokerName`  VARCHAR(45) UNIQUE  NOT NULL,
  `brokerAuth`  VARCHAR(32)         NOT NULL,
  `brokerShort` VARCHAR(256)        NOT NULL,
  PRIMARY KEY (`brokerId`),
  CONSTRAINT userId_refs FOREIGN KEY (`userId`) REFERENCES `tourney`.`users` (`userId`)
)
  ENGINE = InnoDB;


/* Create poms list */
DROP TABLE IF EXISTS `tourney`.`poms`;
CREATE TABLE `tourney`.`poms` (
  `pomId`   INT(10) UNSIGNED    NOT NULL AUTO_INCREMENT,
  `pomName` VARCHAR(45)         NOT NULL,
  `userId`  BIGINT(20) UNSIGNED NOT NULL,
  PRIMARY KEY (`pomId`),
  CONSTRAINT pom_refs FOREIGN KEY (`userId`) REFERENCES `tourney`.`users` (`userId`)
)
  ENGINE = InnoDB;


/* Create top level tournament list */
DROP TABLE IF EXISTS `tourney`.`tournaments`;
CREATE TABLE `tourney`.`tournaments` (
  `tourneyId`   BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `tourneyName` VARCHAR(256) UNIQUE NOT NULL,
  `startTime`   DATETIME            NOT NULL,
  `dateFrom`    DATETIME            NOT NULL,
  `dateTo`      DATETIME            NOT NULL,
  `maxBrokers`  INTEGER             NOT NULL,
  `maxAgents`   INTEGER             NOT NULL DEFAULT 2,
  `status`      VARCHAR(32)         NOT NULL,
  `gameSize1`   INTEGER             NOT NULL,
  `gameSize2`   INTEGER             NOT NULL,
  `gameSize3`   INTEGER             NOT NULL,
  `multiplier1` INT(11)             NOT NULL,
  `multiplier2` INT(11)             NOT NULL,
  `multiplier3` INT(11)             NOT NULL,
  `type`        VARCHAR(32)         NOT NULL, /* Type is either multi-game or single game if single game ignore the gameSize params */
  `pomId`       INT(10) UNSIGNED    NOT NULL, /* This will be a foreign key to poms.pomId */
  `locations`   VARCHAR(256)        NOT NULL, /* This will be a comma delimited list for now */
  `closed`      TINYINT(1)          NOT NULL,
  PRIMARY KEY (`tourneyId`),
  CONSTRAINT tourney_refs FOREIGN KEY (`pomId`) REFERENCES `tourney`.`poms` (`pomId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `tourney`.`registrations`;
CREATE TABLE `tourney`.`registrations` (
  `registrationId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `tourneyId`      BIGINT(20) UNSIGNED NOT NULL,
  `brokerId`       BIGINT(20) UNSIGNED NOT NULL,
  CONSTRAINT tourneyId_refs FOREIGN KEY (`tourneyId`) REFERENCES `tourney`.`tournaments` (`tourneyId`),
  CONSTRAINT brokerId_refs FOREIGN KEY (`brokerId`) REFERENCES `tourney`.`brokers` (`brokerId`),
  PRIMARY KEY (`registrationId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `tourney`.`machines`;
CREATE TABLE `tourney`.`machines` (
  `machineId`     BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `machineName`   VARCHAR(30)         NOT NULL,
  `machineUrl`    VARCHAR(256)        NOT NULL, /* Hostname of the machine */
  `visualizerUrl` VARCHAR(256)        NOT NULL,
  `status`        VARCHAR(20)         NOT NULL, /* Indicates wether a game is running on this machine or not, either "running" or "idle" */
  `available`     BOOLEAN             NOT NULL,
  PRIMARY KEY (`machineId`)
)
  ENGINE = InnoDB;


/* Create top level tournament list */
DROP TABLE IF EXISTS `tourney`.`games`;
CREATE TABLE `tourney`.`games` (
  `gameId`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `gameName`        VARCHAR(256)        NOT NULL,
  `tourneyId`       BIGINT(20) UNSIGNED NOT NULL,
  `machineId`       BIGINT(20) UNSIGNED,
  `status`          VARCHAR(20)         NOT NULL,
  `startTime`       DATETIME            NOT NULL,
  `readyTime`       DATETIME            NULL,
  `visualizerQueue` VARCHAR(256)        NOT NULL, /* name of visualizer output queue */
  `serverQueue`     VARCHAR(256)        NOT NULL, /* name of server input queue */
  `location`        VARCHAR(256)        NOT NULL,
  `simStartDate`    VARCHAR(256)        NOT NULL,
  `gameLength`      INT(11) DEFAULT NULL,
  `lastTick`        INT(11) DEFAULT NULL,
  PRIMARY KEY (`gameId`),
  CONSTRAINT tourneyId2_refs FOREIGN KEY (`tourneyId`) REFERENCES `tourney`.`tournaments` (`tourneyId`),
  CONSTRAINT machineId2_refs FOREIGN KEY (`machineId`) REFERENCES `tourney`.`machines` (`machineId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `tourney`.`locations`;
CREATE TABLE `tourney`.`locations` (
  `locationId` INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
  `location`   VARCHAR(256)     NOT NULL,
  `timezone`   INTEGER          NOT NULL,
  `fromDate`   DATETIME         NOT NULL,
  `toDate`     DATETIME         NOT NULL,
  PRIMARY KEY (`locationId`)
)
  ENGINE = InnoDB;

INSERT INTO `tourney`.`locations`
(`locationId`, `location`, `timezone`, `fromDate`, `toDate`) VALUES
(1, 'rotterdam', 1, '2009-01-01 00:00:00', '2009-06-01 00:00:00');


DROP TABLE IF EXISTS `tourney`.`agents`;
CREATE TABLE `tourney`.`agents` (
  `agentId`     BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `gameId`      BIGINT(20) UNSIGNED NOT NULL,
  `brokerId`    BIGINT(20) UNSIGNED NOT NULL,
  `brokerQueue` VARCHAR(64),
  `status`      VARCHAR(32)         NOT NULL,
  `balance`     DOUBLE              NOT NULL DEFAULT '0',
  CONSTRAINT brokerId_refs2 FOREIGN KEY (`brokerId`) REFERENCES `tourney`.`brokers` (`brokerId`),
  CONSTRAINT gameId_refs2 FOREIGN KEY (`gameId`) REFERENCES `tourney`.`games` (`gameId`),
  PRIMARY KEY (`agentId`)
)
  ENGINE = InnoDB;


DROP TABLE IF EXISTS `tourney`.`config`;
CREATE TABLE IF NOT EXISTS `tourney``config` (
  `configId`    BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `configKey`   VARCHAR(256) UNIQUE NOT NULL,
  `configValue` LONGTEXT,
  PRIMARY KEY (`configId`)
)
  ENGINE = InnoDB;

