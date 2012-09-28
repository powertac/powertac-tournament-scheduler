if [ -z "$1" ]; then
    echo "You need to specify the Jenkins URl"
    echo "Usually this is localhost:8080"
    echo
    exit 1
fi

echo "Configuring Jenkins Boot Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.boot.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=start-boot-server&mode=create`
if [ "$status" != "200" ] ; then
    echo -e "\nConfiguration of start-boot-server Failed!" $status
else
    echo -e "\nConfiguration of start-boot-server Success!" $status
fi

echo
echo -e "\n\nConfiguring Jenkins Start Job...\n"
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.sim.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=start-sim-server&mode=create`
if [ "$status" != "200" ] ; then
    echo -e "\nConfiguration of start-sim-server Failed!" $status
else
    echo -e "\nConfiguration of start-sim-server Success!" $status
fi

echo -e "\n\nConfiguring Jenkins Abort Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.abort.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=abort-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo -e "\nConfiguring Start Abort Failed!" $status
    exit 1
else
    echo -e "\nConfiguring Start Abort Success!" $status
fi

echo -e "\n\nConfiguring Jenkins Kill Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.kill.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=kill-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo -e "\nConfiguring Start Kill Failed!" $status
    exit 1
else
    echo -e "\nConfiguring Start Kill Success!" $status
fi

echo "done."
