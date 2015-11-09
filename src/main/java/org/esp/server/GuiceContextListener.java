package org.esp.server;

import java.io.IOException;
import java.util.Properties;

import org.esp.publisher.ui.AppUI;
import org.esp.publisher.ui.ViewModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.guice.uiscope.UIScopeModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.google.inject.servlet.GuiceServletContextListener;

public class GuiceContextListener extends GuiceServletContextListener {
    
    private static Logger logger = LoggerFactory.getLogger(GuiceContextListener.class);
    
    @Override
    protected Injector getInjector() {
        JpaPersistModule pm = new JpaPersistModule("esp-domain");
        try {
            Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("runtime.properties"));
            pm.properties(properties);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }       
        return Guice.createInjector(new ESPServletModule(), new ViewModule(), new UIScopeModule(AppUI.class), pm);
    }
}
