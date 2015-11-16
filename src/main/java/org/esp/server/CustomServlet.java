package org.esp.server;

import org.vaadin.addons.guice.servlet.VGuiceApplicationServlet;
import org.vaadin.addons.guice.ui.ScopedUIProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;

@Singleton
public class CustomServlet extends VGuiceApplicationServlet {

    private DownlaodRequestHandler downlaodRequestHandler;

    @Inject
    public CustomServlet(ScopedUIProvider applicationProvider, DownlaodRequestHandler downlaodRequestHandler) {
        super(applicationProvider);
        this.downlaodRequestHandler = downlaodRequestHandler;
    }
    
    /**
     * Allow to manage download request URL
     * {@link #DownlaodRequestHandler}
     */
    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        super.sessionInit(event);
        event.getSession().addRequestHandler(downlaodRequestHandler);
    }
}
