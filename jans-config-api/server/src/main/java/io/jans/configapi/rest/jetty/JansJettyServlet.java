/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.jetty;

import jakarta.servlet.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.servlet.*;


import java.io.*;
import java.util.*;
import jakarta.ws.rs.ApplicationPath;

import org.slf4j.*;
public class JansJettyServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    {

         // Create a WebAppContext.
         WebAppContext context = new WebAppContext();
         // Configure the path of the packaged web application (file or directory).
         context.setWar("/path/to/webapp.war");
         // Configure the contextPath.
         context.setContextPath("/app");
    
         // Link the context to the server.
         server.setHandler(context);
    }
}
