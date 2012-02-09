<?xml version="1.0" encoding="UTF-8" ?>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>


<html>

<title>PowerTac Tournament Scheduler 2012</title>


<head>

<div id="nav_bar" align="center">
	<div id="logo_img" align="left">
		<img id="logo" src="garland_logo.png"></img>
		<h1>Tournament Scheduler</h1>
	</div>
	<div id="logo_text" align="center"></div>
</div>

</head>
<f:view>
	<body>
		<h:form rendered="#{!user.loggedIn}">

			<h4>Please enter your username and password.</h4>
			<h:panelGrid columns="2">

				<h:outputText value="Username:" />
				<h:panelGroup>
					<h:inputText value="#{actionLogin.username}" />
				</h:panelGroup>


				<h:outputText value="Password:" />
				<h:panelGroup>
					<h:inputSecret value="#{actionLogin.password}" />
				</h:panelGroup>

				<h:commandLink value="Register" action="Register" />
				<h:panelGroup>
					<h:commandButton value="Login" action="#{actionLogin.login}" />
				</h:panelGroup>
			</h:panelGrid>

		</h:form>

		<h:form rendered="#{user.loggedIn}">
			<h:outputText value="Logged In As: " rendered="#{user.loggedIn}" />
			<h:commandLink value="#{user.username}" action="Account" /><br/>
			<h:commandButton value="Logout" action="#{actionLogin.logout}" />
		</h:form>
	</body>

</f:view>
</html>