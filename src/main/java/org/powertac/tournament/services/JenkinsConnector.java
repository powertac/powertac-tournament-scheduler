package org.powertac.tournament.services;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class JenkinsConnector
{
  private static TournamentProperties properties =
      TournamentProperties.getProperties();

  public static void sendJob (String jobUrl, boolean usePOST) throws Exception
  {
    InputStream is = null;

    try {
      URL url = new URL(jobUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      if (usePOST) {
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
      }

      String user = properties.getProperty("jenkins.username", "");
      String token = properties.getProperty("jenkins.token", "");
      if (!user.isEmpty() && !token.isEmpty()) {
        String userpass = String.format("%s:%s", user, token);
        String basicAuth = "Basic " +
            new String(new Base64().encode(userpass.getBytes()));
        conn.setRequestProperty("Authorization", basicAuth);
      }

      is = conn.getInputStream();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static NodeList getNodeList () throws Exception
  {
    String url = properties.getProperty("jenkins.location")
        + "computer/api/xml";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder docB = dbf.newDocumentBuilder();
    Document doc = docB.parse(new URL(url).openStream());
    return doc.getElementsByTagName("computer");
  }

  public static NodeList getExecutorList (String machineName, int number)
      throws Exception
  {
    String url = properties.getProperty("jenkins.location")
        + "computer/" + machineName + "/executors/" + number + "/api/xml";

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder docB = dbf.newDocumentBuilder();
    Document doc = docB.parse(new URL(url).openStream());
    return doc.getElementsByTagName("idle");
  }
}