package org.xdi.oxd.license.client;

import org.jboss.resteasy.client.ProxyFactory;

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
        return ProxyFactory.create(clientInterface, endpoint);
    }

}
