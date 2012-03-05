<?xml version="1.0" encoding="UTF-8" ?>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">




<html>

<title>PowerTac Tournament Scheduler 2012</title>


<head>

</head>
<f:view>
	<body>
		<div id="container"
			style="margin-left: auto; margin-right: auto; width: 700px">


			<div id="header"
				style="background-color: e4ffe3; border: 3px solid 009E18; border-radius: 15px; height: 120; width: 700">
				<img id="logo" src="garland_logo.png" style="float: left" />
				<div id="logo_text"
					style="position: relative; bottom: -60px; left: -130px; white-space: nowrap;">
					<h1>Tournament Scheduler</h1>
				</div>
			</div>


			<div id="nav_ribbon"
				style="border-radius: 15px; width: 700; vertical-align: middle">
				<h:form>
					<h:commandButton value="Test" action="Test"
						rendered="#{user.permissions <= 2}" />
					<h:commandButton value="Other1" image="image-btn.png"
						action="login" />
					<h:commandButton value="Other2" image="image-btn.png"
						action="login" />
				</h:form>
			</div>

			<div id="content"
				style="background-image: url(bg-image.png); background-repeat: repeat-x; border-radius: 15px; padding-left: 20px; padding-top: 50px;">


				<h:form rendered="#{!user.loggedIn}">

					<h:outputText id="title" style="font:24pt; font-weight:bold;"
						value="Login:" />
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
					<h:commandLink value="#{user.username}" action="Account" />
					<br />
					<h:commandButton value="Logout" action="#{actionLogin.logout}" />
				</h:form>

				<!-- End Content Div -->
			</div>
		</div>
		<!-- End Container Div -->
	</body>

</f:view>
</html>