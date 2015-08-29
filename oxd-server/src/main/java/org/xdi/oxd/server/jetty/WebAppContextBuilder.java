package org.xdi.oxd.server.jetty;

import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Created by yuriy on 8/30/2015.
 */
public class WebAppContextBuilder {

    public static WebAppContext build() {
        WebAppContext context = new WebAppContext();
        context.setDescriptor("resources/WEB-INF/web.xml");
        context.setResourceBase(".");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        return context;
    }
}
