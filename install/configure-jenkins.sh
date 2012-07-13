echo "Configuring Jenkins..."
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=start-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo "Configuration Failed!" $status
    exit 1
else
    echo "Configuration Success!" $status
fi

echo "done."
