package org.powertac.tournament.services;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class JenkinsConnector
{
  private static Properties properties = Properties.getProperties();

  private static String getBasicAuth () {
    String authStr = String.format("%s:%s",
        properties.getProperty("jenkins.username"),
        properties.getProperty("jenkins.token"));
    return "Basic " + new String(new Base64().encode(authStr.getBytes()));
  }

  public static String checkJenkinsLocation ()
  {
    InputStream is = null;
    try {
      URL url = new URL(properties.getProperty("jenkins.location") + "login");
      URLConnection conn = url.openConnection();

      is = conn.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      rd.read();
      rd.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      return "Jenkins Location could not be reached!";
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
    return null;
  }

  public static void sendJob (String jobUrl) throws Exception
  {
    InputStream is = null;
    try {
      URL url = new URL(jobUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setInstanceFollowRedirects(false);
      conn.setRequestProperty("Authorization", getBasicAuth());
      conn.setRequestMethod("POST");
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
      String url = String.format("%scomputer/api/xml",
          properties.getProperty("jenkins.location"));
      HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("Authorization", getBasicAuth());
      con.setDoOutput(true);

      DocumentBuilderFactory factoryBuilder = DocumentBuilderFactory.newInstance();
      factoryBuilder.setIgnoringComments(true);
      factoryBuilder.setIgnoringElementContentWhitespace(true);
      DocumentBuilder docB = factoryBuilder.newDocumentBuilder();
      Document doc = docB.parse(con.getInputStream());
      return doc.getElementsByTagName("computer");
    }
    catch (IOException | ParserConfigurationException | SAXException ignored) {
    }
    return null;
  }

  public static NodeList getExecutorList (String machineName, int number)
  {
    try {
      String url = String.format("%scomputer/%s/executors/%s/api/xml",
          properties.getProperty("jenkins.location"), machineName, number);
      HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("Authorization", getBasicAuth());
      con.setDoOutput(true);

      DocumentBuilderFactory factoryBuilder = DocumentBuilderFactory.newInstance();
      factoryBuilder.setIgnoringComments(true);
      factoryBuilder.setIgnoringElementContentWhitespace(true);
      DocumentBuilder docB = factoryBuilder.newDocumentBuilder();
      Document doc = docB.parse(con.getInputStream());
      return doc.getElementsByTagName("idle");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
