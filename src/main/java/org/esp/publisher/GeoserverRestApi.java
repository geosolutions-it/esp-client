package org.esp.publisher;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.decoder.RESTDataStore;
import it.geosolutions.geoserver.rest.decoder.RESTFeatureType;
import it.geosolutions.geoserver.rest.decoder.RESTFeatureType.Attribute;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import it.jrc.persist.Dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.esp.domain.publisher.ColourMapEntry;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.SimpleHttpClient;
import org.geotools.ows.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GeoserverRestApi {

    private String workspace;

    private GeoServerRESTPublisher publisher;
    
    private GeoServerRESTReader reader;
    
    private Configuration configuration;
    
    private static Pattern searchAggregate = Pattern.compile("^.*<AggregationResults><.*?>([0-9.E\\-]+)</.*?></AggregationResults>.*$",java.util.regex.Pattern.DOTALL);
    
    String wpsUrl;
    
    String classifyUrl;
    
    String describeLayerUrl;
    
    boolean shapefileToPostgis = false;
    String postgisStoreName;
    
    private Logger logger = LoggerFactory.getLogger(GeoserverRestApi.class);
    
    private Pattern searchRules = Pattern.compile("^\\s*<Rules>\\s*(.*?)\\s*</Rules>\\s*$",Pattern.DOTALL);
    private Pattern searchRulesError = Pattern.compile("^\\s*<string>\\s*(.*?):\\s*(.*?)\\s*</string>\\s*$",Pattern.DOTALL);

    Dao dao;
    ShapefileToPostgisImporter importer;
    
    SimpleHttpClient httpClient = new SimpleHttpClient();



    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    @Inject
    public GeoserverRestApi(Configuration config,
            @Named("gs_rest_url") String restUrl,
            @Named("gs_workspace") String workspace,
            @Named("gs_user") String restUser,
            @Named("gs_password") String restPassword,
            @Named("gs_wps_url") String wpsUrl,
            @Named("shapefile_to_postgis") boolean shapefileToPostgis,
            @Named("postgis_store") String postgisStoreName,
            
            ShapefileToPostgisImporter importer,
            Dao dao)
            throws ServiceException, IOException {

        this.classifyUrl = restUrl
                + "/rest/sldservice/"
                + workspace
                + ":%s/classify.xml?attribute=%s&method=%s&intervals=%d&ramp=custom&startColor=0x%s&endColor=0x%s&open=true";
        
        this.describeLayerUrl = restUrl + "/ows?service=WFS&request=DescribeFeatureType&typeName=%s";
        
        this.dao = dao;
        this.publisher = new GeoServerRESTPublisher(restUrl, restUser,
                restPassword);
        this.reader = new GeoServerRESTReader(restUrl, restUser, restPassword);
        
        this.wpsUrl = wpsUrl;
        
        this.configuration = config;
        this.workspace = workspace;
        
        this.shapefileToPostgis = shapefileToPostgis;
        this.postgisStoreName = postgisStoreName;
        
        httpClient.setUser(restUser);
        httpClient.setPassword(restPassword);
        
        this.importer = importer;

    }

    /**
     * Publish a geotiff in the defined workspace.
     * 
     * @param geotiff
     * @param srs
     * @param layerAndStoreName
     * @param styleName TODO
     * @return
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     */
    public boolean publishTiff(File geotiff, String srs, String layerAndStoreName, String styleName)
            throws PublishException {

        logger.info("Publishing Geotiff");
        
        if (styleName == null) {
            styleName = layerAndStoreName;
        }

        try {
            return publisher.publishGeoTIFF(workspace, layerAndStoreName,
                    layerAndStoreName, geotiff, srs, ProjectionPolicy.NONE,
                    styleName, null);
        } catch (FileNotFoundException e) {
            throw new PublishException("File to publish not found", e);
        } catch (IllegalArgumentException e) {
            throw new PublishException("File to publish not valid", e);
        }
    }

    /**
     * Publish a shapefile in the defined workspace.
     * Depending on the shapefileToPostgis flag, the shapefile is published as is,
     * or imported into a configured postgis istance first.
     * 
     * @param zipFile
     * @param srs
     * @param layerAndStoreName
     * @param styleName
     * @return
     * @throws PublishException
     */
    public boolean publishShp(File zipFile, String srs,
            String layerAndStoreName, String styleName) throws PublishException {

        try {
            if(shapefileToPostgis) {
                importer.importShapefile(zipFile, layerAndStoreName, srs);
                GSFeatureTypeEncoder fte = new GSFeatureTypeEncoder();
                fte.setName(layerAndStoreName);
                fte.setTitle(layerAndStoreName);
                fte.setNativeCRS(srs);
                fte.setSRS(srs);
                fte.setEnabled(true);
                GSLayerEncoder layerEncoder = new GSLayerEncoder();
                layerEncoder.setEnabled(true);
                layerEncoder.setQueryable(true);
                layerEncoder.setDefaultStyle(styleName);
                return publisher.publishDBLayer( workspace, postgisStoreName, fte, layerEncoder);
            } else {
                return publisher.publishShp(workspace, layerAndStoreName,
                    layerAndStoreName, zipFile, srs, styleName);
            }
        } catch (FileNotFoundException e) {
            throw new PublishException("Shapefile not found: " + zipFile.getAbsolutePath(), e);
        } catch (IllegalArgumentException e) {
            throw new PublishException("Shapefile not valid: " + zipFile.getAbsolutePath(), e);
        }
    }

    
    /**
     * Deletes the store
     * 
     * @return
     */
    public RESTCoverageStore getRasterStore(String storeName) {

        logger.info("Fetching raster store");
        
        if (storeName == null) {
            return null;
        }

        return reader.getCoverageStore(workspace, storeName);

    }

    /**
     * Deletes the store
     * 
     * @return
     */
    public boolean removeRasterStore(String storeName) {

        logger.info("Removing raster store");
        
        if (storeName == null) {
            return false;
        }

        return publisher.removeCoverageStore(workspace, storeName, true);

    }

    /**
     * Updates a style using the given template and info to build the SLD.
     *  
     * @param styleName
     * @param attributeName
     * @param templateName
     * @param cmes
     * @param rules
     * @return
     * @throws PublishException
     */
    public boolean updateStyle(String styleName, String attributeName, String templateName,
            List<ColourMapEntry> cmes, String rules) throws PublishException {
        logger.info("Updating style: " + styleName);
        String sldBody;
        try {
            sldBody = buildSLDBody(styleName, attributeName, templateName, cmes, rules);
            return updateStyle(styleName, sldBody);
            
        } catch (IOException e) {
            throw new PublishException("Error reading template ("+templateName+") for style " + styleName, e);
        } catch (TemplateException e) {
            throw new PublishException("Error parsing template ("+templateName+") for style " + styleName, e);
        }

        
    }
    
    /**
     * 
     * Creates a new style using the given template and info to build the SLD.
     * 
     * @param styleName
     * @param attributeName
     * @param templateName
     * @param cmes
     * @param rules
     * @return
     * @throws PublishException
     */
    public boolean publishStyle(String styleName, String attributeName, String templateName,
            List<ColourMapEntry> cmes, String rules) throws PublishException {

        logger.info("Publishing style: " + styleName);
        String sldBody;
        try {
            sldBody = buildSLDBody(styleName, attributeName, templateName, cmes, rules);
            return publisher.publishStyle(sldBody);
        } catch (IOException e) {
            throw new PublishException("Error reading template ("+templateName+") for style " + styleName, e);
        } catch (TemplateException e) {
            throw new PublishException("Error parsing template ("+templateName+") for style " + styleName, e);
        }
    }
    

    private String buildSLDBody(String styleName, String attributeName, String templateName,
            List<ColourMapEntry> cmes, String rules) throws IOException, TemplateException {
        Template template = configuration.getTemplate(templateName);

        Map<String, Object> root = new HashMap<String, Object>();

        root.put("styleName", styleName);
        root.put("colourMapEntries", cmes);
        root.put("attributeName", attributeName);
        root.put("rules", rules);

        StringWriter sw = new StringWriter();
        template.process(root, sw);

        String sldBody = sw.toString();
        return sldBody;
    }

    public RESTFeatureType getLayerInfo(String layerName) {
        RESTLayer layer = reader.getLayer(layerName);
        if(layer != null) {
            return reader.getFeatureType(layer);
        }
        return null;
    }

    public double[] getExtrema(String layerName, String attributeName) {
        Template template;
        try {
            template = configuration.getTemplate("Aggregate.ftl");
       
        
            Map<String, Object> root = new HashMap<String, Object>();
    
            root.put("layerName", workspace+":"+layerName);
            root.put("attributeName", attributeName);
            root.put("functionName", "Max");
    
            StringWriter sw = new StringWriter();
            template.process(root, sw);
    
            String wpsRequest = sw.toString();
            SimpleHttpClient httpClient = new SimpleHttpClient();
            HTTPResponse response = httpClient.post(new URL(wpsUrl), IOUtils.toInputStream(wpsRequest), "application/xml");
            String wpsResponse = IOUtils.toString(response.getResponseStream());
            double max = 0.0;
            double min = 0.0;
            Matcher matcher = searchAggregate.matcher(wpsResponse);
            if(matcher.find()) {
                max = Double.parseDouble(matcher.group(1));
            }
            
            root.put("functionName", "Min");
            sw = new StringWriter();
            template.process(root, sw);
            wpsRequest = sw.toString();
            
            response = httpClient.post(new URL(wpsUrl), IOUtils.toInputStream(wpsRequest), "application/xml");
            wpsResponse = IOUtils.toString(response.getResponseStream());
            matcher = searchAggregate.matcher(wpsResponse);
            if(matcher.find()) {
                min = Double.parseDouble(matcher.group(1));
            }
            
            return new double[] {min, max};
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getClassifiedStyle(String layerName, String attributeName, String classificationMethod, String startColor,
            String endColor, int intervals) throws MalformedURLException, IOException, PublishException {
        String sldServiceUrl = String.format(classifyUrl, layerName, attributeName,
                classificationMethod, intervals, startColor, endColor);
        logger.debug("Sending request to SLDService: " + sldServiceUrl);
        HTTPResponse response = httpClient.get(new URL(sldServiceUrl));
        try {
            String rules = IOUtils.toString(response.getResponseStream());
            logger.debug("Response received from SLDService: " + rules);
            Matcher m = searchRules.matcher(rules);
            if(m.find()) {
                return m.group(1);
            }
            m = searchRulesError.matcher(rules);
            if(m.find()) {
                String errorType = m.group(1).toLowerCase();
                String errorDesc = m.group(2);
                if(errorType.equals("intervals")) {
                    throw new PublishException("The layer has " + errorDesc + " unique values. Try increasing # of intervals");
                }
            }
            throw new IOException("sldservice response is in the wrong format");
        } finally {
            response.dispose();
        }
    }

    public boolean removeShapefile(String layerName) throws PublishException {
        boolean result = publisher.unpublishFeatureType(workspace, shapefileToPostgis ? postgisStoreName :  layerName, layerName);
        if(result) {
            if(shapefileToPostgis) {
                importer.removeFeature(layerName);
            }
            RESTDataStore dataStore = reader.getDatastore(workspace, layerName);
            // for shapefiles we need to remove the datastore too
            publisher.removeDatastore(workspace, layerName);
        }
        return result;
    }

    public boolean removeStyle(String styleName) {
        logger.info("Removing style " + styleName);
        
        if (styleName == null) {
            return false;
        }
        return publisher.removeStyle(styleName, true);
    }

    public String getStyle(String styleName) {
        return reader.getSLD(styleName);
    }

    public boolean updateStyle(String styleName, String style) throws PublishException {
        GSLayerEncoder layer = new GSLayerEncoder();
        layer.setWmsPath("newpath");
        if(publisher.configureLayer(workspace, styleName, layer)) {
            logger.info("Configured " + styleName);
            return publisher.updateStyle(style, styleName);
        } else {
            throw new PublishException("Error configuring style " + styleName);
        }
    }

    public String getAttributesInfo(String layerName) throws MalformedURLException, IOException {
        String attributesUrl = String.format(describeLayerUrl, layerName);
        logger.debug("Sending request to WFS: " + attributesUrl);
        HTTPResponse response = httpClient.get(new URL(attributesUrl));
        try {
            String xml = IOUtils.toString(response.getResponseStream());
            logger.debug("Response received from WFS: " + xml);
            return xml;
        } finally {
            response.dispose();
        }
    }

    public String getGeometryType(String layerName) {
        RESTFeatureType featureType = getLayerInfo(layerName);
        for(Attribute attr : featureType.getAttributes()) {
            String binding = attr.getBinding();
            if(binding.startsWith("com.vividsolutions.jts.geom.")) {
                return binding.substring("com.vividsolutions.jts.geom.".length());
            }
        }
        
        return null;
    }

    

}
