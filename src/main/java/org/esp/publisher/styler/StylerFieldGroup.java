package org.esp.publisher.styler;

import it.jrc.form.FieldGroup;
import it.jrc.form.component.IntegerField;
import it.jrc.persist.Dao;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.esp.domain.blueprint.Classification;
import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.EcosystemServiceIndicator_;
import org.esp.domain.publisher.ColourMap;
import org.esp.domain.publisher.ColourMapEntry;
import org.esp.publisher.PublishException;
import org.esp.publisher.StylingMetadata;
import org.esp.publisher.colours.CartographicKey;
import org.esp.publisher.colours.ColourMapEditor;
import org.esp.publisher.colours.ColourMapFieldGroup;
import org.esp.publisher.colours.ColourMapView;
import org.esp.publisher.colours.DoubleField;
import org.esp.publisher.colours.ColourMapFieldGroup.ColourMapAttributeChangeListener;
import org.esp.publisher.form.EditableCombo;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.util.converter.StringToDoubleConverter;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class StylerFieldGroup extends FieldGroup<EcosystemServiceIndicator> implements
        StylingMetadata {

    public interface StyleChangeListener {
        public void onClassify(ColourMap colourMap, String attributeName,
                String classificationMethod, int intervalsNumber);

        public void onUpdate(StylingMetadata style);

        public void onValueChanged(ColourMap colourMap, String attributeName,
                String classificationMethod, int intervalsNumber, String SLD);
    }

    class CachedStylingMetadata implements StylingMetadata {
        String attributeName;

        Class<?> attributeType;

        String classificationMethod;

        ColourMap colourMap;

        int intervalsNumber;

        String sld;

        public CachedStylingMetadata(String attributeName, Class<?> attributeType,
                String classificationMethod, ColourMap colourMap, int intervalsNumber) {
            super();
            this.attributeName = attributeName;
            this.attributeType = attributeType;
            this.classificationMethod = classificationMethod;
            this.colourMap = colourMap;
            this.intervalsNumber = intervalsNumber;
        }

        public CachedStylingMetadata(String sld) {
            super();
            this.sld = sld;
        }

        @Override
        public String getAttributeName() {
            return attributeName;
        }

        @Override
        public Class<?> getAttributeType() {
            return attributeType;
        }

        @Override
        public String getClassificationMethod() {
            return classificationMethod;
        }

        @Override
        public ColourMap getColourMap() {
            return colourMap;
        }

        @Override
        public int getIntervalsNumber() {
            return intervalsNumber;
        }

        @Override
        public String getSLD() {
            return sld;
        }

    }

    public static final String INVALID_ITEM = "<Invalid>";

    public static final String INVALID_CLASSIFY = "*";

    public static final Integer DEFAULT_N_INTERVALS = 10;

    protected static final ColourMap INVALID_COLOR_MAP = new ColourMap();

    // list of available classification for the vector classifier
    List<Classification> classifications;

    // map of attribute name -> attribute type for the current vector type
    private Map<String, Class<?>> attributeTypes = new HashMap<String, Class<?>>();

    private StyleChangeListener styleListener;

    private ColourMapAttributeChangeListener attributeListener;

    private VerticalLayout vl;

    private HorizontalLayout hl;

    private VerticalLayout vlleft, vlright;

    private Label advancedStylerContainer;

    private TextField advancedStylerConfig;

    private Button updateStyleButton;

    private ComboBox attributesField;

    private DoubleField minValField;

    private DoubleField maxValField;

    private EditableCombo<ColourMap> colorMapCombo;

    private CartographicKey ck;

    private ComboBox classificationMethodField;

    private IntegerField intervalsNumberField;

    private Button classifyButton;

    private Dao dao;

    private boolean styleModified = false;

    private boolean initialized = false;

    private boolean updating = false;

    private StylingMetadata currentStyle = null;

    private StylingMetadata committedStyle = null;

    Logger logger = LoggerFactory.getLogger(StylerFieldGroup.class);

    private ColourMap defaultColorMap;

    static {
        INVALID_COLOR_MAP.setId(null);
        INVALID_COLOR_MAP.setLabel(INVALID_ITEM);
    }

    public StylerFieldGroup(String name, Dao dao) {
        super(new BeanFieldGroup<EcosystemServiceIndicator>(EcosystemServiceIndicator.class),
                name, null);

        List<ColourMap> cms = dao.all(ColourMap.class);
        defaultColorMap = cms.get(0);

        this.dao = dao;

        vl = new VerticalLayout();
        // vl.setHeight("400px");

        hl = new HorizontalLayout();
        hl.setWidth("100%");
        // hl.setHeight("320px");

        vl.addComponent(hl);

        /**
         * LEFT PANEL: Classifier.
         */
        addLeftPanel();

        /**
         * RIGHT PANEL: Advanced Styler.
         */
        addRightPanel();

        hl.addComponent(vlleft);
        hl.addComponent(vlright);
        hl.setExpandRatio(vlleft, 60);
        hl.setExpandRatio(vlright, 40);

        /**
         * Update and save style button.
         */

        updateStyleButton = new Button("Update Style");
        updateStyleButton.setImmediate(true);
        updateStyleButton.setEnabled(false);
        vl.addComponent(updateStyleButton);
        vl.setComponentAlignment(updateStyleButton, Alignment.MIDDLE_CENTER);

        updateStyleButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                if (styleModified && currentStyle != null && styleListener != null) {
                    styleListener.onUpdate(currentStyle);
                } else if (attributeTypes.isEmpty()) {
                    styleListener.onUpdate(getColorMapStyle());
                }
            }

        });

        /*
         * vl.setExpandRatio(hl, 350); vl.setExpandRatio(updateStyleButton, 30);
         */
    }

    protected StylingMetadata getColorMapStyle() {
        return new CachedStylingMetadata(null, null, null, getColourMap(), 10);
    }

    private void addLeftPanel() {
        vlleft = new VerticalLayout();
        vlleft.setWidth("100%");

        addAttributeChoose();
        addColorMapChoose();
        addClassificationChoose();
        addIntervals();
        addClassifyButton();
    }

    private void addRightPanel() {
        vlright = new VerticalLayout();
        vlright.setWidth("100%");

        // div for advanced style (JS implementation)
        advancedStylerContainer = new Label(
                "<div class=\"advanced-styler\" id=\"advancedStyler\"></div>");
        advancedStylerContainer.setContentMode(ContentMode.HTML);
        vlright.addComponent(advancedStylerContainer);

        // hidden textfield for serialized style
        advancedStylerConfig = new TextField();
        advancedStylerConfig.setId("advancedStylerConfig");
        advancedStylerConfig.setValue(null);
        advancedStylerConfig.setImmediate(true);
        advancedStylerConfig.setStyleName("advanced-styler-config");
        advancedStylerConfig.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                if (initialized && !updating) {
                    invalidateClassify();
                    styleModified = true;
                    updateStyleButton.setEnabled(true);
                    currentStyle = getAdvancedStyle();
                }
            }
        });
        vlright.addComponent(advancedStylerConfig);
    }

    private void addClassifyButton() {
        classifyButton = new Button("Classify");
        classifyButton.setImmediate(true);
        classifyButton.setEnabled(false);
        classifyButton.setStyleName("update-style-button");
        vlleft.addComponent(classifyButton);
        classifyButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                if (isClassifyValid(true)) {
                    fireClassify();
                }
            }

        });
    }

    protected void fireClassify() {
        if (styleListener != null) {
            if(isAdvancedStyle() || (currentStyle != null && currentStyle.getSLD() != null)) {
                ConfirmDialog.show(UI.getCurrent(),
                        "Are you sure you want to switch to Classified Style? You will loose all the changes made!",
                        new ConfirmDialog.Listener() {
    
                            public void onClose(ConfirmDialog dialog) {
                                if (dialog.isConfirmed()) {
                                    doClassify();
                                }
                            }
    
                        });
            } else {
                doClassify();
            }
            
        }

    }

    protected boolean isClassifyValid(boolean sendNotify) {
        if (!isAttributeValid()) {
            if (sendNotify) {
                Notification.show("Invalid attribute selected", Type.ERROR_MESSAGE);
            }
            return false;
        }
        if (!isColorMapValid()) {
            if (sendNotify) {
                Notification.show("Invalid color map selected", Type.ERROR_MESSAGE);
            }
            return false;
        }
        if (!isClassificationMethodValid()) {
            if (sendNotify) {
                Notification.show("Invalid classification method selected", Type.ERROR_MESSAGE);
            }
            return false;
        }
        if (!isIntervalsValid()) {
            if (sendNotify) {
                Notification.show("Intervals # is not valid, should be a positive integer number",
                        Type.ERROR_MESSAGE);
            }
            return false;
        }
        return true;
    }

    private boolean isIntervalsValid() {

        String value = intervalsNumberField.getValue();
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            int intervals = Integer.parseInt(value);
            if (intervals <= 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean isClassificationMethodValid() {
        if (classificationMethodField.getValue() == null
                || classificationMethodField.getValue().equals(INVALID_ITEM)) {
            return false;
        }
        return true;
    }

    private boolean isColorMapValid() {
        ColourMap colorMap = colorMapCombo.getValue();
        if (colorMap == null || colorMap.equals(INVALID_COLOR_MAP)) {
            return false;
        }
        return true;
    }

    private boolean isAttributeValid() {
        String attributeName = getAttributeName();
        if (attributeName == null || attributeName.equals(INVALID_CLASSIFY)
                || attributeName.equals(INVALID_ITEM)) {
            return false;
        }
        return true;
    }

    private void addIntervals() {
        intervalsNumberField = new IntegerField("Intervals #");
        intervalsNumberField.setImmediate(true);
        intervalsNumberField.setVisible(true);
        intervalsNumberField.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (initialized) {
                    enableDisableClassify();
                }
            }
        });
        vlleft.addComponent(intervalsNumberField);

        getFieldGroup().bind(intervalsNumberField,
                EcosystemServiceIndicator_.intervalsNumber.getName());
    }

    private void addClassificationChoose() {
        // classifictions
        classifications = dao.all(Classification.class);
        classificationMethodField = new ComboBox("Classification");
        classificationMethodField.setImmediate(true);
        classificationMethodField.setVisible(true);
        classificationMethodField.setNullSelectionItemId(INVALID_ITEM);
        classificationMethodField.addItem(INVALID_ITEM);
        for (Classification classification : classifications) {
            classificationMethodField.addItem(classification);
        }

        vlleft.addComponent(classificationMethodField);

        classificationMethodField.addValueChangeListener(new Property.ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (initialized) {
                    if (classificationMethodField.getValue() != null
                            && !classificationMethodField.getValue().equals(INVALID_ITEM)) {

                        if (isStringAttributeType()
                                && !isAvailableForString((Classification) classificationMethodField
                                        .getValue())) {
                            updateClassificationForString();
                            Notification
                                    .show("The selected classification is not valid for this type of attribute",
                                            Type.ERROR_MESSAGE);
                        }
                    }
                    enableDisableClassify();
                }
            }
        });

        getFieldGroup().bind(classificationMethodField,
                EcosystemServiceIndicator_.classification.getName());
    }

    protected void enableDisableClassify() {
        classifyButton.setEnabled(isClassifyValid(false));
    }

    private void addColorMapChoose() {
        // color map
        colorMapCombo = new EditableCombo<ColourMap>(ColourMap.class, dao) {

            @Override
            protected void populateCombo() {
                encapsulatedField.addItem(INVALID_COLOR_MAP);
                super.populateCombo();
            }

        };
        colorMapCombo.setCaption("Color Map");
        colorMapCombo.getEncapsulatedField().setNullSelectionItemId(INVALID_COLOR_MAP);
        ColourMapEditor cme = new ColourMapEditor(dao);
        cme.init(new ColourMapView());
        colorMapCombo.setEditor(cme);
        colorMapCombo.setImmediate(true);
        colorMapCombo.setDescription(dao.getFieldDescription(EcosystemServiceIndicator_.colourMap));

        colorMapCombo.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if (initialized) {
                    enableDisableClassify();
                    updateColors();
                }
            }

        });

        vlleft.addComponent(colorMapCombo);

        GridLayout gl = new GridLayout(2, 4);
        ck = new CartographicKey();
        ck.setWidth("30px");

        StringToDoubleConverter std = new StringToDoubleConverter() {

            @Override
            protected NumberFormat getFormat(Locale locale) {
                return super.getFormat(Locale.ENGLISH);
            }
        };

        minValField = new DoubleField("Min. value", std);
        maxValField = new DoubleField("Max. value", std);
        minValField.setImmediate(true);
        maxValField.setImmediate(true);

        minValField.setDescription(dao.getFieldDescription(EcosystemServiceIndicator_.minVal));
        maxValField.setDescription(dao.getFieldDescription(EcosystemServiceIndicator_.maxVal));

        // Rowspan of 2
        gl.addComponent(ck, 0, 0, 0, 3);
        gl.addComponent(maxValField, 1, 0);
        gl.addComponent(minValField, 1, 3);

        vlleft.addComponent(gl);

        getFieldGroup().bind(colorMapCombo, EcosystemServiceIndicator_.colourMap.getName());
        getFieldGroup().bind(minValField, EcosystemServiceIndicator_.minVal.getName());
        getFieldGroup().bind(maxValField, EcosystemServiceIndicator_.maxVal.getName());
    }

    private void updateColors() {
        ColourMap cm = getColourMap();

        if (cm == null) {
            logger.error("Null colormap");
            ck.setVisible(false);
        } else {
            ck.setVisible(true);
            List<ColourMapEntry> colourMapEntries = cm.getColourMapEntries();
            ck.setColours(colourMapEntries);
        }
    }

    private void addAttributeChoose() {
        attributesField = new ComboBox("Attribute");
        attributesField.setImmediate(true);
        attributesField.setVisible(true);

        attributesField.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                fireAttributeValueChanged();
            }
        });

        vlleft.addComponent(attributesField);

        /*
         * HorizontalLayout exlayout = new HorizontalLayout(); exlayout.addComponent(minValField); exlayout.addComponent(maxValField);
         * exlayout.setStyleName("min-max-layout"); vlleft.addComponent(exlayout);
         */

        getFieldGroup().bind(attributesField, EcosystemServiceIndicator_.attributeName.getName());

    }

    private void fireAttributeValueChanged() {
        if (initialized) {
            String attributeName = getAttributeName();
            if (attributeName != null
                    && !(attributeName.equals(INVALID_ITEM) || attributeName
                            .equals(INVALID_CLASSIFY))) {
                Class<?> attributeType = getAttributeType();
                makeClassificationValid();
                if (attributeListener != null) {
                    attributeListener.onValueChanged(attributeName, attributeType);
                }
            } else {
                invalidateClassify();
            }
        }
    }

    private void makeClassificationValid() {
        makeClassificationMethodValid();

        if (colorMapCombo.getValue() == null || colorMapCombo.getValue().equals(INVALID_COLOR_MAP)) {
            Iterator<?> iterator = colorMapCombo.getEncapsulatedField().getItemIds().iterator();
            // skip Invalid
            iterator.next();
            colorMapCombo.getEncapsulatedField().setValue((ColourMap) iterator.next());
        }
        colorMapCombo.setEnabled(true);
        ck.setVisible(true);
        intervalsNumberField.setEnabled(true);
        minValField.setEnabled(true);
        maxValField.setEnabled(true);
        classifyButton.setEnabled(true);
    }

    private void makeClassificationMethodValid() {
        if (isStringAttributeType()) {
            updateClassificationForString();
        } else if (classificationMethodField.getValue() == null
                || classificationMethodField.getValue().toString().equals(INVALID_ITEM)) {
            classificationMethodField.setValue(classifications.get(0));
        }
        classificationMethodField.setEnabled(true);
    }

    private void updateClassificationForString() {
        Classification newClassification = null;
        for (Classification classification : classifications) {

            if (isAvailableForString(classification)) {
                newClassification = classification;
            }
        }
        if (newClassification != null
                && (classificationMethodField.getValue() == null || !classificationMethodField
                        .getValue().equals(newClassification))) {
            classificationMethodField.setValue(newClassification);
        }
    }

    private boolean isAvailableForString(Classification classification) {
        // only unique values can be used on strings
        return classification.getId() == 2l;
    }

    protected boolean isStringAttributeType() {
        Class<?> attributeType = getAttributeType();
        return attributeType != null && String.class.isAssignableFrom(attributeType);
    }

    @Override
    public String getAttributeName() {
        return attributesField.getValue() == null ? null : attributesField.getValue().toString();
    }

    @Override
    public Class<?> getAttributeType() {
        String attributeName = getAttributeName();
        if (attributeTypes != null && attributeName != null) {
            return attributeTypes.get(attributeName);
        }
        return null;
    }

    @Override
    public String getClassificationMethod() {
        return classificationMethodField.getValue() == null ? "equalInterval"
                : ((Classification) classificationMethodField.getValue()).getMethod();
    }

    @Override
    public ColourMap getColourMap() {
        ColourMap cm = (ColourMap) colorMapCombo.getValue();

        Double min = (Double) minValField.getConvertedValue();
        Double max = (Double) maxValField.getConvertedValue();

        if (cm == null) {
            // colorMapCombo.setInternalValue(defaultColorMap);
            return null;
        }

        if (min == null) {
            min = 0d;
            // setMinValue("0");
        }

        if (max == null || max == 0.0d) {
            max = 1d;
            // setMaxValue("1");
        }

        List<ColourMapEntry> colourMapEntries = cm.getColourMapEntries();

        if (colourMapEntries.size() != 2) {
            logger.error("Empty colour map entries");
            return null;
        }

        colourMapEntries.get(0).setValue(min);
        colourMapEntries.get(1).setValue(max);

        return cm;
    }

    @Override
    public int getIntervalsNumber() {
        if (intervalsNumberField.getValue() == null) {
            return DEFAULT_N_INTERVALS;
        }
        try {
            return Integer.parseInt(intervalsNumberField.getValue());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getSLD() {
        return advancedStylerConfig.getValue();
    }

    public Component getContent() {
        return vl;
    }

    public void setDefaultValues() {
        /**
         * editableCombo.setInternalValue(defaultValue); setMinValue("0"); setMaxValue("1"); ck.setColours(defaultValue.getColourMapEntries());
         * 
         * attributesField.removeAllItems(); setAttributeVisibility(false); updateStyleButton.setEnabled(false);
         * 
         * editableCombo.setVisible(true); minValField.setVisible(true); maxValField.setVisible(true); ck.setVisible(true);
         * advancedStylerContainer.setVisible(false);
         */
    }

    public void setUpdateStyleListener(StyleChangeListener styleListener) {
        this.styleListener = styleListener;
    }

    public void setAttributeListener(ColourMapAttributeChangeListener attributeListener) {
        this.attributeListener = attributeListener;
    }

    public void initStyler(Map<String, Class<?>> attributes, String selectedAttribute) {
        styleModified = false;
        currentStyle = null;

        attributesField.removeAllItems();
        if (attributes != null && !attributes.isEmpty()) {
            updateStyleButton.setEnabled(false);
            setClassifierVisibility(true);
            attributeTypes = attributes;

            fillAttributesCombo(attributes);
            if (isValidAttribute(selectedAttribute)) {
                selectClassifyAttribute(selectedAttribute);
                committedStyle = getBasicStyle();
            } else {
                committedStyle = getAdvancedStyle();
                invalidateClassify();
            }
            updateColors();
            classifyButton.setVisible(true);
            classifyButton.setEnabled(false);
            updateStyleButton.setEnabled(false);

            // build JS advanced styler
            JavaScript.getCurrent().execute("ESPStyler.showStyler()");
        } else {
            committedStyle = null;
            setClassifierVisibility(false);
            attributeTypes = new HashMap<String, Class<?>>();
            updateColors();
            updateStyleButton.setEnabled(true);
            advancedStylerConfig.setValue(null);
        }
    }

    private void setClassifierVisibility(boolean visibility) {
        attributesField.setVisible(visibility);
        classificationMethodField.setVisible(visibility);
        intervalsNumberField.setVisible(visibility);
        classifyButton.setVisible(visibility);
        advancedStylerContainer.setVisible(visibility);
    }

    private CachedStylingMetadata getAdvancedStyle() {
        return new CachedStylingMetadata(advancedStylerConfig.getValue());
    }

    private void invalidateClassify() {
        attributesField.select(INVALID_ITEM);

        /*
         * colorMapCombo.getEncapsulatedField().setValue( colorMapCombo.getEncapsulatedField().getItemIds().iterator().next());
         */
        colorMapCombo.setEnabled(false);

        classificationMethodField.select(INVALID_ITEM);
        classificationMethodField.setEnabled(false);
        intervalsNumberField.setEnabled(false);

        minValField.setValue("0");
        maxValField.setValue("0");
        minValField.setEnabled(false);
        maxValField.setEnabled(false);

        ck.setVisible(false);

        classifyButton.setEnabled(false);
    }

    private void selectClassifyAttribute(String selectedAttribute) {
        attributesField.select(selectedAttribute);
        if (classificationMethodField.getValue() == null) {
            Class<?> attributeType = attributeTypes.get(selectedAttribute);
            if (attributeType != null && String.class.isAssignableFrom(attributeType)) {
                // updateClassificationForString();
            } else {
                classificationMethodField.select(classifications.get(0));
            }
        }
    }

    private void fillAttributesCombo(Map<String, Class<?>> attributes) {
        attributesField.addItem(INVALID_ITEM);
        for (String attribute : attributes.keySet()) {
            attributesField.addItem(attribute);
        }
    }

    private boolean isValidAttribute(String selectedAttribute) {
        return selectedAttribute != null && !selectedAttribute.equals(INVALID_CLASSIFY);
    }

    /**
     * public void enableUpdateStyle(boolean b) { //updateStyleButton.setEnabled(enabled); }
     */

    public void updateStyle(String styleName, String style, String symbolType, String attributes,
            boolean classified) throws PublishException {
        updating = true;
        styleModified = classified;

        try {
            JSONObject json = new JSONObject();
            json.accumulate("style", style);
            json.accumulate("layer", styleName);
            if (symbolType != null) {
                json.accumulate("symbolType", symbolType);
            }
            if (attributes != null) {
                json.accumulate("attributes", attributes);
            }

            JavaScript.getCurrent().execute("ESPStyler.configureStyler(" + json.toString() + ")");
            advancedStylerConfig.setValue(style);
            JavaScript.getCurrent().execute("ESPStyler.showStyler()");

            if (classified) {
                updateStyleButton.setEnabled(true);
                currentStyle = getBasicStyle();
            } else {
                updateStyleButton.setEnabled(false);
                classifyButton.setEnabled(false);
                committedStyle = currentStyle;
                currentStyle = null;
            }
        } catch (JSONException e) {
            throw new PublishException("Error configuring the advanced styler", e);
        } finally {
            updating = false;
        }
    }

    public void setMinValue(String minValue) {
        minValField.setInternalValue(minValue);
    }

    public void setMaxValue(String maxValue) {
        maxValField.setInternalValue(maxValue);
    }

    public void startUpdate() {
        initialized = false;
    }

    public void endUpdate() {
        initialized = true;
    }

    public void updateUI(EcosystemServiceIndicator entity) {
        if (entity.getSpatialDataType().getId() == 2l) {
            attributesField.setEnabled(entity.getLayerName() != null);
            if (entity.getLayerName() == null || !isClassifyValid(false)) {
                invalidateClassify();
            }
        } else {
            if (entity.getLayerName() == null) {
                setClassifierVisibility(false);
                invalidateClassify();
            }
        }

    }

    public ColourMap getDefaultColourMap() {
        double min = 0.0;
        double max = 1.0;
        if (minValField.getValue() != null) {
            min = (Double) minValField.getConvertedValue();
        }

        if (maxValField.getValue() != null) {
            max = (Double) maxValField.getConvertedValue();
        }

        List<ColourMapEntry> colourMapEntries = defaultColorMap.getColourMapEntries();

        colourMapEntries.get(0).setValue(min);
        colourMapEntries.get(1).setValue(max);
        return defaultColorMap;
    }

    public void setDefaultColorMap() {
        colorMapCombo.getEncapsulatedField().setValue(defaultColorMap);
    }

    public StylingMetadata getBasicStyle() {
        return new CachedStylingMetadata(getAttributeName(), getAttributeType(),
                getClassificationMethod(), getColourMap(), getIntervalsNumber());
    }

    public void preCommit(EcosystemServiceIndicator entity) {
        if (this.committedStyle != null) {
            if (entity.getSpatialDataType().getId() == 2) {
                if (committedStyle.getAttributeName() == null
                        || committedStyle.getAttributeName().equals(INVALID_ITEM)) {
                    entity.setAttributeName(INVALID_CLASSIFY);
                    entity.setColourMap(null);
                }

                if (isAdvancedStyle()) {
                    invalidateClassify();
                } else {
                    entity.setAttributeName(committedStyle.getAttributeName());
                    entity.setColourMap(committedStyle.getColourMap());
                    entity.setClassification(getClassificationFor(committedStyle
                            .getClassificationMethod()));
                    entity.setIntervalsNumber(committedStyle.getIntervalsNumber());
                    /*
                     * startUpdate(); attributesField.select(committedStyle.getAttributeName());
                     * colorMapCombo.getEncapsulatedField().setValue(committedStyle.getColourMap()); Classification classification =
                     * getClassificationFor(committedStyle.getClassificationMethod()); classificationMethodField.setValue(classification);
                     * intervalsNumberField.setValue(committedStyle.getIntervalsNumber()+""); endUpdate();
                     */
                }
            }
        }
    }

    private Classification getClassificationFor(String classificationMethod) {
        for (Classification classification : classifications) {
            if (classification.getMethod().equals(classificationMethod)) {
                return classification;
            }
        }
        return null;
    }

    private boolean isAdvancedStyle() {
        return committedStyle != null && committedStyle.getSLD() != null
                && committedStyle.getAttributeName() == null;
    }

    private void doClassify() {
        styleListener.onClassify(getColourMap(), getAttributeName(), getClassificationMethod(),
                getIntervalsNumber());
    }

    /**
     * public boolean isValid() {
     * 
     * if (minValField.getValue() == null) { return false; } if (maxValField.getValue() == null) { return false; } if (editableCombo.getValue() ==
     * null) { return false; }
     * 
     * return false; } return true;
     */

}
