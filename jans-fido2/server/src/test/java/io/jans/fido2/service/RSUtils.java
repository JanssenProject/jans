package io.jans.fido2.service;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import jakarta.ws.rs.client.ClientBuilder;

public final class RSUtils {

    public static ResteasyClient getClient() {
        return (ResteasyClient) ClientBuilder.newClient();
    }
}
