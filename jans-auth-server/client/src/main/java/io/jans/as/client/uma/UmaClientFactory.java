/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma;

import io.jans.as.client.service.ClientFactory;
import io.jans.as.model.uma.UmaMetadata;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Helper class which creates proxied UMA services
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 */
public class UmaClientFactory {

    private static final UmaClientFactory instance = new UmaClientFactory();

    private final ApacheHttpClient43Engine engine;

    private UmaClientFactory() {
        this.engine = ClientFactory.instance().createEngine(true);
    }

    public static UmaClientFactory instance() {
        return instance;
    }

    public UmaResourceService createResourceService(UmaMetadata metadata) {
        return createResourceService(metadata, engine);
    }

    public UmaResourceService createResourceService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyWebTarget target = newClient(engine).target(UriBuilder.fromPath(metadata.getResourceRegistrationEndpoint()));
        return target.proxy(UmaResourceService.class);
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadata) {
        return createPermissionService(metadata, engine);
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyWebTarget target = newClient(engine).target(UriBuilder.fromPath(metadata.getPermissionEndpoint()));
        return target.proxy(UmaPermissionService.class);
    }

    public UmaRptIntrospectionService createRptStatusService(UmaMetadata metadata) {
        return createRptStatusService(metadata, engine);
    }

    public UmaRptIntrospectionService createRptStatusService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyWebTarget target = newClient(engine).target(UriBuilder.fromPath(metadata.getIntrospectionEndpoint()));
        return target.proxy(UmaRptIntrospectionService.class);
    }

    public UmaMetadataService createMetadataService(String umaMetadataUri) {
        return createMetadataService(umaMetadataUri, engine);
    }

    public UmaMetadataService createMetadataService(String umaMetadataUri, ClientHttpEngine engine) {
        ResteasyWebTarget target = newClient(engine).target(UriBuilder.fromPath(umaMetadataUri));
        return target.proxy(UmaMetadataService.class);
    }

    public UmaScopeService createScopeService(String scopeEndpointUri) {
        return createScopeService(scopeEndpointUri, engine);
    }

    public UmaScopeService createScopeService(String scopeEndpointUri, ClientHttpEngine engine) {
        ResteasyWebTarget target = newClient(engine).target(UriBuilder.fromPath(scopeEndpointUri));
        return target.proxy(UmaScopeService.class);
    }

    public UmaTokenService createTokenService(UmaMetadata metadata) {
        return createTokenService(metadata, engine);
    }

    public UmaTokenService createTokenService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyWebTarget target = newClient(engine).target(UriBuilder.fromPath(metadata.getTokenEndpoint()));
        return target.proxy(UmaTokenService.class);
    }

    public ResteasyClient newClient(ClientHttpEngine engine) {
        return ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
    }
}
