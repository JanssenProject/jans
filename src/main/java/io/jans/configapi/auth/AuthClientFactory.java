/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.client.service.IntrospectionService;
import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.UmaMetadata;

import io.jans.configapi.auth.client.OpenIdClientService;
import io.jans.configapi.util.ApiConstants;

import javax.ws.rs.core.UriBuilder;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

public class AuthClientFactory {

    public static OpenIdClientService getIntrospectionService(String url, boolean followRedirects) {
        return createIntrospectionService(url, followRedirects);
    }

    public static UmaMetadataService getUmaMetadataService(String umaMetadataUri, boolean followRedirects) {
        return createUmaMetadataService(umaMetadataUri, followRedirects);
    }

    public static UmaPermissionService getUmaPermissionService(UmaMetadata umaMetadata, boolean followRedirects) {
        return createUmaPermissionService(umaMetadata);
    }

    public static UmaRptIntrospectionService getUmaRptIntrospectionService(UmaMetadata umaMetadata,
            boolean followRedirects) {
        return createUmaRptIntrospectionService(umaMetadata);
    }

    private static OpenIdClientService createIntrospectionService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = createEngine(followRedirects);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
                .register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
        OpenIdClientService proxy = target.proxy(OpenIdClientService.class);
        return proxy;
    }

    private static UmaMetadataService createUmaMetadataService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = createEngine(followRedirects);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
                .register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
        UmaMetadataService proxy = target.proxy(UmaMetadataService.class);
        return proxy;
    }

    private static UmaPermissionService createUmaPermissionService(UmaMetadata umaMetadata) {
        ApacheHttpClient43Engine engine = createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromPath(umaMetadata.getPermissionEndpoint()).build()).register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(umaMetadata.getPermissionEndpoint());
        UmaPermissionService proxy = target.proxy(UmaPermissionService.class);
        return proxy;
    }

    private static UmaRptIntrospectionService createUmaRptIntrospectionService(UmaMetadata umaMetadata) {
        ApacheHttpClient43Engine engine = createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromPath(umaMetadata.getIntrospectionEndpoint()).build()).register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(umaMetadata.getPermissionEndpoint());
        UmaRptIntrospectionService proxy = target.proxy(UmaRptIntrospectionService.class);
        return proxy;
    }

    private static ApacheHttpClient43Engine createEngine(boolean followRedirects) {
        return createEngine(ApiConstants.CONNECTION_POOL_MAX_TOTAL, ApiConstants.CONNECTION_POOL_DEFAULT_MAX_PER_ROUTE,
                ApiConstants.CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY, CookieSpecs.STANDARD, followRedirects);
    }

    private static ApacheHttpClient43Engine createEngine(int maxTotal, int defaultMaxPerRoute,
            int validateAfterInactivity, String cookieSpec, boolean followRedirects) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
        cm.setValidateAfterInactivity(validateAfterInactivity * 1000);

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(cookieSpec).build())
                .setKeepAliveStrategy(connectionKeepAliveStrategy).setConnectionManager(cm).build();

        final ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        engine.setFollowRedirects(followRedirects);
        return engine;
    }

    private static ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {

            HeaderElementIterator headerElementIterator = new BasicHeaderElementIterator(
                    httpResponse.headerIterator(HTTP.CONN_KEEP_ALIVE));

            while (headerElementIterator.hasNext()) {

                HeaderElement headerElement = headerElementIterator.nextElement();

                String name = headerElement.getName();
                String value = headerElement.getValue();

                if (value != null && name.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }

            // Set own keep alive duration if server does not have it
            return ApiConstants.CONNECTION_POOL_CUSTOM_KEEP_ALIVE_TIMEOUT * 1000;
        }
    };
}
