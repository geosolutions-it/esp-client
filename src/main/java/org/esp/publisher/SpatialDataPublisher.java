package org.esp.publisher;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.esp.domain.publisher.ColourMap;

/**
 * Interface for spatial data file publisher modules.
 * Each implementation will be able to publish a different type of spatial data
 * (e.g. vector, raster, etc.)
 * 
 * @author mauro.bartolomeoli@geo-solutions.it
 *
 */
public interface SpatialDataPublisher {
    /**
     * Extract metadata from the given spatial data file.
     * 
     * @param file file to extract the metadata from.
     * @param layerName name to be assigned to the final published layer.
     * 
     * @return
     * @throws PublishException
     * @throws UnknownCRSException
     */
    public PublishedFileMetadata extractMetadata(File file, String layerName)
            throws PublishException, UnknownCRSException;
    
    /**
     * Creates a new style for the layer to be published.
     *  
     * @param metadata layer metadata
     * @param layerName name to be assigned to the layer
     * @param styleTemplate freemarker template to be used to create the style
     * @param colourMap map of colours to be used in the style
     * 
     * @return
     */
    public String createStyle(PublishedFileMetadata metadata, String layerName,
            String styleTemplate, ColourMap colourMap) throws PublishException;
    
    /**
     * Creates and publish the layer.
     * 
     * @param layerName
     * @param styleName
     * @param metadata
     * @return
     */
    public boolean createLayer(String layerName, String styleName, PublishedFileMetadata metadata)
            throws PublishException;

    /**
     * Updates an existing style using full SLD.
     * 
     * @param layerName
     * @param style
     */
    public boolean updateStyle(String layerName, String style) throws PublishException;
    
    /**
     * Updates an existing style using precompiled rules.
     * 
     * @param layerName
     * @param string
     * @param rules
     */
    public boolean updateStyle(String layerName, String styleTemplate, String rules, StylingMetadata metadata) throws PublishException;
    
    /**
     * Updates an existing style using the given colourmap.
     * 
     * @param layerName
     * @param string
     * @param colourMap
     */
    public boolean updateStyle(String layerName, String styleTemplate, StylingMetadata metadata) throws PublishException;

    /**
     * Returns the list of supported attributes for the given layerName,
     * mapped to their types.
     * 
     * @param layerName
     * @return
     * @throws PublishException 
     */
    public Map<String, Class<?>> getAttributes(String layerName) throws PublishException;

    /**
     * Checks if the publisher supports ad hoc styling (to be applied only on 
     * already published data).
     * 
     * @return
     */
    public boolean supportsAdHocStyling();
    
    /**
     * Returns a default style template name
     * for this kind of spatial data.
     * 
     * @return
     */
    public String getDefaultStyleTemplate();

    /**
     * Returns an identifier for the publisher.
     * Binds the publisher to persisted metadata.
     * 
     * @return
     */
    public Integer getId();
    
    /**
     * Configures the global Geoserver REST Api handler.
     * 
     * @param gsr
     */
    public void setGeoserverHandler(GeoserverRestApi gsr);

    /**
     * Unpublish the given layer from Geoserver.
     * 
     * @param layerName
     * @throws PublishException 
     */
    public void unpublish(String layerName) throws PublishException;

    /**
     * Gets a published style from GeoServer.
     * 
     * @param styleName
     * @return
     */
    public String getPublishedStyle(String styleName);

    /**
     * Gets a published layer attribute information (name, type, etc.) from GeoServer.
     * 
     * @param layerName
     * @return
     * @throws PublishException 
     */
    public String getAttributesInfo(String layerName) throws PublishException;

    /**
     * Gets a published layer geometry type from GeoServer.
     * 
     * @param layerName
     * @return
     * @throws PublishException 
     */
    public String getGeometryType(String layerName) throws PublishException;

    /**
     * Configure Publisher limits.
     * 
     * @param limits
     */
    public void setLimits(Map<String, Integer> limits);
    
    
    /**
     * Returns a classified style with the given classification description.
     * 
     * @param layerName
     * @param attributeName
     * @param classificationMethod
     * @param intervalsNumber
     * @param colourMap
     * @return
     * @throws PublishException
     */
    public String classify(String layerName, String attributeName,
            String classificationMethod, int intervalsNumber, ColourMap colourMap)
            throws PublishException;

	public void cleanMetadata(PublishedFileMetadata metadata);
}
