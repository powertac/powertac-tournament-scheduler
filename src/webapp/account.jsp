<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
	xmlns:f="http://java.sun.com/jsf/core"
	xmlns:h="http://java.sun.com/jsf/html">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>PowerTac 2012</title>
<h2>Account Page</h2>

</head>
<body>
	<f:view>
		<h:form>

			<h:outputText value="List of Brokers:" />
			<h:panelGrid columns="1">
				<h:selectOneListbox value="Your Brokers" size="15">
					<f:selectItem itemValue="No Brokers"
						itemLabel="No Brokers Available" />
				</h:selectOneListbox>
				<h:panelGroup>
					<h:commandButton value="Add Broker" action="Broker" />
					<h:commandButton value="Delete Broker" action="DeleteBroker" />
				</h:panelGroup>
			</h:panelGrid>
			<br>
			<h:outputText value="Brokers Registered for Tournaments" />
			<table border="1">
				<tr>
					<td><b>Broker</b></td><td><b>Tournament</b></td>
				</tr>
				
				<tr><td>None</td><td>None</td>
			</table>
			<h:commandButton value="Register Broker..." />

		</h:form>
	</f:view>
</body>
</html>