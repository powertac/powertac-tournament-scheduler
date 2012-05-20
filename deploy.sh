echo "Attempting to upload to url: " $1 
echo "Using tomcat Username: " $2
echo "Using tomcat Password: " $3
echo "Downloading Jenkins.."
wget --quiet http://mirrors.jenkins-ci.org/war/latest/jenkins.war
echo "Installing Jenkins..."
curl --upload-file ./jenkins.war "http://"$2":"$3"@"$1"/manager/text/deploy?path=/jenkins&update=true"
rm jenkins.war
echo "Configuring Jenkins..."
sleep 50
status=`curl -o /dev/null --write-out %{http_code} --upload-file ./config.xml -H Content-Type:text/xml http://$1/jenkins/createItem?name=start-server-instance&mode=create`

if [ "$status" != "200" ] ; then
    echo "Conifguration Failed!" $status
    exit 1
else
    echo "Configuration Success!" $status
fi

echo "done."
echo "Installing TS..."
mvn tomcat:redeploy
#curl --upload-file ./target/TournamentScheduler.war "http://"$2":"$3"@"$1"/manager/text/deploy?path=/TS&update=true"
echo "done."
