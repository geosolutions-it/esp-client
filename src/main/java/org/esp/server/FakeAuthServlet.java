package org.esp.server;

import freemarker.template.Configuration;
import it.jrc.auth.AuthServlet;

import java.io.IOException;

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.expressme.openid.OpenIdManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class FakeAuthServlet extends AuthServlet {

    @Inject
    public FakeAuthServlet(OpenIdManager manager, 
            @Named("context_path") String contextPath,
            @Named("login_page_url") String loginPageUrl,
            Configuration templateConf, EntityManagerFactory emf) {
        super(manager, contextPath, "dummy", "dummy", "http://dummy.it", "http://dummy.it", loginPageUrl, templateConf, emf);
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {


            SecurityUtils.getSubject().login(new UsernamePasswordToken("willtemperley@gmail.com", ""));
            //SecurityUtils.getSubject().login(new UsernamePasswordToken("jimimaes@gmail.com", ""));
            super.doGet(request, response);

    }

}
