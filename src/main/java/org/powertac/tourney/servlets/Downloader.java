package org.powertac.tourney.servlets;

import org.powertac.tourney.services.TournamentProperties;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Servlet implementation class Downloader
 */
@WebServlet(description = "Access to download compressed logfiles",
            urlPatterns = { "/Downloader" })
public class Downloader extends HttpServlet
{
  TournamentProperties properties = TournamentProperties.getProperties();
  String absolutePath = properties.getProperty("logLocation");

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
	{
    String gameId = request.getParameter("game");
    String csvName = request.getParameter("csv");

    if (gameId != null) {
      String downloadFile = "game-" + gameId + "-sim-logs.tar.gz";
      response.setContentType("application/x-tar; x-gzip");
      response.addHeader("Content-Disposition", "attachment; filename=\""
          + downloadFile + "\"");
      streamFile (response, downloadFile);
    } else if (csvName != null) {
      String downloadFile = csvName + ".csv";
      response.setContentType("text/csv");
      response.addHeader("Content-Disposition", "attachment; filename=\""
          + downloadFile + "\"");
      streamFile (response, downloadFile);
    }
	}

  private void streamFile (HttpServletResponse response, String downloadFile)
  {
    byte[] buf = new byte[1024];
    try {
      String realPath = absolutePath + downloadFile;
      File file = new File(realPath);
      long length = file.length();
      BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
      ServletOutputStream out = response.getOutputStream();
      response.setContentLength((int) length);
      while ((length = in.read(buf)) != -1) {
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
