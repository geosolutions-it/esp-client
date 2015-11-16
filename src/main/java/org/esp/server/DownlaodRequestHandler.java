package org.esp.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

public class DownlaodRequestHandler implements RequestHandler {

    private Logger logger = LoggerFactory.getLogger(DownlaodRequestHandler.class);

    public static final String DOWNLOAD_ORIGINAL_URL = "getoriginal";

    private FileService fileService;

    @Inject
    public DownlaodRequestHandler(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        try {
            String[] pathInfo = StringUtils.split(request.getPathInfo(), "/");
            if (pathInfo.length > 0 && (DOWNLOAD_ORIGINAL_URL).equals(pathInfo[0])) {
                Long id = Long.parseLong(pathInfo[1]);
                Long typeId = Long.parseLong(pathInfo[2]);
                File file = fileService.getFile(id, typeId);
                if (file != null && file.length() > 0) {
                    response.setContentType(URLConnection.guessContentTypeFromName(file.getName()));
                    response.setHeader("Content-Disposition",
                            "attachment; filename=" + file.getName());
                    response.setCacheTime(-1);
                    IOUtils.copy(new FileInputStream(file), response.getOutputStream());
                }
            }else{          
                return false;
            }
        } catch (Exception e) {
            if(logger.isErrorEnabled()){
                logger.error(e.getMessage(),e);
                response.sendError(HttpStatus.SC_NOT_FOUND,"");
            }
        }
        return true;
    }

}
