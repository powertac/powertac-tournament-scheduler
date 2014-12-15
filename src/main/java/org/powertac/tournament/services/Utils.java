package org.powertac.tournament.services;

import org.apache.log4j.Logger;
import org.powertac.tournament.beans.Agent;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.*;


public class Utils
{
  private static Logger log = getLogger();

  public static Logger getLogger ()
  {
    return Logger.getLogger("TSLogger");
  }

  public static void secondsSleep (int seconds)
  {
    try {
      Thread.sleep(seconds * 1000);
    }
    catch (Exception ignored) {
    }
  }

  public static void sendMail (String sub, String msg, String recipient)
  {
    TournamentProperties properties = TournamentProperties.getProperties();
    final String username = properties.getProperty("gmail.username");
    final String password = properties.getProperty("gmail.password");

    if (username.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
      return;
    }

    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");

    Session session = Session.getInstance(props,
        new javax.mail.Authenticator()
        {
          protected PasswordAuthentication getPasswordAuthentication ()
          {
            return new PasswordAuthentication(username, password);
          }
        });

    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      message.setRecipients(Message.RecipientType.TO,
          InternetAddress.parse(recipient));
      message.setSubject(sub);
      message.setText(msg);

      Transport.send(message);

      log.info("Done sending mail to : " + recipient);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void redirect ()
  {
    redirect("index.xhtml");
  }

  public static void redirect (String url)
  {
    try {
      ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
      externalContext.redirect(url);
    }
    catch (Exception ignored) {
    }
  }

  private static Random queueGenerator = new Random(new Date().getTime());

  public static String createQueueName ()
  {
    return Long.toString(queueGenerator.nextLong(), 31);
  }

  public static String dateToStringFull (Date date)
  {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }

  public static String dateToStringMedium (Date date)
  {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }

  public static String dateToStringSmall (Date date)
  {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }

  public static Date stringToDateMedium (String dateString)
  {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      return sdf.parse(dateString);
    }
    catch (Exception e) {
      return null;
    }
  }

  public static Date offsetDate ()
  {
    return offsetDate(new Date());
  }

  public static Date offsetDate (int extraOffset)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.HOUR, extraOffset);
    return offsetDate(calendar.getTime());
  }

  // This creates a UTC date from a local date
  public static Date offsetDate (Date date)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    if (TimeZone.getDefault().inDaylightTime(date)) {
      calendar.add(Calendar.HOUR, -1);
    }
    TimeZone tz = Calendar.getInstance().getTimeZone();
    calendar.add(Calendar.MILLISECOND, -1 * tz.getRawOffset());

    return calendar.getTime();
  }

  // Converts a UTS timestamp to a server-local time
  public static Date dateFromLong (long time)
  {
    Calendar cal = Calendar.getInstance();
    int diff = cal.get(Calendar.DST_OFFSET) + cal.get(Calendar.ZONE_OFFSET);
    cal.setTimeInMillis(time * 1000 + diff);
    return cal.getTime();
  }

  public static void growlMessage (String title, String message)
  {
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_INFO, title, message));
  }

  public static void growlMessage (String message)
  {
    growlMessage("Error", message);
  }

  public static class agentIdComparator implements Comparator<Agent>
  {
    public int compare (Agent agent1, Agent agent2)
    {
      return agent1.getAgentId() - agent2.getAgentId();
    }
  }

  /*
  * The Alphanum Algorithm is an improved sorting algorithm for strings
  * containing numbers.  Instead of sorting numbers in ASCII order like
  * a standard sort, this algorithm sorts numbers in numeric order.
  *
  * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
  *
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
  *
  */

  /**
   * This is an updated version with enhancements made by Daniel Migowski,
   * Andre Bogus, and David Koelle
   * <p/>
   * To convert to use Templates (Java 1.5+):
   * - Change "implements Comparator" to "implements Comparator<String>"
   * - Change "compare(Object o1, Object o2)" to "compare(String s1, String s2)"
   * - Remove the type checking and casting in compare().
   * <p/>
   * To use this class:
   * Use the static "sort" method from the java.util.Collections class:
   * Collections.sort(your list, new AlphanumComparator());
   */
  public static class AlphanumComparator implements Comparator
  {
    private boolean isDigit (char ch)
    {
      return ch >= 48 && ch <= 57;
    }

    /**
     * Length of string is passed in for improved efficiency (only need to calculate it once) *
     */
    private String getChunk (String s, int slength, int marker)
    {
      StringBuilder chunk = new StringBuilder();
      char c = s.charAt(marker);
      chunk.append(c);
      marker++;
      if (isDigit(c)) {
        while (marker < slength) {
          c = s.charAt(marker);
          if (!isDigit(c)) {
            break;
          }
          chunk.append(c);
          marker++;
        }
      }
      else {
        while (marker < slength) {
          c = s.charAt(marker);
          if (isDigit(c)) {
            break;
          }
          chunk.append(c);
          marker++;
        }
      }
      return chunk.toString();
    }

    public int compare (Object o1, Object o2)
    {
      if (!(o1 instanceof String) || !(o2 instanceof String)) {
        return 0;
      }
      String s1 = (String) o1;
      String s2 = (String) o2;

      int thisMarker = 0;
      int thatMarker = 0;
      int s1Length = s1.length();
      int s2Length = s2.length();

      while (thisMarker < s1Length && thatMarker < s2Length) {
        String thisChunk = getChunk(s1, s1Length, thisMarker);
        thisMarker += thisChunk.length();

        String thatChunk = getChunk(s2, s2Length, thatMarker);
        thatMarker += thatChunk.length();

        // If both chunks contain numeric characters, sort them numerically
        int result;
        if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
          // Simple chunk comparison by length.
          int thisChunkLength = thisChunk.length();
          result = thisChunkLength - thatChunk.length();
          // If equal, the first different number counts
          if (result == 0) {
            for (int i = 0; i < thisChunkLength; i++) {
              result = thisChunk.charAt(i) - thatChunk.charAt(i);
              if (result != 0) {
                return result;
              }
            }
          }
        }
        else {
          result = thisChunk.compareTo(thatChunk);
        }

        if (result != 0) {
          return result;
        }
      }

      return s1Length - s2Length;
    }
  }
}
