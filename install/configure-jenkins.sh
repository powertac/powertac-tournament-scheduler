if [ -z "$1" ]; then
    echo "You need to specify the Jenkins URl"
    echo "Usually this is localhost:8080"
    echo
    exit 1
fi

echo "Configuring Jenkins Start Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=start-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo "Configuring Start Job Failed!" $status
    exit 1
else
    echo "Configuring Start Job Success!" $status
fi

echo "Configuring Jenkins Kill Job..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.kill.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=kill-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo "Configuring Start Kill Failed!" $status
    exit 1
else
    echo "Configuring Start Kill Success!" $status
fi




echo "done."
