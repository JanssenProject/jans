/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.xdi.oxd.web.CommandServlet;

/**
 * Created by yuriy on 8/30/2015.
 */
public class ServerHandlerBuilder {

    public static Handler build() {
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(CommandServlet.class, "/rest/command");
        return handler;
//        server.setHandler(handler);
//
//        WebAppContext context = new WebAppContext();
//        context.setDescriptor("resources/WEB-INF/web.xml");
////        context.setWar();
//        context.setResourceBase(".");
//        context.setContextPath("/");
//        context.setParentLoaderPriority(true);
//        return context;
    }
}
