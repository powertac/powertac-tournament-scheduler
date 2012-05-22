echo "Attempting to upload to url: " $1 
echo "Using tomcat Username: " $2
echo "Using tomcat Password: " $3
echo "Downloading Jenkins.."
wget --quiet http://mirrors.jenkins-ci.org/war/latest/jenkins.war
echo "Installing Jenkins..."
curl --upload-file ./jenkins.war "http://"$2":"$3"@"$1"/manager/text/deploy?path=/jenkins&update=true"
rm jenkins.war
