/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.net;

import io.jans.as.server.model.net.HttpServiceResponse;
import io.jans.net.SslDefaultHttpClient;
import io.jans.util.StringHelper;
import io.jans.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides operations with http requests
 *
 * @author Yuriy Movchan Date: 02/05/2013
 */
@ApplicationScoped
@Named
public class HttpService implements Serializable {

    private static final long serialVersionUID = -2398422090669045605L;

    @Inject
    private Logger log;

    private Base64 base64;

    @PostConstruct
    public void init() {
        this.base64 = new Base64();
    }

    public HttpClient getHttpsClientTrustAll() {
        try {
            SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }, new AllowAllHostnameVerifier());

            PlainSocketFactory psf = PlainSocketFactory.getSocketFactory();

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", 80, psf));
            registry.register(new Scheme("https", 443, sf));
            ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);
            return new DefaultHttpClient(ccm);
        } catch (Exception ex) {
            log.error("Failed to create TrustAll https client", ex);
            return new DefaultHttpClient();
        }
    }

    public HttpClient getHttpsClient() {
        return new SslDefaultHttpClient();
    }

    public HttpClient getHttpsClient(String trustStoreType, String trustStorePath, String trustStorePassword) {
        return new SslDefaultHttpClient(trustStoreType, trustStorePath, trustStorePassword);
    }

    public HttpClient getHttpsClient(String trustStoreType, String trustStorePath, String trustStorePassword,
                                     String keyStoreType, String keyStorePath, String keyStorePassword) {
        return new SslDefaultHttpClient(trustStoreType, trustStorePath, trustStorePassword, keyStoreType, keyStorePath, keyStorePassword);
    }

    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authData, Map<String, String> headers, String postData, ContentType contentType) {
        HttpPost httpPost = new HttpPost(uri);
        if (StringHelper.isNotEmpty(authData)) {
            httpPost.setHeader("Authorization", "Basic " + authData);
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

    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authData, Map<String, String> headers, String postData) {
        return executePost(httpClient, uri, authData, headers, postData, null);
    }

    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authData, String postData, ContentType contentType) {
        return executePost(httpClient, uri, authData, null, postData, contentType);
    }

    public String encodeBase64(String value) {
        return new String(base64.encode((value).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public String encodeUrl(String value) {
        try {
            return URLEncoder.encode(value, Util.UTF8);
        } catch (UnsupportedEncodingException ex) {
            log.error("Failed to encode url '{}'", value, ex);
        }

        return null;
    }

    public HttpServiceResponse executeGet(HttpClient httpClient, String requestUri, Map<String, String> headers) {
        HttpGet httpGet = new HttpGet(requestUri);

        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                httpGet.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }

        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);

            return new HttpServiceResponse(httpGet, httpResponse);
        } catch (IOException ex) {
            log.error("Failed to execute get request", ex);
        }

        return null;
    }

    public HttpServiceResponse executeGet(HttpClient httpClient, String requestUri) {
        return executeGet(httpClient, requestUri, null);
    }

    public byte[] getResponseContent(HttpResponse httpResponse) throws IOException {
        if ((httpResponse == null) || (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)) {
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
        if ((httpResponse == null) || (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)) {
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
        return responseStastusCode == HttpStatus.SC_OK;
    }


    public boolean isContentTypeXml(HttpResponse httpResponse) {
        Header contentType = httpResponse.getEntity().getContentType();
        if (contentType == null) {
            return false;
        }

        String contentTypeValue = contentType.getValue();
        return StringHelper.equals(contentTypeValue, ContentType.APPLICATION_XML.getMimeType()) || StringHelper.equals(contentTypeValue, ContentType.TEXT_XML.getMimeType());
    }

    public String constructServerUrl(final HttpServletRequest request) {
        int serverPort = request.getServerPort();

        String redirectUrl;
        if ((serverPort == 80) || (serverPort == 443)) {
            redirectUrl = String.format("%s://%s%s", request.getScheme(), request.getServerName(), request.getContextPath());
        } else {
            redirectUrl = String.format("%s://%s:%s%s", request.getScheme(), request.getServerName(), request.getServerPort(), request.getContextPath());
        }

        return redirectUrl.toLowerCase();
    }

}
