package org.xdi.oxd.license.admin.server; /**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class GuiceServletConfig extends GuiceServletContextListener {

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new ServletModule() {

            @Override
            protected void configureServlets() {
                serve("/admin/adminService.rpc").with(AdminServiceImpl.class);
            }
        }, new AdminAppModule());
    }
}
