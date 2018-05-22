package org.xdi.oxd.server;

import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.client.OxdClient;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class Tester {

    private Tester() {
    }

    public static ClientInterface newClient(String targetHost) {
        if ("http://localhost".equalsIgnoreCase(targetHost) || "http://127.0.0.1".equalsIgnoreCase(targetHost) ) {
            targetHost = targetHost + ":" + SetUpTest.SUPPORT.getLocalPort();
        }
        return OxdClient.newClient(targetHost);
    }

    public static String getAuthorization() {
        return "";
    }
}
