if [ -z "$1" ]; then
    echo "You need to specify the Jenkins URl"
    echo "Usually this is localhost:8080"
    echo
    exit 1
fi

echo "Configuring Jenkins..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=start-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo "Configuration Failed!" $status
    exit 1
else
    echo "Configuration Success!" $status
fi

echo "done."
