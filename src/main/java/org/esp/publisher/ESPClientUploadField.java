package org.esp.publisher;

import java.io.File;
import java.util.Map;

import org.esp.domain.blueprint.SpatialDataType;
import org.vaadin.easyuploads.UploadField;

import com.google.common.net.MediaType;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Upload.FinishedEvent;

public class ESPClientUploadField extends UploadField {

    private static final String UPLOAD_MESSAGE = "Choose a %s to upload.  The file size limit is %s.";
    //private static final int FILE_SIZE_LIMIT = 157286400;

    private Map<Long, Map<String, Integer>> limits;
    
    SpatialDataType spatialDataType = new SpatialDataType();
    
    public ESPClientUploadField(Map<Long, Map<String, Integer>> limits) {
        super();
        setFieldType(FieldType.FILE);
        setCaption(UPLOAD_MESSAGE);
        spatialDataType.setId(1l);
        this.limits = limits;
    }

    private File file;
    private MediaType mt;

    @Override
    protected String getDisplayDetails() {

        StringBuilder sb = new StringBuilder();
        sb.append("File: ");
        sb.append(getLastFileName());
        return sb.toString();

    }

    public void uploadFinished(FinishedEvent event) {

        super.uploadFinished(event);

        String mimeType = event.getMIMEType();
        mt = MediaType.parse(mimeType);

    }

    public File getFile() {

        if (getValue() != null) {

            File file = (File) getValue();
            if(!checkLimits(file)) {
                return null;
            }
        }

//        if (mt != null && mt.is(MediaType.TIFF)) {
        //FIXME mt
            return file;
//        }
        
    }

    public void updateSpatialDataType(SpatialDataType spatialDataType) {
        this.spatialDataType = spatialDataType;
        if(spatialDataType != null){
            setCaption(String.format(UPLOAD_MESSAGE, getLabel(spatialDataType.getId()), getLimit(spatialDataType.getId())));
        }else{
            setCaption("");
        }        
    }

    private String getLimit(long spatialDataType) {
        return getFileSizeLimit(spatialDataType) + " Megabytes";
    }

    private Integer getFileSizeLimit(long spatialDataType) {
        return limits.get(spatialDataType).get("size");
    }

    private String getLabel(long spatialDataType) {
        if(spatialDataType == 1) {
            return "Raster (GeoTiff)";
        } else if(spatialDataType == 2) {
            return "Map (.zip Shapefile)";
        }
        return "";
    }

    public boolean checkLimits(File f) {
        int fileSizeLimit = getFileSizeLimit(spatialDataType.getId());
        if (f.length() > fileSizeLimit*1024l*1024l) {
            Notification
                .show("The file size exceeds the limit of " + fileSizeLimit + " Megabytes. Processing aborted.",
                        Notification.Type.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}
