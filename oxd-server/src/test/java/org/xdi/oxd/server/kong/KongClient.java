package org.xdi.oxd.server.kong;

import org.jboss.resteasy.client.ProxyFactory;

/**
 * Created by yuriy on 17.10.16.
 */
public class KongClient {

    public static final String API = "/apis";

    private KongClient() {
    }

    public static KongApiService createApiService(String url) {
        return ProxyFactory.create(KongApiService.class, url + API);
    }

    public static MockBinService createMockBinService(String url) {
        return ProxyFactory.create(MockBinService.class, url);
    }
}
