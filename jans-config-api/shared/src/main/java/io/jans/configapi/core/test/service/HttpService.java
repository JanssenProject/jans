/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.test.service;

import io.jans.model.net.HttpServiceResponse;
import io.jans.util.StringHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

public class HttpService implements Serializable {

    private static final long serialVersionUID = -2398422090669045605L;
    protected transient Logger log = LogManager.getLogger(getClass());
    private static final String CON_STATS_STR = "Connection manager stats: {}";
    private transient Base64 base64;

    private transient PoolingHttpClientConnectionManager connectionManager;

    @PostConstruct
    public void init() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200); // Increase max total connection to 200
        connectionManager.setDefaultMaxPerRoute(50); // Increase default max connection per route to 50

        this.base64 = new Base64();
    }

    public CloseableHttpClient getHttpsClientTrustAll()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());

        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext,
                NoopHostnameVerifier.INSTANCE);

        return HttpClients.custom().setSSLSocketFactory(sslConSocFactory)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager).build();
    }

    public CloseableHttpClient getHttpsClient() {
        return getHttpsClient(RequestConfig.custom().build());
    }

    public CloseableHttpClient getHttpsClient(RequestConfig requestConfig) {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());

        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager).build();
    }

    public CloseableHttpClient getHttpsClient(HttpRoutePlanner routerPlanner) {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());

        return getHttpsClient(RequestConfig.custom().build(), routerPlanner);
    }

    public CloseableHttpClient getHttpsClient(RequestConfig requestConfig, HttpRoutePlanner routerPlanner) {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());

        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager).setRoutePlanner(routerPlanner).build();
    }

    public CloseableHttpClient getHttpsClient(String trustStorePath, String trustStorePassword)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(new File(trustStorePath), trustStorePassword.toCharArray()).build();
        SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext);

        return HttpClients.custom().setSSLSocketFactory(sslConSocFactory)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager).build();
    }

    public CloseableHttpClient getHttpsClient(String trustStorePath, String trustStorePassword, String keyStorePath,
            String keyStorePassword) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException, IOException, UnrecoverableKeyException {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(new File(trustStorePath), trustStorePassword.toCharArray())
                .loadKeyMaterial(new File(keyStorePath), keyStorePassword.toCharArray(), keyStorePassword.toCharArray())
                .build();
        SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext);

        return HttpClients.custom().setSSLSocketFactory(sslConSocFactory)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager).build();
    }

    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authCode,
            Map<String, String> headers, String postData, ContentType contentType, String authType) {

        HttpPost httpPost = new HttpPost(uri);

        if (StringHelper.isEmpty(authType)) {
            authType = "Basic ";
        } else {
            authType = authType + " ";
        }
        if (StringHelper.isNotEmpty(authCode)) {
            httpPost.setHeader("Authorization", authType + authCode);
        }

        if (contentType == null) {
            contentType = ContentType.APPLICATION_JSON;
        }

        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                httpPost.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }

        StringEntity stringEntity = new StringEntity(postData, contentType);
        httpPost.setEntity(stringEntity);

        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);

            return new HttpServiceResponse(httpPost, httpResponse);
        } catch (IOException ex) {
            log.error("Failed to execute post request", ex);
        }

        return null;
    }

    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authCode,
            Map<String, String> headers, String postData) {
        return executePost(httpClient, uri, authCode, headers, postData, null, null);
    }

    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authCode, String postData,
            ContentType contentType) {
        return executePost(httpClient, uri, authCode, null, postData, contentType, null);
    }

    public HttpServiceResponse executePost(String uri, String authCode, String postData,
            ContentType contentType, String authType) {
        return executePost(this.getHttpsClient(), uri, authCode, null, postData, contentType, authType);
    }

    public String encodeBase64(String value) {
        return new String(base64.encode((value).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public String encodeUrl(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public HttpServiceResponse executeGet(HttpClient httpClient, String requestUri, Map<String, String> headers,
            Map<String, String> parameters) {
        HttpGet httpGet = new HttpGet(requestUri);

        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                httpGet.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }

        if (parameters != null && !parameters.isEmpty()) {
            StringBuilder query = new StringBuilder("");
            for (String key : parameters.keySet()) {

                String value = parameters.get(key);
                if (value != null && value.length() > 0) {

                    String delim = "&" + URLEncoder.encode(key, StandardCharsets.UTF_8) + "=";
                    query.append(delim.substring(1));
                    query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            }
            httpGet = new HttpGet(requestUri + query.toString());
        }

        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);

            return new HttpServiceResponse(httpGet, httpResponse);
        } catch (IOException ex) {
            log.error("Failed to execute get request", ex);
        }

        return null;
    }

    public HttpServiceResponse executeGet(String requestUri, Map<String, String> headers, Map<String, String> data) {
        HttpClient httpClient = this.getHttpsClient();
        return executeGet(httpClient, requestUri, headers, data);
    }

    public HttpServiceResponse executeGet(String requestUri, Map<String, String> headers) {
        HttpClient httpClient = this.getHttpsClient();
        return executeGet(httpClient, requestUri, headers, null);
    }

    public HttpServiceResponse executeGet(HttpClient httpClient, String requestUri) {
        return executeGet(httpClient, requestUri, null, null);
    }

    public byte[] getResponseContent(HttpResponse httpResponse) throws IOException {
        if ((httpResponse == null) || !isResponseStastusCodeOk(httpResponse)) {
            return null;
        }

        HttpEntity entity = httpResponse.getEntity();
        byte[] responseBytes = new byte[0];
        if (entity != null) {
            responseBytes = EntityUtils.toByteArray(entity);
        }

        // Consume response content
        if (entity != null) {
            EntityUtils.consume(entity);
        }

        return responseBytes;
    }

    public void consume(HttpResponse httpResponse) throws IOException {
        if ((httpResponse == null) || !isResponseStastusCodeOk(httpResponse)) {
            return;
        }

        // Consume response content
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            EntityUtils.consume(entity);
        }
    }

    public String convertEntityToString(byte[] responseBytes) {
        if (responseBytes == null) {
            return null;
        }

        return new String(responseBytes);
    }

    public String convertEntityToString(byte[] responseBytes, Charset charset) {
        if (responseBytes == null) {
            return null;
        }

        return new String(responseBytes, charset);
    }

    public String convertEntityToString(byte[] responseBytes, String charsetName) throws UnsupportedEncodingException {
        if (responseBytes == null) {
            return null;
        }

        return new String(responseBytes, charsetName);
    }

    public boolean isResponseStastusCodeOk(HttpResponse httpResponse) {
        int responseStastusCode = httpResponse.getStatusLine().getStatusCode();
        if ((responseStastusCode == HttpStatus.SC_OK) || (responseStastusCode == HttpStatus.SC_CREATED)
                || (responseStastusCode == HttpStatus.SC_ACCEPTED)
                || (responseStastusCode == HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION)
                || (responseStastusCode == HttpStatus.SC_NO_CONTENT)
                || (responseStastusCode == HttpStatus.SC_RESET_CONTENT)
                || (responseStastusCode == HttpStatus.SC_PARTIAL_CONTENT)
                || (responseStastusCode == HttpStatus.SC_MULTI_STATUS)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isResponseStatusCodeOk(HttpResponse httpResponse) {
        return isResponseStastusCodeOk(httpResponse);
    }

    public boolean isContentTypeXml(HttpResponse httpResponse) {
        Header contentType = httpResponse.getEntity().getContentType();
        if (contentType == null) {
            return false;
        }

        String contentTypeValue = contentType.getValue();
        if (StringHelper.equals(contentTypeValue, ContentType.APPLICATION_XML.getMimeType())
                || StringHelper.equals(contentTypeValue, ContentType.TEXT_XML.getMimeType())) {
            return true;
        } else {
            return false;
        }
    }

    public String constructServerUrl(final HttpServletRequest request) {
        int serverPort = request.getServerPort();

        String redirectUrl;
        if ((serverPort == 80) || (serverPort == 443)) {
            redirectUrl = String.format("%s://%s%s", request.getScheme(), request.getServerName(),
                    request.getContextPath());
        } else {
            redirectUrl = String.format("%s://%s:%s%s", request.getScheme(), request.getServerName(),
                    request.getServerPort(), request.getContextPath());
        }

        return redirectUrl.toLowerCase();
    }

    public HttpRoutePlanner buildDefaultRoutePlanner(final String hostname, final int port, final String scheme) {
        // Creating an HttpHost object for proxy
        HttpHost proxyHost = new HttpHost(hostname, port, scheme);

        return new DefaultProxyRoutePlanner(proxyHost);
    }

    public HttpRoutePlanner buildDefaultRoutePlanner(final String proxy) {
        return buildDefaultRoutePlanner(proxy, -1, null);
    }

}
