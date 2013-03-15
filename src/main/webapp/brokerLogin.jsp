<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<?xml version='1.0' encoding='UTF-8' ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" >

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Powertac Tournament Scheduler</title>
</head>

<body>
    <h2>Broker Login Page</h2>
    <p>
        To participate in tournaments, brokers will communicate with the
        Tournament Scheduler via a RESTful api. This page contains the
        specification for the api.
    </p>

    <b>REST Specification</b>
    <p>
        To make REST calls on the Tournament Scheduler, you will use the
        brokerLogin.jsp page, as the example below: <br>
    </p>
    <blockquote>
        <i>http://url.to.tournament.scheduler:8080/TournamentScheduler/faces/brokerLogin.jsp?{params}</i>
    </blockquote>

    <p>
        <b>Required Parameters:</b>
    </p>
    <ul>
        <li>authToken=<i>{your-broker's-authorization-token}</i>
            - This allows your broker to authenticate with the tournament
            scheduler and verify its identity. Keep this secret.
        </li>
        <li>requestJoin=<i>{name-of-tournament-you-wish-to-join}</i>
            - When you sign up for a tournament you will be given an official
            name to place here. The tournament scheduler will keep track of a
            broker's games automatically.
        </li>
    </ul>

    <p>
        <b>Optional Parameters:</b>
    </p>
    <ul>
        <li>type=<i>{json(default)|xml}</i>
            - Specify the format you wish the response from the tournament
            scheduler for this REST call.
        </li>
    </ul>
</body>
</html>