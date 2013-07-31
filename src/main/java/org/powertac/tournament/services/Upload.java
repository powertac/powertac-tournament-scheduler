package org.powertac.tournament.services;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.FileOutputStream;


public class Upload
{
  private static Logger log = Logger.getLogger("TMLogger");

  private UploadedFile uploadedFile;
  private String uploadLocation;

  public boolean submit (String fileName)
  {
    String filePath = uploadLocation + fileName;

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filePath);
      IOUtils.copy(uploadedFile.getInputStream(), fos);
      message(0, "File upload success! " + filePath);
    } catch (Exception e) {
      message(0, "File upload failed with I/O error. " + filePath);
      log.error("File upload failed with I/O error. " + filePath);
      e.printStackTrace();
      return false;
    } finally {
      IOUtils.closeQuietly(fos);
    }

    return true;
  }

  private void message (int field, String msg)
  {
    FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
    if (field == 0) {
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
    }
  }

  //<editor-fold desc="Setters and Getters">
  public void setUploadedFile (UploadedFile uploadedFile)
  {
    this.uploadedFile = uploadedFile;
  }

  public void setUploadLocation (String uploadLocation)
  {
    this.uploadLocation = uploadLocation;
  }
  //</editor-fold>
}
