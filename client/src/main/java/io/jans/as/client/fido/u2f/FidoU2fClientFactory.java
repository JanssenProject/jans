/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.fido.u2f;

import io.jans.as.client.service.ClientFactory;
import io.jans.as.model.fido.u2f.U2fConfiguration;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import javax.ws.rs.core.UriBuilder;

/**
 * Helper class which creates proxy FIDO U2F services
 *
 * @author Yuriy Movchan Date: 05/27/2015
 */
public class FidoU2fClientFactory {

    private final static FidoU2fClientFactory instance = new FidoU2fClientFactory();

    private final ApacheHttpClient4Engine engine;

    private FidoU2fClientFactory() {
        this.engine = ClientFactory.instance().createEngine();
    }

    public static FidoU2fClientFactory instance() {
        return instance;
    }

    public U2fConfigurationService createMetaDataConfigurationService(String u2fMetaDataUri) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(u2fMetaDataUri));
        U2fConfigurationService proxy = target.proxy(U2fConfigurationService.class);

        return proxy;
    }

    public AuthenticationRequestService createAuthenticationRequestService(U2fConfiguration metadataConfiguration) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataConfiguration.getAuthenticationEndpoint()));
        AuthenticationRequestService proxy = target.proxy(AuthenticationRequestService.class);

        return proxy;
    }

    public RegistrationRequestService createRegistrationRequestService(U2fConfiguration metadataConfiguration) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataConfiguration.getRegistrationEndpoint()));
        RegistrationRequestService proxy = target.proxy(RegistrationRequestService.class);

        return proxy;
    }
}
