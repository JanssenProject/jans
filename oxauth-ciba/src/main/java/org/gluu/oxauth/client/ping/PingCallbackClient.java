/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.ping;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.gluu.oxauth.client.BaseClient;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
public class PingCallbackClient extends BaseClient<PingCallbackRequest, PingCallbackResponse> {

    private static final Logger LOG = Logger.getLogger(PingCallbackClient.class);

    private final boolean fapiCompatibility;

    public PingCallbackClient(String url, boolean fapiCompatibility) {
        super(url);
        this.fapiCompatibility = fapiCompatibility;
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public PingCallbackResponse exec() {
        if (this.fapiCompatibility) {
            setExecutor(getApacheHttpClient4ExecutorForMTLS());
        }
        initClientRequest();
        return _exec();
    }

    private PingCallbackResponse _exec() {
        try {
            // Prepare request parameters
            clientRequest.setHttpMethod(getHttpMethod());

            clientRequest.header("Content-Type", getRequest().getContentType());

            if (StringUtils.isNotBlank(getRequest().getClientNotificationToken())) {
                clientRequest.header("Authorization", "Bearer " + getRequest().getClientNotificationToken());
            }

            JSONObject requestBody = getRequest().getJSONParameters();
            clientRequest.body(MediaType.APPLICATION_JSON, requestBody.toString(4));

            // Call REST Service and handle response
            clientResponse = clientRequest.post(String.class);
            setResponse(new PingCallbackResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    /**
     * Creates an executor responsible to process rest calls using special SSL context.
     */
    private ApacheHttpClient4Executor getApacheHttpClient4ExecutorForMTLS() {
        try {
            // Ciphers accepted by FAPI-CIBA specs and OpenJDK.
            String[] ciphers = new String[] { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384" };
            SSLContext sslContext = SSLContexts.createDefault();
            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
                    new String[] { "TLSv1.2" }, ciphers, NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", sslConnectionFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                    .setConnectionManager(cm)
                    .build();

            return new ApacheHttpClient4Executor(httpClient);
        } catch (Exception e) {
            LOG.error("Error creating Ping rest client, specific creating executor for SSL Context", e);
            return null;
        }
    }

}
