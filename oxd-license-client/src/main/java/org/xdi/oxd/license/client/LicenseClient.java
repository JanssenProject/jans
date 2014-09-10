package org.xdi.oxd.license.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

public class LicenseClient {

    private LicenseClient() {
    }

    public static GenerateWS generateWs(String endpoint) {
        return proxy(GenerateWS.class, endpoint);
    }

    public static <T> T proxy(Class<T> clientInterface, String endpoint) {
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(endpoint);

        return target.proxy(clientInterface);

    }

}
