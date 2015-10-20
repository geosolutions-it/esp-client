package org.esp.publisher.ui;

import it.jrc.inject.AbstractViewModule;

import java.util.Map;

import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.publisher.MapPublisher;
import org.vaadin.addons.guice.uiscope.UIScoped;

import com.google.inject.multibindings.MapBinder;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;

/**
 * Binds views to their URLs which results in an injectable {@link Map} of
 * views.
 * 
 * Editors will appear in navigation under the first part of the URL, named as
 * the second part of the URL.
 * 
 * Also provides reverse links through a class-to-url mapping
 * 
 * @author will
 */
public class ViewModule extends AbstractViewModule {

	public static final String PUBLISH = "Publish";
    public static final String HOME = "Home";
    private static final String VERSION = "Version";

    @Override
    protected void configure() {
        mapbinder = MapBinder.newMapBinder(binder(), String.class, View.class);
        mapbinder.addBinding(PUBLISH).to(MapPublisher.class).in(UIScoped.class);
        mapbinder.addBinding(HOME).to(SearchView.class).in(UIScoped.class);
        mapbinder.addBinding(VERSION).to(VersionInfoView.class).in(UIScoped.class);
    }

    public static String getESILink(EcosystemServiceIndicator esi) {
        return PUBLISH + "/" +  esi.getId();
    }
    
    public static String getFullESILink(EcosystemServiceIndicator esi) {
        String basePath = Page.getCurrent().getLocation().getScheme() + ":" +
                Page.getCurrent().getLocation().getSchemeSpecificPart();
        return basePath + getESILink(esi);
    } 
    
}
