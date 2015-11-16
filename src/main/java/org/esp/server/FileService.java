package org.esp.server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.PublishStatus;
import org.esp.domain.blueprint.SpatialDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class FileService {

    private Logger logger = LoggerFactory.getLogger(FileService.class);

    private File downloadFolder;

    private Provider<EntityManager> entityManagerProvider;

    @Inject
    public FileService(@Named("download_folder") String downloadFolderName, Provider<EntityManager> entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
        String dfn = FilenameUtils.separatorsToSystem(FilenameUtils.normalize(downloadFolderName));
        downloadFolder = new File(dfn);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs();
        }
    }

    public void uploadFile(Long id, String layerName, File fileToUpload, SpatialDataType datatType)
            throws IOException {
        String subfolder = getSubfolder(id,layerName,datatType);
        String fileName = layerName + "." + (datatType.getId() == 1 ? MediaType.TIFF.subtype() : MediaType.ZIP.subtype());
        String destination = FilenameUtils.concat(downloadFolder.getAbsolutePath() + "/" + subfolder, fileName);
        FileUtils.copyFile(fileToUpload, new File(destination));
    }

    public void deleteFile(Long id, String layerName) throws IOException {
        FileFilter fileFilter = new WildcardFileFilter("*" + id + "_" + layerName);
        File[] files = downloadFolder.listFiles(fileFilter);
        if(files.length == 1){
            FileUtils.deleteQuietly(files[0]);
        }else{
            logger.error("Unable to delete uploaded file for layer : " + layerName);
        }
    }

    public File getFile(Long id, Long typeId) throws IOException {
        File file = null;
        //Retrieve entity and check if is published
        EcosystemServiceIndicator entity = entityManagerProvider.get().find(EcosystemServiceIndicator.class, id);
        if(entity != null && entity.getStatus().getId() == PublishStatus.VALIDATED.getValue()){
            String type = decodeDataType(typeId);
            FileFilter fileFilter = new WildcardFileFilter(type + "_" + id + "*");
            File[] files = downloadFolder.listFiles(fileFilter);
            if(files.length == 1){
                if(files[0].isDirectory()){
                    File[] listOfFiles = files[0].listFiles();
                    if(listOfFiles.length == 1){
                        file = listOfFiles[0];
                    }
                }
            }else{
                logger.error("Unable to download uploaded file for ID : " + id);
            }
        }else{
            logger.error("Ecosystem Service Indicator with ID : " + id + " is not available");
        }
        return file;
    }

    private String getSubfolder(Long id, String layerName,SpatialDataType datatType){
        String subfolder = decodeDataType(datatType.getId());
        subfolder = subfolder + "_" + id + "_" + layerName;
        return subfolder;
    }

    private String decodeDataType(Long dataTypeId){
        String decodedDataType = "";
        if(dataTypeId == 1){ 
            //Raster
            decodedDataType = "R";
        }else{
            //Vector
            decodedDataType = "V";
        }
        return decodedDataType;
    }

}
