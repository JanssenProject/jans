package io.jans.ca.plugin.adminui.utils;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;

@ApplicationScoped
public class ClientFactory {
    private final static ClientFactory INSTANCE = new ClientFactory();
    private final Client client;

    private ClientFactory() {
        this.client = ClientBuilder.newClient();
    }

    public static ClientFactory instance() {
        return INSTANCE;
    }

    public Invocation.Builder getClientBuilder(String url) {
        return client.target(url).request();
    }

    @PreDestroy
    public void destroy() {
        if (this.client != null) {
            this.client.close();
        }
    }
}