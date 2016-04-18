package org.esp.publisher;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.esp.publisher.utils.PublisherUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Importer of shapefiles into a Postgis database.
 * 
 * @author Geo
 *
 */
public class ShapefileToPostgisImporter {
    
    private Map<String, Object> postgisConnectionParams = new HashMap<String, Object>();
    
    private Logger logger = LoggerFactory.getLogger(ShapefileToPostgisImporter.class);
    
    private String schema;
    
    @Inject
    public ShapefileToPostgisImporter(
            @Named("postgis_host") String postgisHost,
            @Named("postgis_port") String postgisPort,
            @Named("postgis_database") String postgisDatabase,
            @Named("postgis_schema") String postgisSchema,
            @Named("postgis_user") String postgisUser,
            @Named("postgis_password") String postgisPassword
            ) {
        postgisConnectionParams.put("dbtype", "postgis");
        postgisConnectionParams.put("host", postgisHost);
        postgisConnectionParams.put("port", postgisPort);
        postgisConnectionParams.put("database", postgisDatabase);
        postgisConnectionParams.put("schema", postgisSchema);
        postgisConnectionParams.put("user", postgisUser);
        postgisConnectionParams.put("passwd", postgisPassword);
        
        schema = postgisSchema;
    }

    /**
     * Imports a new shapefile (contained in the given zipFile),
     * into a table named layerName on the configured postgis connection.
     * 
     * @param zipFile
     * @param layerName
     * @param srs
     * @throws PublishException
     */
    public void importShapefile(File zipFile, String layerName, String tableName, String srs) throws PublishException {
        
        File shapeFile = PublisherUtils.uncompress(zipFile);
        DataStore sourceDataStore = null;
        DataStore destDataStore = null;

        final Transaction transaction = new DefaultTransaction("create");
        try {
            
            // source
            sourceDataStore = createSourceDataStore(shapeFile);
            Query query = buildSourceQuery(sourceDataStore, layerName, srs);
            
            FeatureSource<SimpleFeatureType, SimpleFeature> featureReader = createSourceReader(
                    sourceDataStore, transaction, query);

            SimpleFeatureType sourceSchema = featureReader.getSchema();
            FeatureSource<SimpleFeatureType, SimpleFeature> inputFeatureWriter = null;
            if(featureReader instanceof FeatureStore){
                inputFeatureWriter = createWriter(
                                sourceDataStore, sourceSchema, transaction);
            }
            // output
            destDataStore = createOutputDataStore();
            SimpleFeatureType schema = buildDestinationSchema(featureReader
                    .getSchema(), tableName, srs);

            FeatureStore<SimpleFeatureType, SimpleFeature> featureWriter = createWriter(
                    destDataStore, schema, transaction);
            SimpleFeatureType destSchema = featureWriter.getSchema();

            // check for schema case differences from input to output
            Map<String, String> schemaDiffs = compareSchemas(destSchema, schema);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(destSchema);

            featureWriter.removeFeatures(Filter.INCLUDE);

            FeatureIterator<SimpleFeature> iterator = createSourceIterator(
                    query, featureReader);
            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = buildFeature(builder,
                            iterator.next(), schemaDiffs, sourceDataStore);
                    featureWriter.addFeatures(DataUtilities
                            .collection(feature));

                }
            } finally {
                iterator.close();
            }
            
