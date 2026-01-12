package io.jans.ca.plugin.adminui.utils;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;

public class ClientFactory {
    private final static ClientFactory INSTANCE = new ClientFactory();

    private ClientFactory() {
    }

    public static ClientFactory instance() {
        return INSTANCE;
    }

    public static Invocation.Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }
}