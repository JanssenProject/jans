/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.service;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import javax.ws.rs.core.UriBuilder;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public class ClientFactory {

    private static final ClientFactory INSTANCE = new ClientFactory();

    private final ApacheHttpClient4Engine engine;

    private ClientFactory() {
        this.engine = createEngine();
    }

    public static ClientFactory instance() {
        return INSTANCE;
    }

    public StatService createStatService(String url) {
        return createStatService(url, engine);
    }

    public StatService createStatService(String url, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));
        return target.proxy(StatService.class);
    }

    public IntrospectionService createIntrospectionService(String url) {
        return createIntrospectionService(url, engine);
    }

    public IntrospectionService createIntrospectionService(String url, ClientExecutor clientExecutor) {
        return ProxyFactory.create(IntrospectionService.class, url, clientExecutor);
    }
    
    public IntrospectionService createIntrospectionService(String url, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));
        return target.proxy(IntrospectionService.class);
    }

    public ApacheHttpClient4Engine createEngine() {
        return createEngine(false);
    }

    public ApacheHttpClient4Engine createEngine(boolean followRedirects) {
        return createEngine(200, 20, CookieSpecs.STANDARD, followRedirects);
    }

	public ApacheHttpClient4Engine createEngine(int maxTotal, int defaultMaxPerRoute, String cookieSpec, boolean followRedirects) {
	    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	    CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(cookieSpec).build())
	    		.setConnectionManager(cm).build();
	    cm.setMaxTotal(maxTotal);
	    cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
        final ApacheHttpClient4Engine client4Engine = new ApacheHttpClient4Engine(httpClient);
        client4Engine.setFollowRedirects(followRedirects);
        return client4Engine;
	}
}
