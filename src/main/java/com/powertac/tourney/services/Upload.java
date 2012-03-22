package com.powertac.tourney.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.springframework.stereotype.Service;

@Service
public class Upload {

    // Init ---------------------------------------------------------------------------------------

    private UploadedFile uploadedFile;
    private String fileName;

    // Actions ------------------------------------------------------------------------------------

    public void submit() {

        // Just to demonstrate what information you can get from the uploaded file.
        System.out.println("File type: " + uploadedFile.getContentType());
        System.out.println("File name: " + uploadedFile.getName());
        System.out.println("File size: " + uploadedFile.getSize() + " bytes");

        // Prepare filename prefix and suffix for an unique filename in upload folder.
        String prefix = FilenameUtils.getBaseName(uploadedFile.getName());
        String suffix = FilenameUtils.getExtension(uploadedFile.getName());
        
        // Prepare file and outputstream.
        File file = null;
        OutputStream output = null;
        
        try {
            // Create file with unique name in upload folder and write to it.
            file = File.createTempFile(prefix + "_", "." + suffix, new File("."));
            output = new FileOutputStream(file);
            IOUtils.copy(uploadedFile.getInputStream(), output);
            fileName = file.getName();

            // Show succes message.
            FacesContext.getCurrentInstance().addMessage("uploadForm", new FacesMessage(
                FacesMessage.SEVERITY_INFO, "File upload succeed!", null));
        } catch (IOException e) {
            // Cleanup.
            if (file != null) file.delete();

            // Show error message.
            FacesContext.getCurrentInstance().addMessage("uploadForm", new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "File upload failed with I/O error.", null));

            // Always log stacktraces (with a real logger).
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    // Getters ------------------------------------------------------------------------------------

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public String getFileName() {
        return fileName;
    }

    // Setters ------------------------------------------------------------------------------------

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

}
