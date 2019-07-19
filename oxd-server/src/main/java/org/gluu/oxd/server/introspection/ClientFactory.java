package org.gluu.oxd.server.introspection;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.gluu.oxauth.model.uma.UmaMetadata;

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

    public BadRptIntrospectionService createBadRptStatusService(UmaMetadata metadata) {
        return ProxyFactory.create(BadRptIntrospectionService.class, metadata.getIntrospectionEndpoint());
    }

    public BadRptIntrospectionService createBadRptStatusService(UmaMetadata metadata, ClientExecutor clientExecutor) {
        return ProxyFactory.create(BadRptIntrospectionService.class, metadata.getIntrospectionEndpoint(), clientExecutor);
    }

    public CorrectRptIntrospectionService createCorrectRptStatusService(UmaMetadata metadata) {
        return ProxyFactory.create(CorrectRptIntrospectionService.class, metadata.getIntrospectionEndpoint());
    }

    public CorrectRptIntrospectionService createCorrectRptStatusService(UmaMetadata metadata, ClientExecutor clientExecutor) {
        return ProxyFactory.create(CorrectRptIntrospectionService.class, metadata.getIntrospectionEndpoint(), clientExecutor);
    }
}
