/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.client;

import java.io.IOException;

import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class which creates proxy Fido2 services
 *
 * @author Yuriy Movchan
 * @version 12/21/2018
 */
public class Fido2ClientFactory {

    private final static Fido2ClientFactory instance = new Fido2ClientFactory();

    private ObjectMapper objectMapper;

    private Fido2ClientFactory() {
        this.objectMapper = new ObjectMapper();
    }

    public static Fido2ClientFactory instance() {
        return instance;
    }

    public ConfigurationService createMetaDataConfigurationService(String metadataUri) {
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataUri));
        ConfigurationService proxy = target.proxy(ConfigurationService.class);
        
        return proxy;
    }

    public AttestationService createAttestationService(String metadata) throws IOException {
        JsonNode metadataJson = objectMapper.readTree(metadata);
        String basePath = metadataJson.get("attestation").get("base_uri").asText();

        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        AttestationService proxy = target.proxy(AttestationService.class);
        
        return proxy;
    }

    public AssertionService createAssertionService(String metadata) throws IOException {
        JsonNode metadataJson = objectMapper.readTree(metadata);
        String basePath = metadataJson.get("assertion").get("base_uri").asText();

        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        AssertionService proxy = target.proxy(AssertionService.class);
        
        return proxy;
    }

}
