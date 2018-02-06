package org.xdi.oxd.server.introspection;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;

/**
 * @author yuriyz
 */
public class ClientFactory {
    private final static ClientFactory INSTANCE = new ClientFactory();

    private ClientFactory() {
    }

    public static ClientFactory instance() {
        return INSTANCE;
    }

    public BackCompatibleIntrospectionService createBackCompatibleIntrospectionService(String url) {
        return ProxyFactory.create(BackCompatibleIntrospectionService.class, url);
    }

    public BackCompatibleIntrospectionService createBackCompatibleIntrospectionService(String url, ClientExecutor clientExecutor) {
        return ProxyFactory.create(BackCompatibleIntrospectionService.class, url, clientExecutor);
    }
}
