
DROP DATABASE IF EXISTS `tourney`;
CREATE DATABASE `tourney`;

/* Create user table*/
DROP TABLE IF EXISTS `tourney`.`users`;
CREATE TABLE `tourney`.`users` (
	`userId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`userName` VARCHAR(45) UNIQUE NOT NULL,
	`salt` VARCHAR(45) NOT NULL,
	`password` VARCHAR(45) NOT NULL, 
	`permissionId` BIGINT(20) UNSIGNED NOT NULL,
	PRIMARY KEY (`userId`)
) ENGINE=InnoDB;

/* Admin Erik user insert for testing should be removed in prod*/
INSERT INTO `tourney`.`users` (userName, salt, password, permissionId) VALUES ('erik', '1234567testsalt', md5('test1234567testsalt'), 0);

/* Create broker table with key constraint on userId */
DROP TABLE IF EXISTS `tourney`.`brokers`;
CREATE TABLE `tourney`.`brokers` (
	`brokerId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`userId` BIGINT(20) UNSIGNED NOT NULL,
	`brokerName` VARCHAR(45) NOT NULL,
	`brokerAuth` VARCHAR(32) NOT NULL,
	`brokerShort` VARCHAR(200) NOT NULL,
	`numberInGame` INT NOT NULL,
	PRIMARY KEY (`brokerId`),
	CONSTRAINT userId_refs FOREIGN KEY (`userId`) REFERENCES `tourney`.`users` (`userId`)
) ENGINE=InnoDB;


/* Create top level tournament list */
DROP TABLE IF EXISTS `tourney`.`tournaments`;
CREATE TABLE `tourney`.`tournaments` (
	`tourneyId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`tourneyName` VARCHAR(256) UNIQUE NOT NULL,	
	`openRegistration` BOOLEAN NOT NULL,
	`maxGames` int NOT NULL,
	`startTime` DATETIME NOT NULL,
	`maxBrokers` integer UNSIGNED NOT NULL,
	`status` VARCHAR(32) NOT NULL,
	`gameSize1` integer NOT NULL,
	`numberGameSize1` integer NOT NULL,
	`gameSize2` integer NOT NULL,
	`numberGameSize2` integer NOT NULL,
	`gameSize3` integer NOT NULL,
	`numberGameSize3` integer NOT NULL,
	`maxBrokerInstances` integer NOT NULL DEFAULT 2,
	`type` VARCHAR(32) NOT NULL, /* Type is either multi-game or single game if single game ignore the gameSize params */
	`pomUrl` VARCHAR(256) NOT NULL, /* This will be the url to the pom file */
	`locations` VARCHAR(256) NOT NULL, /* This will be a comma delimited list for now */
	PRIMARY KEY (`tourneyId`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `tourney`.`registration`;
CREATE TABLE `tourney`.`registration` (
	`tourneyId` BIGINT(20) UNSIGNED NOT NULL,
	`brokerId` BIGINT(20) UNSIGNED NOT NULL,
	CONSTRAINT tourneyId_refs FOREIGN KEY (`tourneyId`) REFERENCES `tourney`.`tournaments` ( `tourneyId` ),
	CONSTRAINT brokerId_refs FOREIGN KEY (`brokerId`) REFERENCES `tourney`.`brokers` ( `brokerId` )
) ENGINE=InnoDB;



DROP TABLE IF EXISTS `tourney`.`permission`;
CREATE TABLE `tourney`.`logs` (
	`logId` integer UNSIGNED NOT NULL AUTO_INCREMENT,
	`status` VARCHAR(20) NOT NULL,
	`logUrl` VARCHAR(256) NOT NULL, /* when status is packaged, this points to the permanent url of logs */
	PRIMARY KEY (`logId`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `tourney`.`machines`;
CREATE TABLE `tourney`.`machines` (
	`machineId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`machineName` VARCHAR(30) NOT NULL,
	`machineUrl` VARCHAR(256) NOT NULL, /* Url to the machine */
	`visualizerUrl` VARCHAR(256) NOT NULL,
	`visualizerQueue` VARCHAR(256) NOT NULL,
	`status` VARCHAR(20) NOT NULL, /* Indicates wether a game is running on this machine or not, either "running" or "idle" */
	`available` BOOLEAN NOT NULL,
	PRIMARY KEY (`machineId`)
) ENGINE=InnoDB;

