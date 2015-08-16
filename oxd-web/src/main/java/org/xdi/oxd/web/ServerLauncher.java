package org.xdi.oxd.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuriy on 8/16/2015.
 */
public class ServerLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(ServerLauncher.class);

    public static void main(String[] args) {
        try {
            Server server = new Server(8080);

            WebAppContext context = new WebAppContext();
            context.setDescriptor("webapp/WEB-INF/web.xml");
            context.setResourceBase("../test-jetty-webapp/src/main/webapp");
            context.setContextPath("/");
            context.setParentLoaderPriority(true);

            server.setHandler(context);

            server.start();
            server.join();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