            transaction.commit();
            
        } catch (Exception ioe) {
            if (logger.isErrorEnabled()) {
                logger.error(ioe.getMessage(),ioe);
            }
            try {
                transaction.rollback();
            } catch (IOException e1) {
                final String message = "Transaction rollback unsuccessful: "
                        + e1.getLocalizedMessage();
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new PublishException(message, e1);
            }
            String cause = ioe.getCause() == null ? null : ioe.getCause().getMessage();
            String msg = "MESSAGE: " + ioe.getMessage() + " - CAUSE: " + cause;
            throw new PublishException(msg, ioe);

        } finally {
            closeResource(sourceDataStore);
            closeResource(destDataStore);
            closeResource(transaction);
            if(shapeFile != null && shapeFile.exists()) {
            	File folder = shapeFile.getParentFile();
            	try {
					FileUtils.deleteDirectory(folder);
				} catch (IOException e) {
					if (logger.isErrorEnabled()) {
		                logger.error(e.getMessage(),e);
		            }
				}
            }
        }
    }
    
    /**
     * Creates a new SimpleFeature from the source feature.
     *  
     * @param builder
     * @param sourceFeature
     * @param mappings
     * @param srcDataStore
     * @return
     */
    private SimpleFeature buildFeature(SimpleFeatureBuilder builder, SimpleFeature sourceFeature,
            Map<String, String> mappings, DataStore srcDataStore) {
        for (AttributeDescriptor ad : builder.getFeatureType().getAttributeDescriptors()) {
            String attribute = ad.getLocalName();
            builder.set(attribute, getAttributeValue(sourceFeature, attribute, mappings));
        }
        
        return builder.buildFeature(null);
    }
    
    /**
     * Gets a source attribute value, taking given attribute name mappings into account.
     * 
     * @param sourceFeature
     * @param attributeName
     * @param mappings
     * @return
     */
    private Object getAttributeValue(SimpleFeature sourceFeature, String attributeName,
            Map<String, String> mappings) {
        if (mappings.containsKey(attributeName)) {
            attributeName = mappings.get(attributeName);
        }
        return sourceFeature.getAttribute(attributeName);
    }
    
    /**
     * Creates a FeatureIterator on the source reader.
     * 
     * @param query
     * @param featureReader
     * @return
     * @throws IOException
     */
    private FeatureIterator<SimpleFeature> createSourceIterator(Query query,
            FeatureSource<SimpleFeatureType, SimpleFeature> featureReader)
            throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureReader
                .getFeatures(query);
        FeatureIterator<SimpleFeature> iterator = features.features();
        return iterator;
    }
    
    /**
     * Compares source and destination schema to take into account eventual naming differences in source 
     * and destination attributes (mainly for case changing).
     * 
     * @param destSchema
     * @param schema
     * @return
     */
    private Map<String, String> compareSchemas(SimpleFeatureType destSchema, SimpleFeatureType schema) {
        Map<String, String> diffs = new HashMap<String, String>();
        for (AttributeDescriptor ad : destSchema.getAttributeDescriptors()) {
            String attribute = ad.getLocalName();
            if (schema.getDescriptor(attribute) == null) {
                for (String variant : getNameVariants(attribute)) {
                    if (schema.getDescriptor(variant) != null) {
                        diffs.put(attribute, variant);
                        break;
                    }
                }
            }
        }
        return diffs;
    }
    
    /**
     * Gets all handled variants of an attribute name.
     * 
     * @param name
     * @return
     */
    private String[] getNameVariants(String name) {
        return new String[]{name.toLowerCase(), name.toUpperCase()};
    }
    
    /**
     * Builds the destination (postgis) schema from the source (shapefile).
     * 
     * @param sourceSchema
     * @param typeName
     * @param srs
     * @return
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException
     */
    private SimpleFeatureType buildDestinationSchema(
            SimpleFeatureType sourceSchema, String typeName, String srs) throws NoSuchAuthorityCodeException, FactoryException {
        
        CoordinateReferenceSystem crs = CRS.decode(srs);
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(crs);
        builder.setName(typeName);

        for (String attributeName : buildOutputAttributes(sourceSchema)) {
            builder.add(buildSchemaAttribute(attributeName, sourceSchema, crs));
        }
        return builder.buildFeatureType();
    }
    
    /**
     * Creates a list of destination attributes from the source schema.
     * 
     * @param sourceSchema
     * @return
     */
    private Collection<String> buildOutputAttributes(SimpleFeatureType sourceSchema) {
        List<String> attributes = new ArrayList<String>();
        for (AttributeDescriptor attr : sourceSchema.getAttributeDescriptors()) {
            attributes.add(attr.getLocalName());
        }
        return attributes;
        
    }
    
    /**
     * Creates a destination attribute descriptor from the source descriptor.
     * 
     * @param attributeName
     * @param schema
     * @param crs
     * @return
     */
    private AttributeDescriptor buildSchemaAttribute(String attributeName,
            SimpleFeatureType schema, CoordinateReferenceSystem crs) {
        AttributeDescriptor attr = schema.getDescriptor(attributeName);
        
        AttributeTypeBuilder builder = new AttributeTypeBuilder();
        builder.setName(attr.getLocalName());
        builder.setBinding(attr.getType().getBinding());
        if (attr instanceof GeometryDescriptor) {
            if (crs == null) {
                crs = ((GeometryDescriptor) attr).getCoordinateReferenceSystem();
            }
            builder.setCRS(crs);
        }

        // set descriptor information
        builder.setMinOccurs(attr.getMinOccurs());
        builder.setMaxOccurs(attr.getMaxOccurs());
        builder.setNillable(attr.isNillable());

        return builder.buildDescriptor(attributeName);
    }
    
    /**
     * Creates the output (postgis) DataStore connection.
     * 
     * @return
     * @throws IOException
     */
    private DataStore createOutputDataStore() throws IOException {
        return DataStoreFinder.getDataStore(postgisConnectionParams );
    }
    
    /**
     * Creates a writer for the destination feature.
     * 
     * @param store
     * @param schema
     * @param transaction
     * @return
     * @throws IOException
     */
    private FeatureStore<SimpleFeatureType, SimpleFeature> createWriter(DataStore store,
            SimpleFeatureType schema, Transaction transaction) throws IOException {
        String destTypeName = schema.getTypeName();
        boolean createSchema = true;
        for (String typeName : store.getTypeNames()) {
            if (typeName.equalsIgnoreCase(destTypeName)) {
                createSchema = false;
                destTypeName = typeName;
            }
        }
        // check for case changing in typeName
        if (createSchema) {
            store.createSchema(schema);
            for (String typeName : store.getTypeNames()) {
                if (!typeName.equals(destTypeName) && typeName.equalsIgnoreCase(destTypeName)) {
                    destTypeName = typeName;
                }
            }
        }
        FeatureStore<SimpleFeatureType, SimpleFeature> result = (FeatureStore<SimpleFeatureType, SimpleFeature>) store.getFeatureSource(destTypeName);
        result.setTransaction(transaction);
        return result;
    }
    
    /**
     * Creates a feature reader for the shapefile.
     * 
     * @param sourceDataStore
     * @param transaction
     * @param query
     * @return
     * @throws IOException
     */
    private FeatureSource<SimpleFeatureType, SimpleFeature> createSourceReader(
            DataStore sourceDataStore, final Transaction transaction,
            Query query) throws IOException {
        FeatureSource<SimpleFeatureType, SimpleFeature> featureReader = (FeatureSource<SimpleFeatureType, SimpleFeature>) sourceDataStore.getFeatureSource(query.getTypeName());
        if(featureReader instanceof FeatureStore){
                FeatureStore<SimpleFeatureType, SimpleFeature> featureStoreReader =     (FeatureStore<SimpleFeatureType, SimpleFeature>) sourceDataStore.getFeatureSource(query.getTypeName());
                featureStoreReader.setTransaction(transaction);
                return featureReader;
        }
        return featureReader;
    }
    
    /**
     * Closes a DataStore resource.
     * 
     * @param dataStore
     */
    protected void closeResource(DataStore dataStore) {
        if (dataStore != null) {
            try {
                dataStore.dispose();
            } catch (Throwable t) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error closing datastore connection");
                }
            }
        }
    }
    
    /**
     * Closes a Statement resource.
     * 
     * @param stmt
     */
    protected void closeResource(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();;
            } catch (Throwable t) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error closing datastore connection");
                }
            }
        }
    }
    
    /**
     * Closes a Transaction resource.
     * 
     * @param transaction
     */
    protected void closeResource(Transaction transaction) {
        if (transaction != null) {
            try {
                transaction.close();
            } catch (Throwable t) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error closing transaction");
                }
            }
        }
    }
    
    /**
     * Creates the source (shapefile) DataStore connection.
     * 
     * @param shpFile
     * @return
     * @throws IOException
     */
    private DataStore createSourceDataStore(File shpFile) throws IOException {
        Map<String, Object> connectionParams = new HashMap<String, Object>();
        connectionParams.put("url", DataUtilities.fileToURL(shpFile));
        return DataStoreFinder.getDataStore(connectionParams);
    }
    
    /**
     * Creates a Query object for the source (shapefile).
     * 
     * @param sourceStore
     * @param layerName
     * @param srs
     * @return
     * @throws Exception
     */
    protected Query buildSourceQuery(DataStore sourceStore, String layerName, String srs) throws Exception {
        Query query = new Query();
        query.setTypeName(layerName);
        
        // Used to force the CRS of the source feature: it doesn't perform a reprojection, just force the CRS
        // if the configuration doesn't specify it the Crs will be read from the source feature
        query.setCoordinateSystem(CRS.decode(srs));
        
        return query;
    }

    public void removeFeature(String layerName) throws PublishException {
        final Transaction transaction = new DefaultTransaction("create");
        DataStore dataStore = null;
        Statement stmt = null;
        try {
            dataStore = createOutputDataStore();
            JDBCDataStore jdbcStore = (JDBCDataStore)dataStore;
            Connection conn = jdbcStore.getConnection(transaction);
            stmt = conn.createStatement();
            stmt.execute("DROP TABLE "+schema+".\""+layerName+"\"");
            transaction.commit();
        } catch (IOException e) {
            rollback(transaction);
            throw new PublishException("Error removing table " + layerName +" from database", e);
        } catch (SQLException e) {
            rollback(transaction);
            throw new PublishException("Error removing table " + layerName +" from database", e);
        } finally {
            closeResource(stmt);
            closeResource(dataStore);
            closeResource(transaction);
        }
    }

    private void rollback(Transaction transaction) {
        if (transaction != null) {
            try {
                transaction.rollback();;
            } catch (Throwable t) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error rolling back transaction");
                }
            }
        }
    }
}
