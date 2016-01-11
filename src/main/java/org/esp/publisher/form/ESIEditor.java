package org.esp.publisher.form;

import it.jrc.auth.RoleManager;
import it.jrc.domain.adminunits.Grouping;
import it.jrc.domain.adminunits.Grouping_;
import it.jrc.form.ButtonFactory;
import it.jrc.form.FieldGroup;
import it.jrc.form.component.FormConstants;
import it.jrc.form.component.YearField;
import it.jrc.form.controller.EditorController;
import it.jrc.form.editor.SubmitPanel;
import it.jrc.form.view.DefaultEditorView;
import it.jrc.form.view.IEditorView;
import it.jrc.persist.Dao;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.commons.lang3.time.DateUtils;
import org.esp.domain.blueprint.ArealUnit_;
import org.esp.domain.blueprint.DataSource;
import org.esp.domain.blueprint.DataSource_;
import org.esp.domain.blueprint.EcosystemService;
import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.EcosystemServiceIndicator_;
import org.esp.domain.blueprint.Indicator;
import org.esp.domain.blueprint.Indicator_;
import org.esp.domain.blueprint.PublishStatus;
import org.esp.domain.blueprint.QuantificationUnit_;
import org.esp.domain.blueprint.SpatialDataType;
import org.esp.domain.blueprint.Status;
import org.esp.domain.blueprint.Study;
import org.esp.domain.blueprint.TemporalUnit_;
import org.esp.domain.publisher.ColourMap;
import org.esp.publisher.ESPClientUploadField;
import org.esp.publisher.GeoTiffMetadata;
import org.esp.publisher.GeoserverRestApi;
import org.esp.publisher.PublishException;
import org.esp.publisher.PublishedFileMetadata;
import org.esp.publisher.SpatialDataPublisher;
import org.esp.publisher.SpatialDataPublishers;
import org.esp.publisher.StylingMetadata;
import org.esp.publisher.UnknownCRSException;
import org.esp.publisher.colours.ColourMapFieldGroup.ColourMapAttributeChangeListener;
import org.esp.publisher.styler.StylerFieldGroup;
import org.esp.publisher.styler.StylerFieldGroup.StyleChangeListener;
import org.esp.publisher.ui.ViewModule;
import org.esp.server.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addon.leaflet.LMap;
import org.vaadin.addon.leaflet.util.CRSTranslator;
import org.vaadin.dialogs.ConfirmDialog;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.UI;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class ESIEditor extends EditorController<EcosystemServiceIndicator> {

    public static final String THE_ECOSYSTEM_SERVICE = "The Ecosystem Service";
    public static final String SPATIAL_DATA_TYPE = "Spatial Data Type";
    public static final String SPATIAL_DATA = "Geospatial data";
    public static final String LAY_OUT = "Lay-out";
    private static final Long TEMPORARY_STATUS = PublishStatus.TEMPORARY.getValue();
    private static final Long NOT_VALIDATED_STATUS = PublishStatus.NOT_VALIDATED.getValue();

    private Logger logger = LoggerFactory.getLogger(ESIEditor.class);

    private PolygonField envelopeField;

    private StylerFieldGroup stylerFieldGroup;
    
    private ComboBox spatialDataTypeField;

    private GeoserverRestApi gsr;

    private LayerPublishedListener listener;

    private TextField sridField;

    private ESIEditorView esiEditorView;

    private TextArea spatialReferenceInfoField;

    private RoleManager roleManager;
    
    private ESPClientUploadField uploadField;
    
    private Map<Long, Map<String, Integer>> limits = new HashMap<Long, Map<String, Integer>>();
    private EditableCombo<EcosystemService> ecosystemServiceField;
    private Field<Indicator> indicatorField;
    private EditableCombo<Study> studyField;
    private Indicator dummyIndicator;
    private EcosystemService dummyEcosystemService;
    private Study dummyStudy;
    private FileService fileService;
    
    @Inject
    public ESIEditor(Dao dao, RoleManager roleManager,
            GeoserverRestApi gsr,
            FileService fileService,
            @Named("geotiff_limit_mb") int geotiffSizeLimit,
            @Named("shapefile_limit_mb") int shapefileSizeLimit,
            @Named("shapefile_limit_records") int shapefileRecordsLimit) {

        super(EcosystemServiceIndicator.class, dao);
        
        dummyIndicator = dao.find(Indicator.class, 0l);
        dummyEcosystemService = dao.find(EcosystemService.class, 0l);
        dummyStudy = dao.find(Study.class, 0l);
        
        this.fileService = fileService;
        this.gsr = gsr;
        SpatialDataPublishers.setGeoserverHandler(gsr);
        this.roleManager = roleManager;

        
        Map<String, Integer> rasterLimits = new HashMap<String, Integer>();
        rasterLimits.put("size", geotiffSizeLimit);
        limits.put(1l, rasterLimits);
        
        Map<String, Integer> vectorLimits = new HashMap<String, Integer>();
        vectorLimits.put("size", shapefileSizeLimit);
        vectorLimits.put("records", shapefileRecordsLimit);
        limits.put(2l, vectorLimits);
        
        SpatialDataPublishers.setLimits(limits);
        
        buildPublishForm();
        buildMetaForm();
        
        /*
         * Clear temporary data older than yesterday
         */
        try{
            dao.getEntityManager().getTransaction().begin();
            
            /*
             * Unpublish layers and styles
             */
            TypedQuery<EcosystemServiceIndicator> tq = dao.getEntityManager().createQuery("SELECT es FROM EcosystemServiceIndicator es WHERE es.status.id = :temporary AND es.dateCreated < :yesterday",EcosystemServiceIndicator.class);
            tq.setParameter("temporary", TEMPORARY_STATUS);
            tq.setParameter("yesterday", DateUtils.addDays(new Date(), -1));
            List<EcosystemServiceIndicator> results = tq.getResultList();
            for(EcosystemServiceIndicator ent: results){
                if(ent.getLayerName() != null && !ent.getLayerName().isEmpty()){
                    unpublishEntity(ent);
                }
            }  
            /*
             * Delete entities
             */
            Query q = dao.getEntityManager().createQuery("DELETE FROM EcosystemServiceIndicator es WHERE es.status.id = :temporary AND es.dateCreated < :yesterday");
            q.setParameter("temporary", TEMPORARY_STATUS);
            q.setParameter("yesterday", DateUtils.addDays(new Date(), -1));
            q.executeUpdate();
            dao.getEntityManager().getTransaction().commit();
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            dao.getEntityManager().getTransaction().rollback();
        }
    }

    @Override
    public void init(IEditorView<EcosystemServiceIndicator> view) {
        this.setContent(view);

        esiEditorView = (ESIEditorView) view;

        List<FieldGroup<EcosystemServiceIndicator>> fieldGroups = getFieldGroups();
        view.buildForm(fieldGroups);

        buildSubmitPanel(view.getTopSubmitPanel());
        buildSubmitPanel(view.getBottomSubmitPanel());
    }
    
    @Override
    public void doUpdate(EcosystemServiceIndicator entity) {
        if (!roleManager.isOwner(entity) && !roleManager.getRole().getIsSuperUser()) {
            Notification.show("You do not have permission to edit this.", Type.ERROR_MESSAGE);
            UI.getCurrent().getNavigator().navigateTo(ViewModule.HOME);
            return;
        }
        /*
         * Temporary is not available for edit
         */
        if(entity.getStatus().getId() == PublishStatus.TEMPORARY.getValue()){
            Notification.show("No longer available.", Type.HUMANIZED_MESSAGE);
            UI.getCurrent().getNavigator().navigateTo(ViewModule.HOME);
            return;
        }
        super.doUpdate(entity);
        esiEditorView.setNewStatus(false);
        SpatialDataPublisher filePublisher = getFilePublisher();
        Map<String, Class<?>> attributes;
        try {
            String layerName = entity.getLayerName();
            if(layerName != null) {
                attributes = filePublisher.getAttributes(layerName);
                String attributesInfo = filePublisher.getAttributesInfo(layerName);
                String symbolType = filePublisher.getGeometryType(layerName);
                stylerFieldGroup.startUpdate();
                updateUIStyle(layerName, attributesInfo, symbolType);
                stylerFieldGroup.initStyler(attributes, entity.getAttributeName());
                stylerFieldGroup.endUpdate();
            } else {
                stylerFieldGroup.updateUI(entity);
            }
        } catch (PublishException e) {
            Notification.show("Error getting attributes for the layer: " + e.getMessage(), Type.ERROR_MESSAGE);
        }
        
        uploadField.updateSpatialDataType(entity.getSpatialDataType());
        
        //stylerFieldGroup.enableUpdateStyle(true);
    }

    private SpatialDataPublisher getFilePublisher(Long id) {
        return SpatialDataPublishers.getSpatialDataPublisher(id.intValue());
    }

    @Override
    public void doCreate() {

        /*
         * Clear form
         */
        esiEditorView.setNewStatus(true);
        super.doCreate();
        
        EcosystemServiceIndicator entity = getEntity();
        
        entity.setStatus(dao.find(Status.class, TEMPORARY_STATUS));
        
        entity.setIntervalsNumber(StylerFieldGroup.DEFAULT_N_INTERVALS);
        
        stylerFieldGroup.setDefaultValues();
    }
    
    @Override
    protected void doPreCommit(EcosystemServiceIndicator entity) {
        entity.setRole(roleManager.getRole());
        entity.setDateUpdated(Calendar.getInstance().getTime());
        
        stylerFieldGroup.preCommit(entity);
        
        
    }

    @Override
    protected void doPostCommit(EcosystemServiceIndicator entity) {
        esiEditorView.setNewStatus(false);
        uploadField.updateSpatialDataType(entity.getSpatialDataType());
        stylerFieldGroup.updateUI(entity);
    }

    private void buildPublishForm() {

        /*
         * The service
         */
        ecosystemServiceField = new EditableCombo<EcosystemService>(
                EcosystemService.class, dao, "SELECT es FROM EcosystemService es WHERE es.id > 0");

        ff.addField(EcosystemServiceIndicator_.ecosystemService, ecosystemServiceField);
        /*
         * The indicator
         */
        indicatorField = getFieldWithPopup("SELECT i FROM Indicator i WHERE i.id > 0",
                EcosystemServiceIndicator_.indicator, Indicator_.label);

        /*
         * The study
         */
        studyField = new EditableCombo<Study>(
                Study.class, dao, "SELECT s FROM Study s WHERE s.id > 0");
        InlineStudyEditor studyEditor = new InlineStudyEditor(dao, roleManager);
        studyField.setEditor(studyEditor);
        studyEditor.init(new DefaultEditorView<Study>());
        ff.addField(EcosystemServiceIndicator_.study, studyField);
        
        spatialDataTypeField = (ComboBox) ff.addField(EcosystemServiceIndicator_.spatialDataType);
        spatialDataTypeField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                Object value = event.getProperty().getValue();
                getEntity().setSpatialDataType(value != null ? (SpatialDataType)value : null);
                uploadField.updateSpatialDataType(getEntity().getSpatialDataType());
                esiEditorView.setNewStatus(value == null);
            }
        });
        spatialDataTypeField.setImmediate(true);

        addFieldGroup(THE_ECOSYSTEM_SERVICE);

        /*
         * Uploading
         */
        uploadField = new ESPClientUploadField(limits);

        uploadField.addListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                File f = (File) event.getProperty().getValue();
                if(f == null) {
                    showError("File has not been correctly upload.");
                } else if(uploadField.checkLimits(f)) {

//                boolean success = ESIEditor.this.isFormValid();
//                if (!success) {
//                    Notification.show("Please ensure the form is valid before continuing.");
//                    return;
//                }
//
//                Preconditions.checkArgument(f != null, "Where is the file?");
                    publishFile(f);
                }
            }

        });

        ff.addField("file", uploadField);

        stylerFieldGroup = new StylerFieldGroup(LAY_OUT,dao);

        
        stylerFieldGroup.setUpdateStyleListener(new StyleChangeListener() {
            
            @Override
            public void onValueChanged(ColourMap colourMap, String attributeName,
                    String classificationMethod, int intervalsNumber, String SLD) {
                updateStyle(stylerFieldGroup);
                
            }

            @Override
            public void onClassify(ColourMap colourMap, String attributeName,
                    String classificationMethod, int intervalsNumber) {
                try {
                    SpatialDataPublisher filePublisher = getFilePublisher();
                    String layerName = getLayerName();
                    String sld = normalizeSLD(filePublisher.classify(layerName, attributeName, classificationMethod, intervalsNumber, colourMap));
                    stylerFieldGroup.updateStyle(layerName, sld,
                            filePublisher.getGeometryType(layerName),
                            filePublisher.getAttributesInfo(layerName), true);
                } catch (PublishException e) {
                    showError("Error get classified style: " + e.getMessage());
                }
                
            }

            @Override
            public void onUpdate(StylingMetadata style) {
                updateStyle(style);
            }
        });
        
       

        stylerFieldGroup.setAttributeListener(new ColourMapAttributeChangeListener() {
            
            @Override
            public void onValueChanged(String attributeName, Class<?> attributeType) {
                if(getLayerName() != null) {
                    if(!updateExtrema(getLayerName(), attributeName, attributeType)) {
                        showError("Unable to update min - max values");
                    }
                    
                }
            }
        });


        this.sridField = ff.addTextField(EcosystemServiceIndicator_.srid);
        sridField.setVisible(false);
        sridField.setConverter(new StringToIntegerConverter());

        spatialReferenceInfoField = ff
                .addTextArea(EcosystemServiceIndicator_.spatialReferenceInfo);
        spatialReferenceInfoField.setReadOnly(true);

        getGeometryField();

        // Invisible form field
        ff.addField(EcosystemServiceIndicator_.envelope, envelopeField);
        envelopeField.setVisible(false);

        addFieldGroup(SPATIAL_DATA);
        
        // Add to the field groups
        getFieldGroups().add(stylerFieldGroup);
    }
    
    /**
     * Creates a complete SLD from the classify output.
     * 
     * @param classify
     * @return
     */
    protected String normalizeSLD(String sld) {
        String layerName = getLayerName(); 
        String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        prefix += "<sld:StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" version=\"1.0.0\">";
        prefix += "<sld:NamedLayer><sld:Name>" + layerName + "</sld:Name><sld:UserStyle><sld:Name>" + layerName + "</sld:Name><sld:Title>" + layerName + "</sld:Title>";
        prefix += "<sld:Abstract>" + layerName + "</sld:Abstract><sld:FeatureTypeStyle><sld:Name>name</sld:Name>";
        
        String postfix = "</sld:FeatureTypeStyle></sld:UserStyle></sld:NamedLayer></sld:StyledLayerDescriptor>";
        return prefix + sld
                .replace("\n", "")
                .replace("\r", "") 
                .replace("<Filter>", "<ogc:Filter>")
                .replace("</Filter>", "</ogc:Filter>")
                .replace("<Literal>", "<ogc:Literal>")
                .replace("</Literal>", "</ogc:Literal>")
                .replace("<PropertyName>", "<ogc:PropertyName>")
                .replace("</PropertyName>", "</ogc:PropertyName>")
                .replace("<PropertyIs", "<ogc:PropertyIs")
                .replace("</PropertyIs", "</ogc:PropertyIs")
                .replace("<And>", "<ogc:And>")
                .replace("</And>", "</ogc:And>")
                + postfix;
    }

    /**
     * Updates a style definition with the given style metadata.
     * 
     * @param styleName
     * @param metadata
     * @return
     * @throws PublishException
     */
    private boolean updateStyle(String styleName, StylingMetadata metadata) throws PublishException {
        SpatialDataPublisher filePublisher = getFilePublisher();
        return filePublisher.updateStyle(styleName, filePublisher.getDefaultStyleTemplate(), metadata);
    }
    
    /**
     * Updates a style definition with the given style metadata.
     * 
     * @param styleName
     * @param metadata
     * @return
     * @throws PublishException
     */
    private void updateUIStyle(String styleName, String attributes, String symbolType) throws PublishException {
        SpatialDataPublisher filePublisher = getFilePublisher();
        String style = filePublisher.getPublishedStyle(styleName);
        
        if(style != null) {
            stylerFieldGroup.updateStyle(styleName, style, symbolType, attributes, false);
        }
    }

    /**
     * Returns the appropriate SpatialDataPublisher for the current entity.
     * 
     * @return
     */
    private SpatialDataPublisher getFilePublisher() {
        return getFilePublisher(getEntity().getSpatialDataType().getId());
    }

    private void buildMetaForm() {

        ff.addField(EcosystemServiceIndicator_.ecosystemServiceAccountingType);
        ff.addField(EcosystemServiceIndicator_.ecosystemServiceBenefitType);

        addFieldGroup("Accounting");


        getFieldWithPopup(
                EcosystemServiceIndicator_.quantificationUnit,
                QuantificationUnit_.label,
                QuantificationUnit_.quantificationUnitCategory);

        getFieldWithPopup(EcosystemServiceIndicator_.arealUnit,
                ArealUnit_.label);
        getFieldWithPopup(EcosystemServiceIndicator_.temporalUnit,
                TemporalUnit_.label);

        addFieldGroup("Quantification");

        ff.addField(EcosystemServiceIndicator_.startYear, new YearField());
        ff.addField(EcosystemServiceIndicator_.endYear, new YearField());
        ff.addField(EcosystemServiceIndicator_.spatialLevel);

        // Quick hack to get a filtered TwinColSelect
        TwinColSelect groupingField = new TwinColSelect();
        groupingField.setWidth(FormConstants.LARGE_FIELD_DEFAULT_WIDTH);
        ff.addField(EcosystemServiceIndicator_.regions, groupingField);
        List<Grouping> groupings = dao.all(Grouping.class, Grouping_.id);
        for (Grouping grouping : groupings) {
            if (grouping.getGroupingType().getId().equals("Region")) {
                groupingField.addItem(grouping);
            }
        }

        groupingField.setCaption("Regions");

        addFieldGroup("Spatio-temporal");

        ff.addField(EcosystemServiceIndicator_.quantificationMethod);

        {
            EditableTwinColSelect<DataSource> dataSourceField = new EditableTwinColSelect<DataSource>(DataSource.class, dao);
            dataSourceField.setEditor(new EditorController<DataSource>(DataSource.class, dao) {
                {
                    ff.addField(DataSource_.name);
                    ff.addField(DataSource_.url);
                    addFieldGroup("");
                    init(new DefaultEditorView<DataSource>());
                }
            });
            ff.addField(EcosystemServiceIndicator_.dataSources, dataSourceField);
        }

        addFieldGroup("Model and data");

        ff.addField(EcosystemServiceIndicator_.minimumMappingUnit);

        addFieldGroup("Spatial data");

        ff.addField(EcosystemServiceIndicator_.biomes);

        ff.addField(EcosystemServiceIndicator_.studyObjectiveMet);
        ff.addTextArea(EcosystemServiceIndicator_.comments);

        addFieldGroup("Other");

    }
    
    public <X> Field<X> getFieldWithPopup(Attribute<? extends EcosystemServiceIndicator, X> prop,
            final SingularAttribute<X, ?>... childProps) {
        return getFieldWithPopup("", prop, childProps);
    }
    
    public <X> Field<X> getFieldWithPopup(String query,
            Attribute<? extends EcosystemServiceIndicator, X> prop,
            final SingularAttribute<X, ?>... childProps) {

        EditableField<X> c = new EditableCombo<X>(prop.getJavaType(), dao, query);
        c.setEditor(new EditorController<X>(prop.getJavaType(), dao) {
            {
                for (int i = 0; i < childProps.length; i++) {
                    ff.addField(childProps[i]);
                }
                addFieldGroup("");
                init(new DefaultEditorView<X>());
            }
        });
        
        ff.addField(prop, c);
        return c;
    }

    /**
     * TODO all JTS Field stuff should be refactored
     */
    private void getGeometryField() {

        envelopeField = new PolygonField();

        envelopeField.setConverter(new Converter<LinearRing, Polygon>() {

            @Override
            public Polygon convertToModel(LinearRing value,
                    Class<? extends Polygon> targetType, Locale locale)
                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                if (value != null) {

                    Polygon polygon = new Polygon(value, null,
                            new GeometryFactory());
                    polygon.setSRID(value.getSRID());
                    return polygon;
                }
                return null;
            }

            @Override
            public LinearRing convertToPresentation(Polygon value,
                    Class<? extends LinearRing> targetType, Locale locale)
                    throws com.vaadin.data.util.converter.Converter.ConversionException {

                // It's guaranteed to be a LinearRing
                if (value != null) {
                    LinearRing exteriorRing = (LinearRing) value
                            .getExteriorRing();
                    return exteriorRing;
                }
                return null;
            }

            @Override
            public Class<Polygon> getModelType() {
                return Polygon.class;
            }

            @Override
            public Class<LinearRing> getPresentationType() {
                return LinearRing.class;
            }
        });

        envelopeField.setCRSTranslator(new CRSTranslator<Geometry>() {

            @Override
            public Geometry toPresentation(Geometry geom) {
                geom.setSRID(4326);
                return geom;
            }

            @Override
            public Geometry toModel(Geometry geom) {
                geom.setSRID(4326);
                return geom;
            }
        });

    }

    /**
     * Access the hidden map within the envelope field.
     * 
     * @return
     */
    public LMap getMap() {
        return envelopeField.getMap();
    }

    public String getLayerName() {
        if (getEntity() == null) {
            return null;
        }
        return getEntity().getLayerName();
    }

    /**
     * Updates the entity layer name.
     * 
     * @param layerName
     */
    public void setLayerName(String layerName) {
        if (getEntity() == null) {
            return;
        }
        getEntity().setLayerName(layerName);
    }

    
    /**
     * Publish an uploaded file.
     * 
     * @param f
     */
    private void publishFile(File f) {
        SpatialDataType spatialDataType = (SpatialDataType)spatialDataTypeField.getValue();
        
        String layerName = getLayerName();
        // new file
        if(layerName == null) {
            layerName = generateLayerName();
        }
        try {
            publishFile(f, layerName, spatialDataType);
        } catch (UnknownCRSException e) {
            showError("Error extracting  CRS: " + e.getMessage());
        } catch (PublishException e) {
            showError("Publishing error: " + e.getMessage());
        } catch (IOException e) {
            showError("Error creating uploaded file copy: " + e.getMessage());
        }
    }
    
    /**
     * Publish a new uploaded file of a specific data type with the given layer name.
     * 
     * @param f
     * @param layerName
     * @param spatialDataType
     * @throws UnknownCRSException
     * @throws PublishException
     * @throws IOException 
     */
    private void publishFile(File f, String layerName, SpatialDataType spatialDataType) throws UnknownCRSException, PublishException, IOException {
        Long spatialDataTypeId = spatialDataType.getId();
        // we use a different publisher for each kind of data type
        // to take into account a different publishing workflow
        // for each of them
        SpatialDataPublisher filePublisher = getFilePublisher(spatialDataTypeId);
        
        // extracts basic metadata from the uploaded file to store them in the EPS-Client database
        PublishedFileMetadata metadata = filePublisher.extractMetadata(f ,layerName);
        stylerFieldGroup.startUpdate();
        // update UI with the extracted metadata (min - max, srs, etc.)
        updateUIAfterPublish(metadata);
        try {
            if (getLayerName() == null) {
                // new layer, set the chosen layer name
                setLayerName(layerName);
            } else {
                // remove the old published data, before replacing it with the new one
                unpublishEntity(getEntity());
            }
            // create a basic style for the layer
            if(metadata instanceof GeoTiffMetadata){
                GeoTiffMetadata gtm = (GeoTiffMetadata) metadata;
                getEntity().setPixelSizeX(gtm.getPixelSizeX());
                getEntity().setPixelSizeY(gtm.getPixelSizeY());
            }
           
            
            String styleName = filePublisher.createStyle(metadata, layerName,
                    filePublisher.getDefaultStyleTemplate(), stylerFieldGroup.getDefaultColourMap());
            
            if(styleName != null) {
                logger.info("Basic style created: " + styleName);
                // publish the new layer
                if(filePublisher.createLayer(layerName, styleName, metadata)) {
                    logger.info("Layer created: " + layerName);
                    
                    // some data types require the layer to be published before
                    // we can create the final style for it
                    if(filePublisher.supportsAdHocStyling()) {
                        if(updateStyle(layerName, stylerFieldGroup.getBasicStyle())) {
                            logger.info("Ad hoc style created");
                        } else {
                            throw new PublishException("Error creating ad hoc style");
                        }
                    }
                    String attributesInfo = filePublisher.getAttributesInfo(layerName);
                    String symbolType = filePublisher.getGeometryType(layerName);
                    updateUIStyle(styleName, attributesInfo, symbolType);
                    commitWithoutValidation("File saved");
                    //Persist source file
                    fileService.uploadFile(getEntity().getId(),layerName,f,spatialDataType);
                    
                    firePublishEvent();
                } else {
                    // TODO: rollback
                    throw new PublishException("Error publishing layer");
                }
            } else {
                // TODO: rollback
                throw new PublishException("Error creating style");
            }
        } finally {
            stylerFieldGroup.endUpdate();
        }
    }

    /**
     * Updates the UI after a new layer has been updloaded and published.
     * Metadata extracted from the file is shown to the user.
     * 
     * @param metadata
     */
    private void updateUIAfterPublish(PublishedFileMetadata metadata) {
        Double minVal = metadata.getMinVal();

        // A bit hacky but there's no good way to determine real no-data
        // values.
        if (minVal < 0) {
            stylerFieldGroup.setMinValue("0");
        } else {
            stylerFieldGroup.setMinValue(metadata.getMinVal().toString());
        }
        stylerFieldGroup.setMaxValue(metadata.getMaxVal().toString());

        sridField.setValue(metadata.getSrid().toString());
        stylerFieldGroup.setDefaultColorMap();
        spatialReferenceInfoField.setValue(metadata
                .getDescription());

        envelopeField.setValue((LinearRing) metadata.getEnvelope().getBoundary());
        stylerFieldGroup.initStyler(metadata.getAttributes(), metadata.getAttributeName());
    }

    private void showError(String message) {
        Notification.show(message, Type.ERROR_MESSAGE);
    }

    @Override
    protected void doPostDelete(EcosystemServiceIndicator entity) {
        try {
            unpublishEntity(entity);
            UI.getCurrent().getNavigator().navigateTo(ViewModule.HOME);
            super.doPostDelete(entity);
        } catch (PublishException e) {
            showError("Error during delete: " + e.getMessage());
        } catch (IOException e) {
            showError("Error during delete uploaded file: " + e.getMessage());
        }
    }

    /**
     * Removes all published objects related to an entity: layer, store, style, db table.
     * 
     * @param entity
     * @throws PublishException
     * @throws IOException 
     */
    private void unpublishEntity(EcosystemServiceIndicator entity) throws PublishException, IOException {
        getFilePublisher(entity.getSpatialDataType().getId()).unpublish(entity.getLayerName());
        fileService.deleteFile(entity.getId(),entity.getLayerName());
    }
    
    private String generateLayerName() {
        return "esp-layer-"
                + dao.getNextValueInSequence("blueprint.geoserver_layer");
    }

    public void setPublishEventListener(LayerPublishedListener listener) {
        this.listener = listener;
    }

    /**
     * Publish event
     */
    private void firePublishEvent() {
        if (listener == null) {
            logger.error("Null listener");
        } else {
            listener.onLayerPublished(getLayerName(), getEntity().getEnvelope(), getEntity().getTimestamp());
        }
    }

    private boolean updateExtrema(String layerName, String attributeName, Class<?> attributeType) {
        if(attributeType != null && Number.class.isAssignableFrom(attributeType)) {
            double[] extrema = gsr.getExtrema(layerName, attributeName);
            if(extrema != null) {
                Double minVal = extrema[0];
                Double maxVal = extrema[1];
                if (minVal < 0) {
                    stylerFieldGroup.setMinValue("0");
                } else {
                    stylerFieldGroup.setMinValue(minVal.toString());
                }
                stylerFieldGroup.setMaxValue(maxVal.toString());
                return true;
            } else {
                return false;
            }
        } else {
            stylerFieldGroup.setMinValue("0");
            stylerFieldGroup.setMaxValue("1");
            return true;
        }
    }
    
    private void updateStyle(StylingMetadata metadata) {
        String layerName = getLayerName();
        String attributeName = metadata.getAttributeName();
        Class<?> attributeType = metadata.getAttributeType();
        String sld = metadata.getSLD();
        if (layerName != null) {
            try {
                
                if(attributeName != null && sld == null) {
                    if(!updateExtrema(layerName, attributeName, attributeType)) {
                        showError("Cannot retrieve min - max values for the layer");
                        return;
                    }
                }
                if(updateStyle(layerName, metadata)) {
                    updateUIStyle(layerName, null, null);
                    commitWithoutValidation("Style updated");
                    firePublishEvent();
                } else {
                 // TODO: rollback
                    throw new PublishException("Error updating style");
                }
            } catch (PublishException e) {
                showError("Error updating style: " + e.getMessage());
            }
            
        }
    }
    
    private void commitWithoutValidation(String message) throws PublishException{
        /*
         * Disable validation and store dummy values for empty fields
         */
        if(indicatorField.getValue() == null){
            indicatorField.getPropertyDataSource().setValue(dummyIndicator);
            indicatorField.setInvalidCommitted(true);
        }
        if(ecosystemServiceField.getValue() == null){
            ecosystemServiceField.getPropertyDataSource().setValue(dummyEcosystemService);
            ecosystemServiceField.setInvalidCommitted(true);
        }
        if(studyField.getValue() == null){
            studyField.getPropertyDataSource().setValue(dummyStudy);
            studyField.setInvalidCommitted(true);
        }
        boolean com = commitForm(false);
        /*
         * Override dummy values with empty
         */
        if(indicatorField.getValue().equals(dummyIndicator)){
            indicatorField.getPropertyDataSource().setValue(null);
        }
        if(ecosystemServiceField.getValue().equals(dummyEcosystemService)){
            ecosystemServiceField.getPropertyDataSource().setValue(null);
        }
        if(studyField.getValue().equals(dummyStudy)){
            studyField.getPropertyDataSource().setValue(null);
        }
        
        /*
         * Enable validation
         */
        ecosystemServiceField.setInvalidCommitted(false);
        indicatorField.setInvalidCommitted(false);
        studyField.setInvalidCommitted(false);
        
        if(com){
            // enable the Update Style button
            //stylerFieldGroup.enableUpdateStyle(true);
            esiEditorView.showStyler();
            logger.info(message);
        }else {
            // TODO: rollback
            throw new PublishException("Error saving published layer metadata");
        }
    }
    
    @Override
    protected void buildSubmitPanel(SubmitPanel submitPanel) {
        /*
         * Submit panel
         */
        Button commit = ButtonFactory.getButton(
                ButtonFactory.SAVE_BUTTON_CAPTION, ButtonFactory.SAVE_ICON);
        commit.setEnabled(containerManager.canUpdate());

        commit.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                /*
                 * Change form TEMPORARY to NOT VALIDATED
                 */
                Status preStatus = getEntity().getStatus();
                getEntity().setStatus(dao.find(Status.class, NOT_VALIDATED_STATUS));
                if(!commitForm(true)){
                    getEntity().setStatus(preStatus);
                }
            }
        });

        Button delete = ButtonFactory.getButton(
                ButtonFactory.DELETE_BUTTON_CAPTION, ButtonFactory.DELETE_ICON);
        delete.setEnabled(containerManager.canDelete());

        delete.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {

                ConfirmDialog.show(UI.getCurrent(),
                        "Are you sure you wish to delete this record?",
                        new ConfirmDialog.Listener() {

                            public void onClose(ConfirmDialog dialog) {
                                if (dialog.isConfirmed()) {
                                    doDelete();

                                }
                            }

                        });
            }
        });

        submitPanel.addLeft(commit);
        submitPanel.addRight(delete);
    }
}
