/*
 * Copyright (c) 2014-2015, Argonne National Laboratory.
 *
 * SVN Information:
 *   $HeadURL$
 *   $Date$
 *   $Revision$
 *   $Author$
 */
package gov.anl.aps.cdb.portal.model.jsf.beans;

import gov.anl.aps.cdb.common.constants.CdbPropertyValue;
import gov.anl.aps.cdb.common.exceptions.ImageProcessingFailed;
import gov.anl.aps.cdb.portal.model.db.entities.PropertyValue;
import gov.anl.aps.cdb.common.utilities.FileUtility;
import gov.anl.aps.cdb.common.utilities.ImageUtility;
import gov.anl.aps.cdb.portal.utilities.SessionUtility;
import gov.anl.aps.cdb.portal.utilities.StorageUtility;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import org.apache.log4j.Logger;

import org.primefaces.model.UploadedFile;

/**
 * JSF bean for property value image upload.
 */
@Named("propertyValueImageUploadBean")
@RequestScoped
public class PropertyValueImageUploadBean {
    
    private static final Logger logger = Logger.getLogger(PropertyValueImageUploadBean.class.getName());

    private UploadedFile uploadedFile;
    private PropertyValue propertyValue;

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public PropertyValue getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(PropertyValue propertyValue) {
        this.propertyValue = propertyValue;
    }

    public void upload(PropertyValue propertyValue) {
        this.propertyValue = propertyValue;
        Path uploadDirPath;
        try {
            if (uploadedFile != null && !uploadedFile.getFileName().isEmpty()) {
                String uploadedExtension = FileUtility.getFileExtension(uploadedFile.getFileName());

                uploadDirPath = Paths.get(StorageUtility.getFileSystemPropertyValueImagesDirectory());
                logger.debug("Using property value images directory: " + uploadDirPath.toString());
                if (Files.notExists(uploadDirPath)) {
                    Files.createDirectory(uploadDirPath);
                }
                File uploadDir = uploadDirPath.toFile();

                String imageFormat = uploadedExtension;
                String originalExtension = "." + uploadedExtension + CdbPropertyValue.IMAGE_ORIGINAL_EXTENSION;
                if (uploadedExtension.isEmpty()) {
                    originalExtension = CdbPropertyValue.IMAGE_ORIGINAL_EXTENSION;
                    imageFormat = ImageUtility.DEFAULT_IMAGE_FORMAT;
                }
                File originalFile = File.createTempFile(CdbPropertyValue.IMAGE_PREFIX, originalExtension, uploadDir);
                String baseName = originalFile.getName().replace(CdbPropertyValue.IMAGE_ORIGINAL_EXTENSION, "");
                InputStream input = uploadedFile.getInputstream();
                Files.copy(input, originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Saved file: " + originalFile.toPath());
                byte[] originalData = Files.readAllBytes(originalFile.toPath());
                byte[] thumbData = ImageUtility.resizeImage(originalData, StorageUtility.THUMBNAIL_IMAGE_SIZE, imageFormat);
                String thumbFileName = originalFile.getAbsolutePath().replace(CdbPropertyValue.IMAGE_ORIGINAL_EXTENSION, CdbPropertyValue.IMAGE_THUMBNAIL_EXTENSION);
                Path thumbPath = Paths.get(thumbFileName);
                Files.write(thumbPath, thumbData);
                byte[] scaledData = ImageUtility.resizeImage(originalData, StorageUtility.SCALED_IMAGE_SIZE, imageFormat);
                String scaledFileName = originalFile.getAbsolutePath().replace(CdbPropertyValue.IMAGE_ORIGINAL_EXTENSION, CdbPropertyValue.IMAGE_SCALED_EXTENSION);
                Path scaledPath = Paths.get(scaledFileName);
                Files.write(scaledPath, scaledData);
                propertyValue.setValue(baseName);
                SessionUtility.addInfoMessage("Success", "Uploaded file " + uploadedFile.getFileName() + ".");
            }
        } catch (IOException | ImageProcessingFailed ex) {
            logger.error(ex);
            SessionUtility.addErrorMessage("Error", ex.toString());
        }
    }
}
