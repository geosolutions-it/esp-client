package org.esp.publisher.ui;

import it.jrc.inject.GuicedViewProvider;
import it.jrc.persist.Dao;

import org.vaadin.addons.guice.ui.ScopedUI;

import com.google.inject.Inject;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.VerticalLayout;

@Theme("dashboard")
@JavaScript({"vaadin://themes/dashboard/js/ext/adapter/ext/ext-base.js", 
    "vaadin://themes/dashboard/js/ext/ext-all.js",
    "vaadin://themes/dashboard/js/styler/OpenLayers.js",
    "vaadin://themes/dashboard/js/styler/GeoExt.js",
    "vaadin://themes/dashboard/js/styler/GeoExtExt.js",
    "vaadin://themes/dashboard/js/styler/Styler.js",
    "vaadin://themes/dashboard/js/ESPStyler.js"
    })
@StyleSheet({"vaadin://themes/dashboard/resources/css/ext-all.css",
    "vaadin://themes/dashboard/resources/all.css"})
public class AppUI extends ScopedUI  {

	private VerticalLayout rootLayout = new VerticalLayout();
    
	/*
	 * This holds the dynamically changing content
	 */
    private CssLayout content = new CssLayout();

	private Navigator nav;

	private GuicedViewProvider viewProvider;

        private HeaderView headerView;

	@Inject
	public AppUI(Dao dao, GuicedViewProvider viewProvider, HeaderView headerView) {
		this.viewProvider = viewProvider;
		this.headerView = headerView;
	}

	@Override
	protected void init(VaadinRequest request) {
		
		/*
		 * Set up main panels
		 */
		setContent(rootLayout);
        rootLayout.addStyleName("dashboard-view");
		rootLayout.setSizeFull();
		
		headerView.setHeight("80px");
//        headerView.addStyleName("header");
		rootLayout.addComponent(headerView);
		
		rootLayout.addComponent(content);
		content.setSizeFull();
		rootLayout.setExpandRatio(content, 1);
		
		nav = new Navigator(this, content);
		nav.addProvider(viewProvider);
		
		final NavMenu navMenu = new NavMenu();
		headerView.addMenuBar(navMenu);
		
		//The nav menu implements the view change listener
		nav.addViewChangeListener(navMenu);
	}
	

}
