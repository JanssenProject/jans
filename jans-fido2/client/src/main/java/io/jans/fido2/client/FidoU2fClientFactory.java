/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.client;

import io.jans.as.client.service.ClientFactory;
import io.jans.fido2.model.u2f.U2fConfiguration;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Helper class which creates proxy FIDO U2F services
 *
 * @author Yuriy Movchan Date: 05/27/2015
 */
public class FidoU2fClientFactory {

    private final static FidoU2fClientFactory instance = new FidoU2fClientFactory();

    private final ApacheHttpClient43Engine engine;

    private FidoU2fClientFactory() {
        this.engine = ClientFactory.instance().createEngine();
    }

    public static FidoU2fClientFactory instance() {
        return instance;
    }

    public U2fConfigurationService createMetaDataConfigurationService(String u2fMetaDataUri) {
        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(u2fMetaDataUri));
        return target.proxy(U2fConfigurationService.class);
    }

    public AuthenticationRequestService createAuthenticationRequestService(U2fConfiguration metadataConfiguration) {
        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataConfiguration.getAuthenticationEndpoint()));
        return target.proxy(AuthenticationRequestService.class);
    }

    public RegistrationRequestService createRegistrationRequestService(U2fConfiguration metadataConfiguration) {
        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataConfiguration.getRegistrationEndpoint()));
        return target.proxy(RegistrationRequestService.class);
    }
}
