# Power TAC Tournament Scheduler

## Introduction

The Power TAC Tournament Schedudler is a dynamic jsf web application for scheduling and managing tournaments and experiments between brokers. For more information, see http://www.powertac.org.

## Getting Started 

* Download a copy of apache tomcat 7 (http://tomcat.apache.org/download-70.cgi).

* Edit the tomcat-users.xml file and add the following markup in the tomcat-users tag:

`<role rolename="manager-gui"/>`

`<role rolename="admin-gui"/>`

`<role rolename="manager-script"/>`

`<role rolename="admin-script"/>`

`<user username="admin" password="admin" roles="admin-gui,manager-gui,admin-script,manager-script"/>`

* Run the ./startup.sh script for apache tomcat

* Navigate to the tournament scheduler project and run `mvn compile tomcat7:deploy`


