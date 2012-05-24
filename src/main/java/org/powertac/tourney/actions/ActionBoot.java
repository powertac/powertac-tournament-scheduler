package org.powertac.tourney.actions;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.powertac.tourney.services.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("actionBoot")
@Scope("request")
public class ActionBoot
{

  @Autowired
  Upload upload;

  private UploadedFile boot;

  public UploadedFile getBoot ()
  {
    return boot;
  }

  public void setBoot (UploadedFile boot)
  {
    this.boot = boot;
  }

  public void submit ()
  {
    upload.setUploadedFile(boot);
    upload.submit(boot.getName());

  }

}
