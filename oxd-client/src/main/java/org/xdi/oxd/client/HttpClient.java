package org.xdi.oxd.client;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/05/2017
 */

public class HttpClient {

    private HttpClient() {
    }

    public static HttpClientInterface client(String endpoint) {
        return ProxyFactory.create(HttpClientInterface.class, endpoint);
    }

    public static HttpClientInterface client(String endpoint, ClientExecutor clientExecutor) {
        return ProxyFactory.create(HttpClientInterface.class, endpoint, clientExecutor);
    }
}
