#!/bin/bash

HOST="localhost:8080"
USER=""
PASS=""
REST="manager/text/deploy?path=/jenkins&update=true"

if [ "$#" -eq "0" ]; then
  echo "No arguments given. Using default values."
elif [ "$#" -eq "1" ]; then
  HOST=$1
elif [ "$#" -eq "3" ]; then
  HOST=$1
  USER=$2
  PASS=$3
else
  echo "Incorrect number of arguments (need 3), using default values."
fi

echo "Attempting to upload to url: $HOST"
echo "Using tomcat Username: $USER"
echo "Using tomcat Password: $PASS"
echo "Downloading Jenkins.."
wget --quiet http://mirrors.jenkins-ci.org/war/latest/jenkins.war
echo "Installing Jenkins..."

if [ -z "$USER" ]; then
    result=`curl -s -S --upload-file ./jenkins.war "http://$HOST/$REST"`
else
    result=`curl -s -S --upload-file ./jenkins.war "http://$USER:$PASS@$HOST/$REST"`
fi

rm jenkins.war

if [ "$result" != "OK - Deployed application at context path /jenkins" ]; then
    echo -e "\nError uploading jenkins at context path /jenkins"
fi
