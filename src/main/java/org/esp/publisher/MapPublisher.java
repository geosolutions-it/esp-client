package org.esp.publisher;

import it.jrc.form.controller.EditorController;
import it.jrc.form.controller.EditorController.EditCompleteListener;
import it.jrc.form.view.TwinPanelView;
import it.jrc.persist.Dao;
import it.jrc.ui.SimplePanel;

import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.publisher.form.ESIEditor;
import org.esp.publisher.form.LayerPublishedListener;
import org.esp.publisher.form.ESIEditorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addon.leaflet.LMap;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vividsolutions.jts.geom.Polygon;

/***
 * Interface for publishing maps.
 * 
 * Colour maps are stored in database entities. TODO: allow for choosing either
 * from default colour maps or personal ones
 * 
 * [ ] Support for multiple formats (geotiff, world * file etc)
 * 
 * @author Will Temperley
 * 
 */
public class MapPublisher extends TwinPanelView implements View {
    
    private Logger logger = LoggerFactory.getLogger(MapPublisher.class);

    private Dao dao;

    private LayerManager layerManager;

    private EditorController<EcosystemServiceIndicator> surfaceEditor;

    @Inject
    public MapPublisher(Dao dao, ESIEditor surfaceEditor, 
            @Named("gs_wms_url") String defaultWms) {

        this.dao = dao;

        this.surfaceEditor = surfaceEditor;
        
        surfaceEditor.setPublishEventListener(new LayerPublishedListener() {

            @Override
            public void onLayerPublished(String layerName, Polygon extent, long timestamp) {
                layerManager.setSurfaceLayerName(layerName, timestamp);
                layerManager.zoomTo(extent);
            }
        });

        {
            VerticalLayout vl = new VerticalLayout();
            vl.setSizeFull();

            LMap map = surfaceEditor.getMap();
            layerManager = new LayerManager(map, defaultWms);
            vl.addComponent(map);
            map.setSizeFull();

            getLeftPanel().addComponent(vl);
        }


        {
            SimplePanel rightPanel = getRightPanel();

            TabSheet tabSheet = new TabSheet();
            tabSheet.setSizeFull();
            replaceComponent(rightPanel, tabSheet);

            ESIEditorView esiEditorView =  new ESIEditorView();
            ESIEditorView esiEditorView2 =  new ESIEditorView();
            surfaceEditor.init(esiEditorView);
            surfaceEditor.initTab2(esiEditorView2);
            surfaceEditor.addEditCompleteListener(new EditCompleteListener<EcosystemServiceIndicator>() {
                @Override
                public void onEditComplete(EcosystemServiceIndicator entity) {
                    
                }
            });

            tabSheet.addTab(esiEditorView, "Maps");
            tabSheet.addTab(esiEditorView2, "INSPIRE");
        }
    }


    @Override
    public void enter(ViewChangeEvent event) {

        String params = event.getParameters();
        if (params != null && !params.isEmpty()) {
            doEditById(params);
        } else {
            layerManager.reset();
            surfaceEditor.doCreate();
        }
    }

    private void doEditById(String stringId) {
        try {
            Long id = Long.valueOf(stringId);
            EcosystemServiceIndicator entity = dao.find(EcosystemServiceIndicator.class, id);

            if (entity == null) {
                Notification.show("No entity with id: " + stringId);
                return;
            }
            
            surfaceEditor.doUpdate(entity);
            if(entity.getLayerName() == null) {
                Notification.show("No layer bound to entity with id: " + stringId);
                return;
            }
            layerManager.setSurfaceLayerName(entity.getLayerName(), entity.getTimestamp());
            layerManager.zoomTo(entity.getEnvelope());

        } catch (NumberFormatException e) {
            Notification.show("This isn't a valid id: " + stringId);
        }
    }
}
