/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.uma;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * Helper class which creates proxied UMA services
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 */
public class UmaClientFactory {

    private final static UmaClientFactory instance = new UmaClientFactory();

    private ApacheHttpClient4Engine engine;

    private UmaClientFactory() {
        this.engine = createEngine();
    }

    public static UmaClientFactory instance() {
        return instance;
    }

    public UmaResourceService createResourceService(UmaMetadata metadata) {
        return createResourceService(metadata, engine);
    }

    public UmaResourceService createResourceService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getResourceRegistrationEndpoint()));
        UmaResourceService proxy = target.proxy(UmaResourceService.class);

        return proxy;
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadata) {
        return createPermissionService(metadata, engine);
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getPermissionEndpoint()));
        UmaPermissionService proxy = target.proxy(UmaPermissionService.class);

        return proxy;
    }

    public UmaRptIntrospectionService createRptStatusService(UmaMetadata metadata) {
        return createRptStatusService(metadata, engine);
    }

    public UmaRptIntrospectionService createRptStatusService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getIntrospectionEndpoint()));
        UmaRptIntrospectionService proxy = target.proxy(UmaRptIntrospectionService.class);

        return proxy;
    }

    public UmaMetadataService createMetadataService(String umaMetadataUri) {
        return createMetadataService(umaMetadataUri, engine);
    }

    public UmaMetadataService createMetadataService(String umaMetadataUri, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(umaMetadataUri));
        UmaMetadataService proxy = target.proxy(UmaMetadataService.class);

        return proxy;
    }

    public UmaScopeService createScopeService(String scopeEndpointUri) {
        return createScopeService(scopeEndpointUri, engine);
    }

    public UmaScopeService createScopeService(String scopeEndpointUri, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(scopeEndpointUri));
        UmaScopeService proxy = target.proxy(UmaScopeService.class);

        return proxy;
    }

    public UmaTokenService createTokenService(UmaMetadata metadata) {
        return createTokenService(metadata, engine);
    }

    public UmaTokenService createTokenService(UmaMetadata metadata, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadata.getTokenEndpoint()));
        UmaTokenService proxy = target.proxy(UmaTokenService.class);

        return proxy;
    }
	
	private ApacheHttpClient4Engine createEngine() {
	    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	    CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
	    		.setConnectionManager(cm).build();
	    cm.setMaxTotal(200); // Increase max total connection to 200
	    cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
	    ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
	    
	    return engine;
	}
}
