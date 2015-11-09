package org.esp.server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.esp.domain.blueprint.SpatialDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class FileService {

    private Logger logger = LoggerFactory.getLogger(FileService.class);
    
    private File downloadFolder;

    @Inject
    public FileService(@Named("download_folder") String downloadFolderName) {
        String dfn = FilenameUtils.separatorsToSystem(FilenameUtils.normalize(downloadFolderName));
        downloadFolder = new File(dfn);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs();
        }
    }

    public void uploadFile(String layerName, File fileToUpload, SpatialDataType datatType)
            throws IOException {
        String fileName = layerName + "." + (datatType.getId() == 1 ? MediaType.TIFF.subtype() : MediaType.ZIP.subtype());
        String destination = FilenameUtils.concat(downloadFolder.getAbsolutePath(), fileName);
        FileUtils.copyFile(fileToUpload, new File(destination));
    }

    public void deleteFile(String layerName) throws IOException {
        FileFilter fileFilter = new WildcardFileFilter(layerName+".*");
        File[] files = downloadFolder.listFiles(fileFilter);
        if(files.length == 1){
            FileUtils.deleteQuietly(files[0]);
        }else{
            logger.error("Unable to delete uploaded file for layer : " + layerName);
        }
    }

    public void downloadFile(String layerName) throws IOException {
        FileFilter fileFilter = new WildcardFileFilter(layerName+".*");
        File[] files = downloadFolder.listFiles(fileFilter);
        if(files.length == 1){
            //TODO:downlaod
        }else{
            logger.error("Unable to download uploaded file for layer : " + layerName);
        }
    }

}
