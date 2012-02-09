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
<title>Powertac 2012</title>
<h2>Register New Account</h2>
</head>
<body>
	<f:view>
		<h:form>
			
			<h:panelGrid columns="2">

				<h:outputText value="Username of Group:" />
				<h:panelGroup>
					<h:inputText value="#{actionRegister.username}" />
				</h:panelGroup>


				<h:outputText value="Institution/Research Group:" />
				<h:panelGroup>
					<h:inputText value="#{actionRegister.group}" />
				</h:panelGroup>
				
				<h:outputText value="Point of Contact(name):" />
				<h:panelGroup>
					<h:inputText value="#{actionRegister.name}" />
				</h:panelGroup>
				
				<h:outputText value="Point of Contact(email):" />
				<h:panelGroup>
					<h:inputText value="#{actionRegister.email}" />
				</h:panelGroup>
				
				<h:outputText value="Point of Contact(phone):" />
				<h:panelGroup>
					<h:inputText value="#{actionRegister.phone}" />
				</h:panelGroup>
				
				<h:outputText value="Password:" />
				<h:panelGroup>
					<h:inputSecret value="#{actionRegister.password}" />
				</h:panelGroup>
				
				<h:outputText value="Confirm Password:" />
				<h:panelGroup>
					<h:inputSecret value="#{actionRegister.password}" />
				</h:panelGroup>
				
				<h:panelGroup>
					<h:commandButton value="Submit Registration" action="#{actionRegister.register}" />
				</h:panelGroup>
			</h:panelGrid>

		</h:form>

	</f:view>
</body>
</html>