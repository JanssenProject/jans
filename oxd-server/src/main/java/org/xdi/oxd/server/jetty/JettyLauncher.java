package org.xdi.oxd.server.jetty;

/**
 * Created by yuriy on 8/30/2015.
 */
public class JettyLauncher {

    public static void main(String[] args) {
        JettyServer server = new JettyServer(8080);
        server.start();
    }

}
