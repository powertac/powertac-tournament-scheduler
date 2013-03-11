#!/bin/bash

HOST="localhost:8080"

if [ "$#" -eq "0" ]; then
  echo "No arguments given. Using default values."
elif [ "$#" -eq "1" ]; then
  HOST=$1
fi

echo "Configuring Jenkins Boot Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.boot.xml -H Content-Type:text/xml http://$HOST/jenkins/createItem?name=start-boot-server&mode=create`
if [ "$status" != "200" ] ; then
    echo -e "\nConfiguration of start-boot-server Failed!" $status
else
    echo -e "\nConfiguration of start-boot-server Success!" $status
fi

echo
echo -e "\n\nConfiguring Jenkins Start Job...\n"
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.sim.xml -H Content-Type:text/xml http://$HOST/jenkins/createItem?name=start-sim-server&mode=create`
if [ "$status" != "200" ] ; then
    echo -e "\nConfiguration of start-sim-server Failed!" $status
else
    echo -e "\nConfiguration of start-sim-server Success!" $status
fi

echo -e "\n\nConfiguring Jenkins Abort Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.abort.xml -H Content-Type:text/xml http://$HOST/jenkins/createItem?name=abort-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo -e "\nConfiguring abort-server-instance Failed!" $status
    exit 1
else
    echo -e "\nConfiguring abort-server-instance Success!" $status
fi

echo -e "\n\nConfiguring Jenkins Kill Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.kill.xml -H Content-Type:text/xml http://$HOST/jenkins/createItem?name=kill-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo -e "\nConfiguring kill-server-instance Failed!" $status
    exit 1
else
    echo -e "\nConfiguring kill-server-instance Success!" $status
fi

echo "done."
