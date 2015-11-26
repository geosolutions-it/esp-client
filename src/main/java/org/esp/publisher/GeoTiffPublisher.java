package org.esp.publisher;

import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.media.jai.ImageLayout;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ExtremaDescriptor;

import org.esp.domain.publisher.ColourMap;
import org.esp.publisher.utils.PublisherUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.google.common.primitives.Doubles;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * GeoTiff file publisher module.
 * 
 * @author mauro.bartolomeoli@geo-solutions.it
 *
 */
public class GeoTiffPublisher extends AbstractFilePublisher {


    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    public GeoTiffPublisher() {
        super();
    }
    
   

    @Override
    protected PublishedFileMetadata createMetadata(File tifFile, String layerName)
            throws PublishException, UnknownCRSException {
        
        GeoTiffMetadata surface = new GeoTiffMetadata();;
        GeoTiffReader gtr = null;
        
        surface.setFile(tifFile);
        try {
            gtr = new GeoTiffReader(tifFile);
            CoordinateReferenceSystem crs = gtr.getCoordinateReferenceSystem();
            setCrs(surface, crs);

            /*
             * Build the envelope and set to WGS84
             */
            GeneralEnvelope origEnv = gtr.getOriginalEnvelope();
            DirectPosition ll = origEnv.getLowerCorner();
            DirectPosition ur = origEnv.getUpperCorner();

            Envelope e = new Envelope();
            e.expandToInclude(ll.getOrdinate(0), ll.getOrdinate(1));
            e.expandToInclude(ur.getOrdinate(0), ur.getOrdinate(1));
            
            ReferencedEnvelope env = new ReferencedEnvelope(e,crs);
            
            Geometry poly = PublisherUtils.envelopeToWgs84(env);

            if (poly instanceof Polygon) {
                surface.setEnvelope((Polygon) poly);
            }

            /*
             * Figure out the pixel size
             */
            ImageLayout imageLayout = gtr.getImageLayout();
            int imageWidth = imageLayout.getWidth(null);
            int imageHeight = imageLayout.getHeight(null);

            double pixelSizeX = surface.getEnvelope().getEnvelopeInternal().getWidth() / imageWidth;
            double pixelSizeY = surface.getEnvelope().getEnvelopeInternal().getHeight() / imageHeight;

            surface.setPixelSizeX(pixelSizeX);
            surface.setPixelSizeY(pixelSizeY);

            surface.setMinVal(0d);
            surface.setMaxVal(100d);

            GridCoverage2D gridCoverage2D = gtr.read(null);

            try {
                int nDims = gridCoverage2D.getNumSampleDimensions();
                surface.setNumSampleDimensions(nDims);
                // calculate min and max from raster data
                extremaOp(surface, gridCoverage2D);

            } finally {
                gridCoverage2D.dispose(false);
            }

        } catch (DataSourceException e) {
            throw new PublishException("Error opening tif file: " + e.getMessage(), e);
        } catch (FactoryException e) {
            throw new PublishException("Error reading crs from tif file: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PublishException("Error reading tif file: " + e.getMessage(), e);
        } catch (TransformException e) {
            throw new PublishException("Error transforming bbox to WGS84: " + e.getMessage(), e);
        } finally {
            if(gtr != null){
                gtr.dispose();
            }
        }
        return surface;
    }

    private void extremaOp(GeoTiffMetadata surface, GridCoverage2D gridCoverage2D) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        RenderedImage img = gridCoverage2D.getRenderedImage();

        RenderedOp extremaOp = ExtremaDescriptor.create(img, null, 10, 10,
                false, 1, null);
        double[] allMins = (double[]) extremaOp.getProperty("minimum");
        min = Doubles.min(allMins);

        double[] allMaxs = (double[]) extremaOp.getProperty("maximum");
        max = Doubles.max(allMaxs);

        surface.setMaxVal(max);
        surface.setMinVal(min);
    }

    
    
    @Override
    public String createStyle(PublishedFileMetadata metadata, String layerName,
            String styleTemplate, ColourMap colourMap) throws PublishException {
        if (((GeoTiffMetadata)metadata).getNumSampleDimensions() == 3) {
            return "raster";
        }
        return super.createStyle(metadata, layerName, styleTemplate, colourMap);
    }

    @Override
    public boolean createLayer(String layerName, String styleName, PublishedFileMetadata metadata) throws PublishException {
        return gsr.publishTiff(metadata.getFile(), metadata.getSrid(), layerName, styleName);
    }
    
    @Override
    public String getDefaultStyleTemplate() {
        return "SldRaster.ftl";
    }
    
    @Override
    public Integer getId() {
        return 1;
    }



    @Override
    protected void removeLayer(String layerName) throws PublishException {
        if(gsr.getRasterStore(layerName) != null && !gsr.removeRasterStore(layerName)) {
            throw new PublishException("Cannot remove layer " + layerName);
        }
    }

    
}