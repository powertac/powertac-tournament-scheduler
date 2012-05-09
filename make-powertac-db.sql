
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
	`tourneyName` VARCHAR(256) NOT NULL,	
	`startTime` DATETIME NOT NULL,
	`maxBrokers` integer UNSIGNED NOT NULL,
	`status` VARCHAR(32) NOT NULL,
	`type` VARCHAR(32) NOT NULL, /* Type is either multi-game or single game */
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
	`status` VARCHAR(20) NOT NULL, /* Indicates wether a game is running on this machine or not, either "running" or "idle" */
	PRIMARY KEY (`machineId`)
) ENGINE=InnoDB;

/* Local machines for testing purposes should be removed in prod*/
INSERT INTO `tourney`.`machines` (machineName, machineUrl, status) VALUES ('tac04', 'tac04.cs.umn.edu', 'idle');
INSERT INTO `tourney`.`machines` (machineName, machineUrl, status) VALUES ('tac10', 'tac10.cs.umn.edu', 'idle');
INSERT INTO `tourney`.`machines` (machineName, machineUrl, status) VALUES ('tac11', 'tac11.cs.umn.edu', 'idle');
INSERT INTO `tourney`.`machines` (machineName, machineUrl, status) VALUES ('tac12', 'tac12.cs.umn.edu', 'idle');
INSERT INTO `tourney`.`machines` (machineName, machineUrl, status) VALUES ('tac13', 'tac13.cs.umn.edu', 'idle');




/* Create properties list*/
DROP TABLE IF EXISTS `tourney`.`properties`;
CREATE TABLE `tourney`.`properties` (
	`propId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`gameId` BIGINT(20) UNSIGNED NOT NULL, /* Not a foreign key to prevent cycles*/
	`tourneyId` BIGINT(20) UNSIGNED NOT NULL, /* Not a foreign key to prevent cycles*/
	`location` VARCHAR(256) NOT NULL,
	`startTime` VARCHAR(256) NOT NULL,
	PRIMARY KEY (`propId`)
) ENGINE=InnoDB;

/* Create top level tournament list */
DROP TABLE IF EXISTS `tourney`.`games`;
CREATE TABLE `tourney`.`games` (
	`gameId` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`gameName` VARCHAR(256) NOT NULL,
	`tourneyId` BIGINT(20) UNSIGNED NOT NULL,
	`machineId` BIGINT(20) UNSIGNED NOT NULL,
	`propId` BIGINT(20) UNSIGNED NOT NULL,
	`logId` integer UNSIGNED NOT NULL,
	`status` VARCHAR(20) NOT NULL,
	`hasBootstrap` BOOLEAN NOT NULL,
	`startTime` DATETIME NOT NULL,
	`visualizerUrl` VARCHAR(256) NOT NULL,
	`propertiesUrl` VARCHAR(256) NOT NULL, /* This will be the url to the properties file */
	`bootstrapUrl` VARCHAR(256) NOT NULL, /* This will be the url to the bootstrap file*/
	`location` VARCHAR(256) NOT NULL, /* This will be a comma delimited list for now */
	PRIMARY KEY (`gameId`),	
	CONSTRAINT prop_game_refs FOREIGN KEY (`propId`) REFERENCES `tourney`.`properties` (`propId`),
	CONSTRAINT tourneyId2_refs FOREIGN KEY (`tourneyId`) REFERENCES `tourney`.`tournaments` ( `tourneyId` ),
	CONSTRAINT machineId_refs FOREIGN KEY (`machineId`) REFERENCES `tourney`.`machines` ( `machineId` ),
	CONSTRAINT logId_refs FOREIGN KEY (`logId`) REFERENCES `tourney`.`logs` ( `logId` )
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




