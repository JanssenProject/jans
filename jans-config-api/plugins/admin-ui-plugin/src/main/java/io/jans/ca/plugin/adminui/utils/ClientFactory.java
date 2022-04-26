package io.jans.ca.plugin.adminui.utils;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

public class ClientFactory {
    private final static ClientFactory INSTANCE = new ClientFactory();

    private ClientFactory() {
    }

    public static ClientFactory instance() {
        return INSTANCE;
    }

   /* public ResteasyWebTarget createResteasyWebTargetWithCompleteUrl(String url) {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();

        ResteasyClient client = ((ResteasyClientBuilderImpl) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(url);

        return target;
    }

    public ResteasyWebTarget createResteasyWebTarget(String url) {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();

        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));

        return target;
    }*/

    public static Invocation.Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }
}
