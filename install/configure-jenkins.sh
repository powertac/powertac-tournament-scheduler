#!/bin/bash

HOST="localhost:8080"
USER=""
TOKEN=""

AUTH=$USER:$TOKEN
URL="http://"$HOST"/jenkins/createItem?name=JOBNAME"
PAYLOAD="--data-binary @FILENAME -H Content-Type:text/xml"

install () {
    echo -e "\nConfiguring $1 Job..."
    url="${URL/JOBNAME/$1}"
    payload="${PAYLOAD/FILENAME/$2}"
    cmd="curl -s --write-out %{http_code} -XPOST $payload -u $AUTH $url"

    status=`$cmd`
    if [ "$status" != "200" ] ; then
        echo "Configuration of $1 Failed!"
    else
        echo "Configuration of $1 Success!"
    fi
}

install start-boot-server     config.boot.xml
install start-sim-server      config.sim.xml
install abort-server-instance config.abort.xml
install kill-server-instance  config.kill.xml

echo -e "\ndone"
