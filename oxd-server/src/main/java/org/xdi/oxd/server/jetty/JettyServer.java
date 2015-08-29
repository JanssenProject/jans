package org.xdi.oxd.server.jetty;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuriy on 8/29/2015.
 */
public class JettyServer {

    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

    private final Server server;

    public JettyServer(int port) {
        server = new Server(port);

        server.setHandler(ServerHandlerBuilder.build());
    }

    public void start() {
        try {
            server.start();
            //LOG.trace(server.dump());
            //server.dumpStdErr();
            server.join();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void stop() throws Exception {
        server.stop();
    }
}
