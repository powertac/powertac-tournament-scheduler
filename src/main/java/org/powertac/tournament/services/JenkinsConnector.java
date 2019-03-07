package org.powertac.tournament.services;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class JenkinsConnector
{
  private static Properties properties = Properties.getProperties();

  public static void sendJob (String jobUrl) throws Exception
  {
    InputStream is = null;
    try {
      URL url = new URL(jobUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setInstanceFollowRedirects(false);

      String user = properties.getProperty("jenkins.username", "");
      String token = properties.getProperty("jenkins.token", "");
      if (!user.isEmpty() && !token.isEmpty()) {
        String userpass = String.format("%s:%s", user, token);
        String basicAuth = "Basic " +
            new String(new Base64().encode(userpass.getBytes()));
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setRequestMethod("POST");
      }
      else {
        conn.setRequestMethod("GET");
      }

      is = conn.getInputStream();
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static NodeList getNodeList ()
  {
    try {
      String url = properties.getProperty("jenkins.location")
          + "computer/api/xml";
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder docB = dbf.newDocumentBuilder();
      Document doc = docB.parse(new URL(url).openStream());
      return doc.getElementsByTagName("computer");
    }
    catch (IOException | ParserConfigurationException | SAXException ignored) {
    }
    return null;
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