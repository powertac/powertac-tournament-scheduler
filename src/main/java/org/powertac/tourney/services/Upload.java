package org.powertac.tourney.services;

import org.apache.commons.io.IOUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.springframework.stereotype.Service;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.FileOutputStream;

@Service("upload")
public class Upload
{
  private UploadedFile uploadedFile;
  private String uploadLocation;

  public boolean submit (String fileName) {
    String filePath = uploadLocation + fileName;

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filePath);
      IOUtils.copy(uploadedFile.getInputStream(), fos);

      // Show succes message.
      String msg = "File upload success! " + filePath;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
    }
    catch (Exception e) {
      // Show error message.
      String msg = "File upload failed with I/O error. " + filePath;
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      e.printStackTrace();
      return false;
    }
    finally {
      IOUtils.closeQuietly(fos);
    }

    return true;
  }

  public void setUploadedFile (UploadedFile uploadedFile)
  {
    this.uploadedFile = uploadedFile;
  }

  public void setUploadLocation (String uploadLocation)
  {
    this.uploadLocation = uploadLocation;
  }

}
