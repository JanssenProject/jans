package org.xdi.oxd.client;

import org.glassfish.jersey.client.proxy.WebResourceFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 * @author yuriyz
 */
public class OxdClient {

    private OxdClient() {
    }

    public static ClientInterface newClient(String target) {
        WebTarget webTarget = ClientBuilder.newClient().target(target);
        return WebResourceFactory.newResource(ClientInterface.class, webTarget);
    }
}
