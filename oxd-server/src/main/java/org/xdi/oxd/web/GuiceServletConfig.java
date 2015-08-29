package org.xdi.oxd.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/08/2015
 */

public class GuiceServletConfig extends GuiceServletContextListener {

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new ServletModule(), new AppModule());
    }
}