/* Local machines for testing purposes should be removed in prod*/
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac09', 'tac09.cs.umn.edu','tac02.cs.umn.edu:8080/viz1','vizQueue1', 'idle', true);
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac10', 'tac10.cs.umn.edu','tac02.cs.umn.edu:8080/viz2','vizQueue2', 'idle', true);
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac11', 'tac11.cs.umn.edu','tac02.cs.umn.edu:8080/viz3','vizQueue3', 'idle', true);
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac12', 'tac12.cs.umn.edu','tac02.cs.umn.edu:8080/viz4','vizQueue4', 'idle', true);
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac13', 'tac13.cs.umn.edu','tac02.cs.umn.edu:8080/viz5','vizQueue5', 'idle', true);
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac14', 'tac14.cs.umn.edu','tac02.cs.umn.edu:8080/viz6','vizQueue6', 'idle', true);
INSERT INTO `tourney`.`machines` (machineName, machineUrl, visualizerUrl, visualizerQueue, status, available) VALUES ('tac15', 'tac15.cs.umn.edu','tac02.cs.umn.edu:8080/viz7','vizQueue7', 'idle', true);





/* Create properties list*/
DROP TABLE IF EXISTS `tourney`.`properties`;
CREATE TABLE `tourney`.`properties` (
	`propId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`gameId` BIGINT(20) UNSIGNED NOT NULL, /* Not a foreign key to prevent cycles*/
	`location` VARCHAR(256) NOT NULL,
	`startTime` VARCHAR(256) NOT NULL,
	`jmsUrl` VARCHAR(256) NOT NULL,
	`vizQueue` VARCHAR(256) NOT NULL,
	PRIMARY KEY (`propId`)
) ENGINE=InnoDB;

/* Create top level tournament list */
DROP TABLE IF EXISTS `tourney`.`games`;
CREATE TABLE `tourney`.`games` (
	`gameId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`gameName` VARCHAR(256) NOT NULL,
	`tourneyId` BIGINT(20) UNSIGNED NOT NULL,
	`machineId` BIGINT(20) UNSIGNED,
	`status` VARCHAR(20) NOT NULL,
	`maxBrokers` integer NOT NULL,
	`hasBootstrap` BOOLEAN NOT NULL,
	`brokers` VARCHAR(256) NOT NULL,
	`startTime` DATETIME NOT NULL,
	`jmsUrl` VARCHAR(256) NOT NULL,
	`visualizerUrl` VARCHAR(256) NOT NULL,
	`propertiesUrl` VARCHAR(256) NOT NULL, /* This will be the url to the properties file */
	`bootstrapUrl` VARCHAR(256) NOT NULL, /* This will be the url to the bootstrap file*/
	`location` VARCHAR(256) NOT NULL, /* This will be a comma delimited list for now */
	PRIMARY KEY (`gameId`),	
	CONSTRAINT tourneyId2_refs FOREIGN KEY (`tourneyId`) REFERENCES `tourney`.`tournaments` ( `tourneyId` ),
	CONSTRAINT machineId_refs FOREIGN KEY (`machineId`) REFERENCES `tourney`.`machines` ( `machineId` )
) ENGINE=InnoDB;



DROP TABLE IF EXISTS `tourney`.`permissions`;
CREATE TABLE `tourney`.`permissions` (
	`permissionId` integer UNSIGNED NOT NULL AUTO_INCREMENT,
	`permissionName` VARCHAR(20) NOT NULL,
	PRIMARY KEY (`permissionId`)
) ENGINE=InnoDB;


