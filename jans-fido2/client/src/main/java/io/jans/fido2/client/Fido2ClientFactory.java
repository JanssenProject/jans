/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;

/**
 * Helper class which creates proxy Fido2 services
 *
 * @author Yuriy Movchan
 * @version 12/21/2018
 */
public class Fido2ClientFactory {

    private final static Fido2ClientFactory instance = new Fido2ClientFactory();

    private ApacheHttpClient43Engine engine;
    private ObjectMapper objectMapper;
    

    private Fido2ClientFactory() {
        this.engine = createEngine();
        this.objectMapper = new ObjectMapper();
    }

    public static Fido2ClientFactory instance() {
        return instance;
    }

    public ConfigurationService createMetaDataConfigurationService(String metadataUri) {
        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataUri));
        ConfigurationService proxy = target.proxy(ConfigurationService.class);
        
        return proxy;
    }

    public AttestationService createAttestationService(String metadata) throws IOException {
        JsonNode metadataJson = objectMapper.readTree(metadata);
        String basePath = metadataJson.get("attestation").get("base_path").asText();

        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        AttestationService proxy = target.proxy(AttestationService.class);
        
        return proxy;
    }

    public AssertionService createAssertionService(String metadata) throws IOException {
        JsonNode metadataJson = objectMapper.readTree(metadata);
        String basePath = metadataJson.get("assertion").get("base_path").asText();

        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        AssertionService proxy = target.proxy(AssertionService.class);
        
        return proxy;
    }

    private ApacheHttpClient43Engine createEngine() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
        		.setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        engine.setFollowRedirects(true);
        
        return engine;
    }

}
