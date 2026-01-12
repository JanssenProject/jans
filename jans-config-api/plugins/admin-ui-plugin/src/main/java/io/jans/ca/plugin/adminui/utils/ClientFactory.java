package io.jans.ca.plugin.adminui.utils;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;

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
}