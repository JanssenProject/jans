package io.jans.casa.rest;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import jakarta.ws.rs.client.ClientBuilder;

public final class RSUtils {

    public static ResteasyClient getClient() {
        return (ResteasyClient) ClientBuilder.newClient();
    }

}