DROP TABLE IF EXISTS `tourney`.`poms`;
CREATE TABLE `tourney`.`poms` (
	`pomId` integer UNSIGNED NOT NULL AUTO_INCREMENT,
	`location` VARCHAR(256) NOT NULL,
	`name` VARCHAR(45) NOT NULL,
	`uploadingUser` VARCHAR(45) NOT NULL,
	PRIMARY KEY (`pomId`)
) ENGINE=InnoDB;


DROP TABLE IF EXISTS `tourney`.`locations`;
CREATE TABLE `tourney`.`locations` (
	`locationId` integer UNSIGNED NOT NULL AUTO_INCREMENT,
	`location` VARCHAR(256) NOT NULL,
	`fromDate` DATETIME NOT NULL,
	`toDate` DATETIME NOT NULL,
	PRIMARY KEY (`locationId`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `tourney`.`ingame`;
CREATE TABLE `tourney`.`ingame` (
	`ingameId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`gameId` BIGINT(20) UNSIGNED NOT NULL,
	`brokerId` BIGINT(20) UNSIGNED NOT NULL,
	`brokerAuth` VARCHAR(256) NOT NULL,
	`brokerName` VARCHAR(256) NOT NULL,
	CONSTRAINT brokerId_refs2 FOREIGN KEY (`brokerId`) REFERENCES `tourney`.`brokers` (`brokerId`),
	CONSTRAINT gameId_refs2 FOREIGN KEY (`gameId`) REFERENCES `tourney`.`games` ( `gameId` ),
	PRIMARY KEY (`ingameId`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `tourney`.`competitions`;
CREATE TABLE `tourney`.`competitions` (
	`competitionId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`qualifierRoundId` BIGINT(20) UNSIGNED NOT NULL,
	`finalRoundId` BIGINT(20) UNSIGNED NOT NULL,
	`status` VARCHAR(256) NOT NULL,
	CONSTRAINT qualifier_refs FOREIGN KEY (`qualifierRoundId`) REFERENCES `tourney`.`tournaments`(`tourneyId`),
	CONSTRAINT final_refs FOREIGN KEY (`finalRoundId`) REFERENCES `tourney`.`tournaments`(`tourneyId`),
	PRIMARY KEY (`competitionId`)
) ENGINE=InnoDB;


/*** SCHEDULING TABLES ****/

                                             
CREATE TABLE `tourney`.`AgentAdmin` (
  `InternalAgentID` int(18) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Internal Game ID',
  `AgentType` int(18) unsigned NOT NULL,
  `AgentCopy` int(5) unsigned NOT NULL,
  `AgentName` varchar(250) DEFAULT NULL,
  `AgentDescription` text,
  PRIMARY KEY (`InternalAgentID`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=latin1;

CREATE TABLE `tourney`.`AgentQueue` (
  `InternalAgentID` int(18) unsigned NOT NULL,
  `AgentType` int(10) DEFAULT NULL,
  `Prev_Age` int(18) unsigned NOT NULL,
  `Age` int(18) unsigned NOT NULL DEFAULT '0',
  `IsPlaying` tinyint(1) DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `tourney`.`GameArchive` (
  `InternalGameID` int(18) unsigned NOT NULL AUTO_INCREMENT,
  `GameType` int(5) unsigned NOT NULL,
  `TimePlayed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `TimeCompleted` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ServerNumber` int(11) unsigned NOT NULL,
  `IsPlaying` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY (`InternalGameID`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=latin1;

CREATE TABLE `tourney`.`GameLog` (
  `InternalAgentID` int(18) unsigned NOT NULL,
  `InternalGameID` int(18) unsigned NOT NULL,
  PRIMARY KEY (`InternalAgentID`,`InternalGameID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `tourney`.`GameServers` (
  `ServerID` int(11) NOT NULL,
  `ServerName` varchar(175) NOT NULL,
  `ServerNumber` int(18) unsigned NOT NULL AUTO_INCREMENT,
  `IsPlaying` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ServerNumber`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;


