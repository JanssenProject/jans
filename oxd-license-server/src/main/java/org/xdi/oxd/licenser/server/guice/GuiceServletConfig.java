package org.xdi.oxd.licenser.server.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import org.xdi.oxd.licenser.server.guice.AppModule;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

public class GuiceServletConfig extends GuiceServletContextListener {

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new ServletModule(), new AppModule());
    }
}
