/**
 * Created by IntelliJ IDEA.
 * User: Govert Buijs
 * Date: 9/10/12
 * Time: 9:47 AM
 */

package org.powertac.tourney.test;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Principal;
import java.util.*;


public class Test
{

  public static void main(String args[]) throws Exception
  {
    // http://stackoverflow.com/questions/508019/jpa-hibernate-store-date-in-utc-time-zone
    // http://stackoverflow.com/questions/9823411/hibernate-force-timestamp-to-persist-load-as-utc
    // http://www.developerscrappad.com/228/java/java-ee/ejb3-jpa-dealing-with-date-time-and-timestamp/

    setup();

    //System.out.println("Hello!!!");


    String finalUrl = "http://130.115.197.49:8080"
        + "/TournamentScheduler/faces/serverInterface.jsp"
        + "?action=heartbeat"
        + "&gameId=3"
        + "&message=399"
        + "&standings=sample1:-6159.54425535747,sample5:-6261.904539793478,"
        + "sample4:-6546.004023574167,sample3:-6363.737838356869,"
        + "sample2:2867.4646795618655,sample8:-6759.968447673174,"
        + "sample7:-6535.3469221619125,sample6:-8077.44880207158,default";


    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      // Get the response
      InputStream input = conn.getInputStream();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("heartbeat failure");
    }



    /*HashMap<Integer, Integer> gameLengths = new HashMap<Integer, Integer>();
    int foo = gameLengths.get(10);
    System.out.println(gameLengths.get(10) + 10);*/

    //getGame();

    //List<User> users = User.getUserList();
    //System.out.println(users);

    //User user = User.getUserByName ("gbuijs");
    //System.out.println(user);

    //List<Pom> poms = Pom.getPomList();
    //System.out.println(poms);

    //List<Location> locations = Location.getLocationList();
    //System.out.println(locations);

    //getMachine();

    //List<Machine> machines = Machine.getMachineList();
    //System.out.println(machines);

    //getTournamentById();

    //getTournamentByName();

    //getNotCompleteTournaments();

    //getGameById();

    //getBootableSingleGames();
    //getBootableMultiGames();
    //getRunnableSingleGames();
    //getRunnableMultiGames();
    //getRunnableSingleGamesDate ();

    //games = Game.getNotCompleteGamesList();
    //System.out.println(games);
    //for (Game game: games) {
    //  System.out.println(game.getTournament());
    //  System.out.println(game.getMachine());
    //}

    //games = Game.getCompleteGamesList();
    //System.out.println(games);
    //for (Game game: games) {
    //  System.out.println(game.getTournament());
    //  System.out.println(game.getMachine());
    //}

    //List<Broker> brokers = Broker.getBrokerList();
    //System.out.println(brokers);

    //getBrokerById();

    //Broker broker = Broker.getBrokerByName("sample1");
    //System.out.println(broker);

    //getBrokerByAuth();

    //getAvailableTournaments();

    //Machine.checkMachines();

    //tryStartRunnableGames();

    //testBrokerLogin();

    //testEndOfGame();

    //testWinnerDetermination();

