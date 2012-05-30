package org.powertac.tourney.dl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LogDownloader
 */
@WebServlet(description = "Access to download compressed logfiles", urlPatterns = { "/LogDownloader" })
public class LogDownloader extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	
	// TODO - make this configurable...
	private String absolutePath = "/project/msse01/powertac/game-logs/";
	//private String absolutePath = "/home/jcollins/Desktop/";
       
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LogDownloader() {
	  super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
    // response.setContentType("application/force-download");
    String gameName = request.getParameter("game");
    //response.setCharacterEncoding("x-gzip");
    response.setContentType("application/x-tar; x-gzip");
    String downloadFile = "game-" + gameName + "-sim-logs.tar.gz";
    response.addHeader("Content-Disposition", "attachment; filename=\""
      + downloadFile + "\"");
    byte[] buf = new byte[1024];
    try {
      String realPath = absolutePath + downloadFile;// context.getRealPath("/resources/"
                                                                             // +
                                                                             // downloadFile);
      File file = new File(realPath);
      long length = file.length();
      BufferedInputStream in =
              new BufferedInputStream(new FileInputStream(file));
      ServletOutputStream out = response.getOutputStream();
      response.setContentLength((int) length);
      while ((in != null) && ((length = in.read(buf)) != -1)) {
        out.write(buf, 0, (int) length);
      }
      in.close();
      out.flush();
      out.close();
    }
    catch (Exception exc) {
      exc.printStackTrace();
    }
	}

}
