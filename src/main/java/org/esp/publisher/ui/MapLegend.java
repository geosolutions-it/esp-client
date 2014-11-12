package org.esp.publisher.ui;

import it.jrc.ui.HtmlHeader;
import it.jrc.ui.HtmlLabel;

import java.util.List;

import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.Study;
import org.esp.domain.publisher.ColourMap;
import org.esp.domain.publisher.ColourMapEntry;
import org.esp.publisher.GeoserverRestApi;
import org.esp.publisher.PublishException;
import org.esp.publisher.colours.CartographicKey;
import org.json.JSONException;
import org.json.JSONObject;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;

/**
 * The map legend used in {@link SearchView}
 * 
 * @author Will Temperley
 * 
 */
public class MapLegend extends CssLayout {

    private HtmlHeader header = new HtmlHeader();

    private HtmlLabel label = new HtmlLabel();

    private CartographicKey ck = new CartographicKey();
    
    private HtmlLabel legend = new HtmlLabel("<div class=\"advanced-styler\" id=\"advancedLegend\"></div>");
    
    private GeoserverRestApi gsr;

    public MapLegend(GeoserverRestApi gsr) {
        addComponent(header);
        addComponent(label);
        addComponent(ck);
        addComponent(legend);
        ck.showValues(true);
        setSizeFull();
        addStyleName("map-legend");
        this.gsr = gsr;
    }

    public void setValue(EcosystemServiceIndicator entity) {
        header.setCaption(entity.getIndicator().getLabel());
        legend.setVisible(false);
        ColourMap colourMap = entity.getColourMap();
        if (colourMap != null) {
            if(entity.getAttributeName() != null && entity.getAttributeName().equals("*")) {
                ck.setVisible(false);
                legend.setVisible(true);
                JSONObject json = new JSONObject();
                try {
                    String layerName = entity.getLayerName();
                    String style = gsr.getStyle(layerName);
                    json.accumulate("style", style);
                    json.accumulate("layer", layerName);
                    
                    JavaScript.getCurrent().execute("ESPStyler.configureStyler("+json.toString()+")");
                    JavaScript.getCurrent().execute("ESPStyler.showLegend()");
                } catch (JSONException e) {
                    throw new RuntimeException("Error configuring the advanced legend", e);
                }
                
            } else {    
                ck.setVisible(true);
                List<ColourMapEntry> colourMapEntries = colourMap
                        .getColourMapEntries();
    
                if (colourMapEntries != null && colourMapEntries.size() == 2) {
                    Double minVal = entity.getMinVal();
                    if (minVal == null) {
                        minVal = 0d;
                    }
                    colourMapEntries.get(0).setValue(minVal);
                    Double maxVal = entity.getMaxVal();
                    if (maxVal == null) {
                        maxVal = 1d;
                    }
                    colourMapEntries.get(1).setValue(maxVal);
                    ck.setColours(colourMap.getColourMapEntries());
                }
            }

        } else {
            ck.setVisible(false);
        }

        label.setValue(getLink(entity.getStudy()));
        label.setWidth("100%");
    }

    private String getLink(Study s) {

        StringBuilder sb = new StringBuilder();
        
        sb.append("Study reference: <br/>");

        if (s.getUrl() != null) {
            sb.append("<a href='");
            sb.append(s.getUrl());
            sb.append("' target='_blank'>");
            sb.append(s.getStudyName());
            sb.append("</a>");
        } else {
            sb.append(s.getStudyName());
        }
        
        sb.append("<br/>");
        sb.append(s.getProjectReferences());

        return sb.toString();
    }

}
