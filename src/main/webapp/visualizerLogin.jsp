<?xml version='1.0' encoding='UTF-8' ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
	xmlns:f="http://java.sun.com/jsf/core"
	xmlns:h="http://java.sun.com/jsf/html">


<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />


<title>Powertac 2012</title>
<h2>Visualizer Login Page</h2>
</head>
<body>
	<f:view>
		<p>To participate in tournaments, visualizers will communicate with
			the Tournament Scheduler via a RESTful api. This page contains the
			specification for the api.</p>
		<br>
		<b>REST Specification</b>
		<p>
			To make REST calls on the Tournament Scheduler, you will use the
			visualizerLogin.jsp page, as the example below: <br>
		<blockquote>
			<i>http://url.to.tournament.scheduler:8080/TournamentScheduler/faces/visualizerLogin.jsp?=asdf&requestJoin=2012Compitition&type=json</i>
		</blockquote>

		<br> Required Parameters: 
		<br>
		<ul>
			<li><b>machineName: name of machine this viz is fronting for</li>
		</ul>				
		</p>
	</f:view>
</body>
</html>