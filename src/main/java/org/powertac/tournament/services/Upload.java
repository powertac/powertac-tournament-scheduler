package org.powertac.tournament.services;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import java.io.File;
import java.io.FileOutputStream;


public class Upload
{
  private static Logger log = Utils.getLogger();

  private static TournamentProperties properties =
      TournamentProperties.getProperties();

  private UploadedFile uploadedFile;

  public Upload (UploadedFile uploadedFile)
  {
    this.uploadedFile = uploadedFile;
  }

  public String submit (String location, String fileName)
  {
    String uploadLocation = properties.getProperty(location);
    File filePath = new File(new File(uploadLocation), fileName);

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filePath);
      IOUtils.copy(uploadedFile.getInputStream(), fos);
      return "File upload success! " + filePath.getPath();
    }
    catch (Exception e) {
      log.error("File upload failed with I/O error. " + filePath.getPath());
      e.printStackTrace();
      return "File upload failed with I/O error. " + filePath.getPath();
    }
    finally {
      IOUtils.closeQuietly(fos);
    }
  }
}
