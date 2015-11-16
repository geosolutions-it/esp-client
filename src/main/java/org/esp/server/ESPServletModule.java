package org.esp.server;

import it.jrc.auth.AuthFilter;
import it.jrc.auth.AuthServlet;
import it.jrc.auth.JpaRealm;
import it.jrc.auth.SecurityFilter;
import it.jrc.inject.AbstractGuiceServletModule;

import org.apache.shiro.realm.Realm;

import com.google.inject.name.Names;
import com.google.inject.persist.PersistFilter;

/**
 * 
 * @author Will Temperley
 *
 */
public class ESPServletModule extends AbstractGuiceServletModule {

    

    @Override
    protected void configureServlets() {
        
        
        bind(Realm.class).to(JpaRealm.class);

        /*
         * Persistence objects
         */
//        bind(Dao.class).in(SessionScoped.class);

        /*
         * Bind constants
         */
        Names.bindProperties(binder(), getRuntimeProperties());

        /*
         * Security and persistence filters
         */
        filter("/*").through(PersistFilter.class);

        filter("/*").through(SecurityFilter.class, getIni());
        
        filterRegex("^((?!/getoriginal).)*$").through(AuthFilter.class);
        
        if (isInProductionMode()) {
            serve("/login").with(AuthServlet.class);
//            serve("/login").with(AnonymousAuthServlet.class);
//            serve("/login").with(FakeAuthServlet.class);
        } else {
            serve("/login").with(FakeAuthServlet.class);
        }
        
        /*
         * Main application servlet
         */
        serve("/*").with(CustomServlet.class, getServletParams());

    }
    
}