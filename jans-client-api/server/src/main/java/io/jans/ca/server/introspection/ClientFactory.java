package io.jans.ca.server.introspection;

import io.jans.as.model.uma.UmaMetadata;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

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
        final ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));

        return target.proxy(BackCompatibleIntrospectionService.class);
    }

    public BackCompatibleIntrospectionService createBackCompatibleIntrospectionService(String url, ClientHttpEngine clientEngine) {
        final ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(clientEngine).build();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));
        return target.proxy(BackCompatibleIntrospectionService.class);
    }

    public BadRptIntrospectionService createBadRptStatusService(UmaMetadata metadata) {
        final ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getIntrospectionEndpoint()));

        return target.proxy(BadRptIntrospectionService.class);
    }

    public BadRptIntrospectionService createBadRptStatusService(UmaMetadata metadata, ClientHttpEngine clientEngine) {
        final ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(clientEngine).build();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getIntrospectionEndpoint()));
        return target.proxy(BadRptIntrospectionService.class);
    }

    public CorrectRptIntrospectionService createCorrectRptStatusService(UmaMetadata metadata) {
        final ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getIntrospectionEndpoint()));

        return target.proxy(CorrectRptIntrospectionService.class);
    }

    public CorrectRptIntrospectionService createCorrectRptStatusService(UmaMetadata metadata, ClientHttpEngine clientEngine) {
        final ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(clientEngine).build();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getIntrospectionEndpoint()));
        return target.proxy(CorrectRptIntrospectionService.class);
    }
}
