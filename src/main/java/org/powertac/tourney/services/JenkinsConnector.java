/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 9/27/12
 * Time: 12:48 PM
 */

package org.powertac.tourney.services;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLConnection;

public class JenkinsConnector {

  public static void sendJob (String jobUrl) throws Exception
  {
    TournamentProperties properties = TournamentProperties.getProperties();

    URL url = new URL(jobUrl);
    URLConnection conn = url.openConnection();

    String user = properties.getProperty("jenkins.username", "");
    String pass = properties.getProperty("jenkins.password", "");
    if (!user.isEmpty() && !pass.isEmpty()) {
      String userpass = String.format("%s:%s", user, pass);
      String basicAuth = "Basic " +
          new String(new Base64().encode(userpass.getBytes()));
      conn.setRequestProperty("Authorization", basicAuth);
    }

    conn.getInputStream();
  }

  public static NodeList getNodeList () throws Exception
  {
    TournamentProperties properties = TournamentProperties.getProperties();

    String url = properties.getProperty("jenkins.location")
        + "computer/api/xml";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder docB = dbf.newDocumentBuilder();
    Document doc = docB.parse(new URL(url).openStream());
    NodeList nList = doc.getElementsByTagName("computer");

    return nList;
  }
}