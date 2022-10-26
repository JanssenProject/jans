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
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public class ClientFactory {

    private static final ClientFactory INSTANCE = new ClientFactory();

    private final ApacheHttpClient43Engine engine;

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
        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));
        return target.proxy(StatService.class);
    }

    public IntrospectionService createIntrospectionService(String url) {
        return createIntrospectionService(url, engine);
    }

    public IntrospectionService createIntrospectionService(String url, ClientHttpEngine engine) {
        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(url));
        return target.proxy(IntrospectionService.class);
    }

    public ApacheHttpClient43Engine createEngine() {
        return createEngine(false);
    }

    public ApacheHttpClient43Engine createEngine(boolean followRedirects) {
        return createEngine(200, 20, CookieSpecs.STANDARD, followRedirects);
    }

    public ApacheHttpClient43Engine createEngine(int maxTotal, int defaultMaxPerRoute, String cookieSpec, boolean followRedirects) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(cookieSpec).build())
                .setConnectionManager(cm).build();
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
        final ApacheHttpClient43Engine client4Engine = new ApacheHttpClient43Engine(httpClient);
        client4Engine.setFollowRedirects(followRedirects);
        return client4Engine;
    }
}
