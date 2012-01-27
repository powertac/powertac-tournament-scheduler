<?xml version="1.0" encoding="UTF-8"?>
<!-- 
    Document   : Page1
    Created on : Dec 1, 2011, 12:39:57 PM
    Author     : erik
-->
<jsp:root version="2.1" xmlns:f="http://java.sun.com/jsf/core" xmlns:h="http://java.sun.com/jsf/html" xmlns:jsp="http://java.sun.com/JSP/Page" xmlns:webuijsf="http://www.sun.com/webui/webuijsf">
    <jsp:directive.page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"/>
    <f:view>
        <webuijsf:page id="page1">
            <webuijsf:html id="html1">
                <webuijsf:head id="head1">
                    <webuijsf:link id="link1" url="/resources/stylesheet.css"/>
                </webuijsf:head>
                <webuijsf:body id="body1" style="-rave-layout: grid; text-align: center;">
                    <webuijsf:form id="homeForm" style="alignment:center">
                        <webuijsf:tabSet id="tabExperiment" selected="tabAdmin" style="border-color: rgb(0, 0, 0) rgb(0, 0, 0) rgb(0, 0, 0) rgb(0, 0, 0); height: 166px; left: 0px; top: 48px; position: absolute; text-align: center; width: 574px">
                            <webuijsf:tab id="tabHome" style="text-align:center" tabIndex="0" text="Home">
                                <webuijsf:panelLayout id="layoutPanel1" style="height: 347px; position: relative; width: 100%; -rave-layout: grid">
                                    <webuijsf:staticText id="welcomeText" style="font-size: 18px; left: 24px; top: 24px; position: absolute" text="Welcome to the PowerTac Tournament Scheduler"/>
                                    <webuijsf:staticText id="staticText1" style="left: 24px; top: 96px; position: absolute" text="Login:"/>
                                    <webuijsf:staticText id="userText" style="left: 24px; top: 120px; position: absolute" text="Username:"/>
                                    <webuijsf:staticText id="passText" style="left: 24px; top: 144px; position: absolute" text="Password"/>
                                    <webuijsf:textField id="userInput" style="left: 96px; top: 120px; position: absolute"/>
                                    <webuijsf:passwordField id="passwordInput" style="left: 96px; top: 144px; position: absolute"/>
                                    <webuijsf:button id="loginSubmitButton" style="height: 24px; left: 239px; top: 144px; position: absolute; width: 72px" text="Login"/>
                                </webuijsf:panelLayout>
                            </webuijsf:tab>
                            <webuijsf:tab id="tabReg" style="text-align:center" tabIndex="2" text="Register">
                                <webuijsf:panelLayout id="layoutPanel2" style="width: 100%; height: 128px;"/>
                            </webuijsf:tab>
                            <webuijsf:tab id="tabJoin" style="text-align:center" tabIndex="2" text="Join Tournament"/>
                            <webuijsf:tab id="tabTourny" style="text-align:center" tabIndex="97" text="Start Tourny">
                                <webuijsf:panelLayout id="layoutPanel3" style="width: 100%; height: 128px;"/>
                            </webuijsf:tab>
                            <webuijsf:tab id="tabExperiment1" style="text-align:center" tabIndex="98" text="Start Experiment">
                                <webuijsf:panelLayout id="layoutPanel4" style="width: 100%; height: 128px;"/>
                            </webuijsf:tab>
                            <webuijsf:tab id="tabAdmin" style="text-align:center" tabIndex="99" text="Admin">
                                <webuijsf:panelLayout id="layoutPanel5" style="width: 100%; height: 128px;">
                                    <webuijsf:button actionExpression="#{Main.buttonJenkinsTest_action}" id="buttonJenkinsTest"
                                        style="height: 72px; left: -1px; top: 24px; position: absolute; width: 143px" text="KickBuild"/>
                                    <webuijsf:button id="buttonJenkinsAuth" style="height: 72px; left: 143px; top: 24px; position: absolute; width: 144px" text="Jenkins Authentication&#xa;"/>
                                    <webuijsf:staticText id="adminOutputText" style="position: absolute; left: 360px; top: 48px"/>
                                </webuijsf:panelLayout>
                            </webuijsf:tab>
                        </webuijsf:tabSet>
                        <webuijsf:staticText id="titleText"
                            style="font-family: 'Georgia','Times New Roman','times',serif; font-size: 36px; height: 47px; left: 0px; top: 0px; position: absolute; width: 574px; z-index: 500; text-align:center"
                            styleClass="AccdHeader_sun4" text="PowerTac Tournament Scheduler"/>
                    </webuijsf:form>
                </webuijsf:body>
            </webuijsf:html>
        </webuijsf:page>
    </f:view>
</jsp:root>
