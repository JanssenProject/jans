package io.jans.configapi.security.client;

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
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import io.jans.configapi.util.ApiConstants;

public class ClientFactoryUtil {

    public static ApacheHttpClient43Engine createEngine(boolean followRedirects) {
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