    System.out.println();
    System.out.println("Done !!!");
    System.exit(0);
  }

  public static void setup()
  {
    ApplicationContext context = new FileSystemXmlApplicationContext("src/main/webapp/WEB-INF/applicationContext.xml");
    SpringApplicationContext springApplicationContext = new SpringApplicationContext();
    springApplicationContext.setApplicationContext(context);
  }

  public static void testWinnerDetermination()
  {
    String gameName = "test_result_2_3_1";

    String[] parts = gameName.split("_");
    String type = parts[parts.length - 1];

    System.out.println(Arrays.asList(parts));
    System.out.println(parts[parts.length - 1]);
    System.out.println(parts[parts.length - 2]);


  }

  public static void testEndOfGame()
  {
    HttpServletRequest request = getRequest("192.168.56.103");
    Map<String, String[]> params = new HashMap<String, String[]>();
    String[] string1 = new String[]{Constants.Rest.REQ_PARAM_GAMERESULTS};
    String[] string2 = new String[]{
        "sample1:271407.5679242534,default broker:267110.9106018134"};
    //"sample1:196004.8514476629,sample4:228215.93411946768,sample3:271407.5679242534,sample2:258379.00782030486,default broker:267110.9106018134"};
    params.put(Constants.Rest.REQ_PARAM_ACTION, string1);
    params.put(Constants.Rest.REQ_PARAM_MESSAGE, string2);
    Rest rest = new Rest();

    String result = rest.handleServerInterfacePOST(params, request);
    System.out.println(result);
  }

  public static void getGame()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();

    Transaction transaction = session.beginTransaction();
    try {
      Game game = (Game) session.get(Game.class, 1);
      System.out.println(game.getGameId());

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getGameById()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_GAME_BY_ID);
      query.setInteger("gameId", 1);
      Game game = (Game) query.uniqueResult();
      System.out.println(game);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getMachine()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();

    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.
          createQuery(Constants.HQL.GET_MACHINE_BY_MACHINENAME);
      query.setString("machineName", "ubuntu0");
      Machine machine = (Machine) query.uniqueResult();
      System.out.println(machine);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getTournamentById()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_TOURNAMENT_BY_ID);
      query.setInteger("tournamentId", 1);
      Tournament tournament = (Tournament) query.uniqueResult();
      System.out.println(tournament);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getTournamentByName()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_TOURNAMENT_BY_NAME);
      query.setString("tournamentName", "test3");
      Tournament tournament = (Tournament) query.uniqueResult();
      System.out.println(tournament);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getNotCompleteTournaments()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      @SuppressWarnings("unchecked")
      List<Tournament> tournaments = (List<Tournament>) session
          .createQuery(Constants.HQL.GET_TOURNAMENTS_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

      System.out.println(tournaments);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getBootableSingleGames()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      @SuppressWarnings("unchecked")
      List<Game> games = session
          .createQuery(Constants.HQL.GET_GAMES_SINGLE_BOOT_PENDING)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      System.out.println(games);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getBootableMultiGames()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      @SuppressWarnings("unchecked")
      List<Game> games = session
          .createQuery(Constants.HQL.GET_GAMES_MULTI_BOOT_PENDING)
          .setInteger("tournamentId", 1)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      System.out.println(games);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getRunnableSingleGames()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      List<Game> games = Game.getStartableSingleGames(session);

      System.out.println(games);

      for (Game game : games) {
        String brokers = "";

        for (Agent agent : game.getAgentMap().values()) {
          if (!agent.getBroker().agentsAvailable()) {
            System.out.println(String.format("Not starting game %s : broker %s doesn't have "
                + "enough available agents",
                game.getGameId(), agent.getBroker().getBrokerId()));
          }

          brokers += agent.getBroker().getBrokerName() + "/";
          brokers += agent.getBrokerQueue() + ",";
        }
        brokers = brokers.substring(0, brokers.length() - 1);
        System.out.println(brokers);
      }
      System.out.println();

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getRunnableSingleGamesDate()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      String GET_GAMES_SINGLE_BOOT_COMPLETE =
          "FROM Game AS game "
              + "LEFT JOIN FETCH game.tournament AS tournament "
              + "LEFT JOIN FETCH game.machine "

              + "WHERE game.status='" + Game.STATE.boot_complete.toString() + "' "
              + "AND game.startTime < :startTime "
              + "AND tournament.type='" + Tournament.TYPE.SINGLE_GAME + "'";


      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.HOUR, -2);
      Date date = calendar.getTime();

      @SuppressWarnings("unchecked")
      List<Game> games = session
          .createQuery(GET_GAMES_SINGLE_BOOT_COMPLETE)
          .setTimestamp("startTime", Utils.offsetDate())
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      for (Game game : games) {
        System.out.println(game.getGameId() + " : " + game.getStartTime());
      }

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getRunnableMultiGames()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Tournament tournament = (Tournament) session.get(Tournament.class, 10);

      List<Game> games = Game.getStartableMultiGames(session, tournament);

      System.out.println(games);

      for (Game game : games) {
        String brokers = "";

        for (Agent agent : game.getAgentMap().values()) {
          if (!agent.getBroker().agentsAvailable()) {
            System.out.println(String.format("Not starting game %s : broker %s doesn't have "
                + "enough available agents",
                game.getGameId(), agent.getBroker().getBrokerId()));
          }

          brokers += agent.getBroker().getBrokerName() + "/";
          brokers += agent.getBrokerQueue() + ",";
        }
        brokers = brokers.substring(0, brokers.length() - 1);
        System.out.println(brokers);
      }

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getBrokerById()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Broker broker = (Broker) session
          .createQuery(Constants.HQL.GET_BROKER_BY_ID)
          .setInteger("brokerId", 1).uniqueResult();
      System.out.println(broker);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getBrokerByAuth()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Query query = session.createQuery(Constants.HQL.GET_BROKER_BY_BROKERAUTH);
      query.setString("brokerAuth", "7b57b79f2d1467ecc14b59fd00a5fa4b");
      Broker broker = (Broker) query.uniqueResult();
      System.out.println(broker);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public static void getAvailableTournaments()
  {
    for (Tournament tourney : Tournament.getNotCompleteTournamentList()) {
      System.out.println(tourney.getTournamentId());
    }
  }

  public static void tryStartRunnableGames()
  {
    Tournament tournament = null;

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      tournament = (Tournament) session.get(Tournament.class, 14);

      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    if (tournament != null) {
      RunGame.startRunnableGames(tournament);
    }
  }

  public static void testBrokerLogin()
  {
    Rest rest = new Rest();

    Map<String, String[]> params = new HashMap<String, String[]>();
    String[] params1 = {"7b57b79f2d1467ecc14b59fd00a5fa4b"};
    String[] params2 = {"xml"};
    String[] params3 = {"test0"};
    params.put("authToken", params1);
    params.put("type", params2);
    params.put("requestJoin", params3);

    System.out.println();
    String result = rest.parseBrokerLogin(params);
    System.out.println(result);
  }

  public static HttpServletRequest getRequest(final String remoteAddres)
  {
    HttpServletRequest request = new HttpServletRequest()
    {
      @Override
      public String getRemoteAddr()
      {
        return remoteAddres;
      }

      @Override
      public String getAuthType()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Cookie[] getCookies()
      {
        return new Cookie[0];  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public long getDateHeader(String s)
      {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getHeader(String s)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getHeaders(String s)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getHeaderNames()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getIntHeader(String s)
      {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getMethod()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getPathInfo()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getPathTranslated()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getContextPath()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getQueryString()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRemoteUser()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isUserInRole(String s)
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Principal getUserPrincipal()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRequestedSessionId()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRequestURI()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public StringBuffer getRequestURL()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getServletPath()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public HttpSession getSession(boolean b)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public HttpSession getSession()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdValid()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdFromCookie()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdFromURL()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdFromUrl()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void login(String s, String s2) throws ServletException
      {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void logout() throws ServletException
      {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Collection<Part> getParts() throws IOException, ServletException
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Part getPart(String s) throws IOException, ServletException
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Object getAttribute(String s)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getAttributeNames()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getCharacterEncoding()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void setCharacterEncoding(String s) throws UnsupportedEncodingException
      {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getContentLength()
      {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getContentType()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public ServletInputStream getInputStream() throws IOException
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getParameter(String s)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getParameterNames()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String[] getParameterValues(String s)
      {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Map getParameterMap()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getProtocol()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getScheme()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getServerName()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getServerPort()
      {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public BufferedReader getReader() throws IOException
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRemoteHost()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void setAttribute(String s, Object o)
      {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void removeAttribute(String s)
      {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Locale getLocale()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getLocales()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isSecure()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public RequestDispatcher getRequestDispatcher(String s)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRealPath(String s)
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getRemotePort()
      {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getLocalName()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getLocalAddr()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getLocalPort()
      {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public ServletContext getServletContext()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public AsyncContext startAsync() throws IllegalStateException
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isAsyncStarted()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isAsyncSupported()
      {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public AsyncContext getAsyncContext()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public DispatcherType getDispatcherType()
      {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }
    };
    return request;
  }
}