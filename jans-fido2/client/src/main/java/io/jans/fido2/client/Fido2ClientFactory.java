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

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
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
        return createAttestationService(metadata, null, null);
    }

    /**
     * Same as {@link #createAttestationService(String)}, but has the proxy carry the end user's real
     * IP/user agent to fido2 on every call — for a caller (Casa, a person-authentication script) that
     * sits between the browser and fido2 and would otherwise show up in metrics as the relay's own
     * connection. Either argument may be {@code null} to skip forwarding it.
     */
    public AttestationService createAttestationService(String metadata, String forwardedFor, String userAgent) throws IOException {
        JsonNode metadataJson = objectMapper.readTree(metadata);
        String basePath = metadataJson.get("attestation").get("base_path").asText();

        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        registerClientContextFilter(target, forwardedFor, userAgent);
        AttestationService proxy = target.proxy(AttestationService.class);

        return proxy;
    }

    public AssertionService createAssertionService(String metadata) throws IOException {
        return createAssertionService(metadata, null, null);
    }

    /** Same as {@link #createAssertionService(String)}, see {@link #createAttestationService(String, String, String)}. */
    public AssertionService createAssertionService(String metadata, String forwardedFor, String userAgent) throws IOException {
        JsonNode metadataJson = objectMapper.readTree(metadata);
        String basePath = metadataJson.get("assertion").get("base_path").asText();

        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        registerClientContextFilter(target, forwardedFor, userAgent);
        AssertionService proxy = target.proxy(AssertionService.class);

        return proxy;
    }

    /**
     * Registers a filter that stamps X-Forwarded-For/User-Agent onto every request the resulting proxy
     * sends, so a caller that already knows the browser's connection details (a caller cannot otherwise
     * set headers on a JAX-RS proxy interface it doesn't own) can pass them through to fido2's metrics.
     * A null argument is not forwarded, and the filter is skipped entirely when both are null.
     */
    private void registerClientContextFilter(ResteasyWebTarget target, String forwardedFor, String userAgent) {
        if (forwardedFor == null && userAgent == null) {
            return;
        }
        target.register((ClientRequestFilter) (ClientRequestContext requestContext) -> {
            if (forwardedFor != null) {
                requestContext.getHeaders().putSingle("X-Forwarded-For", forwardedFor);
            }
            if (userAgent != null) {
                requestContext.getHeaders().putSingle("User-Agent", userAgent);
            }
        });
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
