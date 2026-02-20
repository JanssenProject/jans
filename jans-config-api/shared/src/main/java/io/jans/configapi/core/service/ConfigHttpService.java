/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import io.jans.configapi.core.util.Jackson;
import io.jans.model.net.HttpServiceResponse;
import io.jans.util.StringHelper;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpDelete;
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

@ApplicationScoped
public class ConfigHttpService implements Serializable {

    private static final String OPENID_CONFIGURATION_URL = "/.well-known/openid-configuration";
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

    public static String getOpenidConfigurationUrl() {
        return OPENID_CONFIGURATION_URL;
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

    /**
     * Create an HTTP client configured to use the specified trust store for TLS trust material.
     *
     * @param trustStorePath     filesystem path to the trust store file (e.g., JKS or PKCS12)
     * @param trustStorePassword password for the trust store
     * @return a CloseableHttpClient that uses the provided trust store for SSL/TLS connections
     * @throws KeyManagementException   if the SSL context cannot be initialized
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     * @throws KeyStoreException        if the trust store cannot be loaded or is invalid
     * @throws CertificateException     if any certificates in the trust store cannot be parsed
     * @throws IOException              if an I/O error occurs while reading the trust store file
     */
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

    /**
     * Create a CloseableHttpClient configured with an optional TLS protocol and cipher-suite policy.
     *
     * If `enabledProtocols` is null or empty, the returned client uses the JVM/default TLS configuration.
     *
     * @param enabledProtocols the TLS protocols to enable (e.g., {"TLSv1.2","TLSv1.3"}); when null or empty, default protocols are used
     * @param allowedCipherSuites the cipher suites to enable, or null to use the default cipher suite list
     * @return a CloseableHttpClient configured with the specified TLS protocols and cipher suites (or a default TLS configuration when `enabledProtocols` is null/empty)
     * @throws NoSuchAlgorithmException if the requested SSL algorithm is not available when building the SSLContext
     * @throws KeyManagementException if there is an issue initializing the SSLContext's key management
     */
    public CloseableHttpClient createHttpsClientWithTlsPolicy(String[] enabledProtocols,
                                                              String[] allowedCipherSuites
    ) throws NoSuchAlgorithmException, KeyManagementException {
        log.trace(CON_STATS_STR, connectionManager.getTotalStats());
        RequestConfig requestConfig = RequestConfig.custom().build();

        // Build SSL context with specific protocols
        SSLContext sslContext = SSLContexts.custom().build();
        if (enabledProtocols == null || enabledProtocols.length == 0) {
            return HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD).build())
                    .setConnectionManager(connectionManager)
                    .build();
        }
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                enabledProtocols,
                allowedCipherSuites,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        return HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(connectionManager)
                .build();
    }

    /**
     * Builds an HTTPS CloseableHttpClient configured to use the provided trust store and key store.
     *
     * The returned client uses an SSLContext initialized from the specified trust store and key store,
     * and is configured with the service's connection manager and a standard cookie specification.
     *
     * @param trustStorePath     filesystem path to the trust store file (e.g., JKS/PKCS12)
     * @param trustStorePassword password for the trust store
     * @param keyStorePath       filesystem path to the key store containing client certificate/private key
     * @param keyStorePassword   password for the key store (and key material)
     * @return                   a CloseableHttpClient that uses the loaded SSL context
     * @throws KeyManagementException   if the SSLContext cannot be initialized
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     * @throws KeyStoreException        if the key store cannot be loaded or accessed
     * @throws CertificateException     if any certificate cannot be parsed or validated
     * @throws IOException              if an I/O error occurs reading the store files
     * @throws UnrecoverableKeyException if a key in the key store cannot be recovered (e.g., wrong password)
     */
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

    /**
     * Execute an HTTP POST to the given URI and return the request/response pair.
     *
     * @param httpClient the HttpClient used to execute the request
     * @param uri the target URI for the POST request
     * @param authCode an authorization token to include; if blank, no Authorization header is added
     * @param headers additional headers to include in the request; may be null
     * @param postData the request body as a string; may be null
     * @param contentType the content type for the request body; if null, defaults to application/json
     * @param authType the authentication scheme prefix (for example "Bearer ") to prepend to {@code authCode}
     * @return an HttpServiceResponse containing the executed HttpPost and HttpResponse, or {@code null} if execution failed
     */
    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authCode,
                                           Map<String, String> headers, String postData, ContentType contentType, String authType) {

        HttpPost httpPost = new HttpPost(uri);

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

    /**
     * Execute an HTTP POST to the specified URI using the provided HttpClient and payload.
     *
     * @param httpClient the HttpClient used to execute the request
     * @param uri the target request URI
     * @param authCode an optional authorization value; if non-null an Authorization header is set
     * @param headers additional request headers to include (may be null)
     * @param postData the request body to send (may be null)
     * @return an HttpServiceResponse wrapping the executed HttpPost and HttpResponse, or `null` if execution failed
     */
    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authCode,
                                           Map<String, String> headers, String postData) {
        return executePost(httpClient, uri, authCode, headers, postData, null, null);
    }

    /**
     * Execute an HTTP POST request using the provided HTTP client.
     *
     * @param httpClient the HttpClient to execute the request
     * @param uri the target URI for the POST request
     * @param authCode optional authorization token value; when provided it will be set on the `Authorization` header
     * @param postData the request body to send
     * @param contentType the content type of the request body; if `null`, `application/json` is used
     * @return an HttpServiceResponse containing the executed HttpPost and the HttpResponse, or `null` if the request failed
     */
    public HttpServiceResponse executePost(HttpClient httpClient, String uri, String authCode, String postData,
                                           ContentType contentType) {
        return executePost(httpClient, uri, authCode, null, postData, contentType, null);
    }

    /**
     * Sends an HTTP POST to the given URI using the service's default HTTPS client.
     *
     * @param uri the target request URI
     * @param authCode optional authorization credentials (token or credential string) to include in the Authorization header
     * @param postData the request body to send
     * @param contentType the content type of the request body
     * @param authType the scheme or prefix to use for the Authorization header (for example "Bearer ")
     * @return an HttpServiceResponse containing the executed request and response, or {@code null} if the request failed
     */
    public HttpServiceResponse executePost(String uri, String authCode, String postData, ContentType contentType,
                                           String authType) {
        return executePost(this.getHttpsClient(), uri, authCode, null, postData, contentType, authType);
    }

    public String encodeBase64(String value) {
        return new String(base64.encode((value).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    /**
     * URL-encodes the given string using UTF-8 encoding.
     *
     * @param value the string to URL-encode
     * @return the URL-encoded form of the input string
     */
    public String encodeUrl(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Executes an HTTP GET against the given URI with optional headers and query parameters and returns the request/response pair.
     *
     * The method appends URL-encoded query parameters to the requestUri when provided, applies any supplied headers, and executes the request using the given HttpClient.
     *
     * @param requestUri the target URI (may be appended with URL-encoded query parameters)
     * @param headers    optional HTTP headers to include on the request (may be null)
     * @param parameters optional query parameters to append to the URI; entries with blank values are ignored (may be null)
     * @return a HttpServiceResponse containing the executed HttpGet and the HttpResponse, or `null` if the request failed due to an I/O error
     */
    public HttpServiceResponse executeGet(HttpClient httpClient, String requestUri, Map<String, String> headers,
                                          Map<String, String> parameters) {

        log.info("\n\n requestUri{}, headers:{}, parameters:{}", requestUri, headers, parameters);

        if (parameters != null && !parameters.isEmpty()) {
            StringBuilder query = new StringBuilder();
            int i = 0;
            for (Iterator<String> iterator = parameters.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                String value = parameters.get(key);
                if (StringUtils.isNotBlank(value)) {
                    String delim = (i == 0) ? "?" : "&";
                    query.append(delim + URLEncoder.encode(key, StandardCharsets.UTF_8) + "=");
                    query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                    i++;
                }
            }
            requestUri = requestUri + query.toString();
            log.info("\n\n\n Final requestUri:{}", requestUri);
        }

        HttpGet httpGet = new HttpGet(requestUri);
        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                httpGet.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            log.info("httpResponse:{}", httpResponse);
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

    /**
     * Execute an HTTP DELETE request to the given URI using the service's default HTTPS client.
     *
     * @param requestUri the target URI for the DELETE request
     * @param headers optional HTTP headers to include
     * @param data optional query parameters to append to the URI
     * @return the HttpServiceResponse containing the executed request and response, or {@code null} if the request failed
     */
    public HttpServiceResponse executeDelete(String requestUri, Map<String, String> headers, Map<String, String> data) {
        HttpClient httpClient = this.getHttpsClient();
        return executeDelete(httpClient, requestUri, headers, data);
    }

    /**
     * Execute an HTTP DELETE to the specified URI, applying provided headers and URL-encoded query parameters.
     *
     * Query parameters with blank values are skipped; keys and values are URL-encoded using UTF-8 before being
     * appended to the URI.
     *
     * @param requestUri the target URI (query string will be appended if parameters are provided)
     * @param headers    map of header names to values to set on the request; may be null
     * @param parameters map of query parameter names to values to append to the URI; may be null
     * @return           an HttpServiceResponse containing the executed HttpDelete and the HttpResponse, or `null` if execution failed
     */
    public HttpServiceResponse executeDelete(HttpClient httpClient, String requestUri, Map<String, String> headers,
                                             Map<String, String> parameters) {

        if (parameters != null && !parameters.isEmpty()) {
            StringBuilder query = new StringBuilder();
            int i = 0;
            for (Iterator<String> iterator = parameters.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                String value = parameters.get(key);
                if (StringUtils.isNotBlank(value)) {
                    String delim = (i == 0) ? "?" : "&";
                    query.append(delim + URLEncoder.encode(key, StandardCharsets.UTF_8) + "=");
                    query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                    i++;
                }
            }
            requestUri = requestUri + query.toString();
            log.info("\n\n\n Final Delete requestUri:{}", requestUri);
        }

        HttpDelete httpDelete = new HttpDelete(requestUri);
        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                httpDelete.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        try {
            HttpResponse httpResponse = httpClient.execute(httpDelete);
            log.info("HttpDelete httpResponse:{}", httpResponse);
            return new HttpServiceResponse(httpDelete, httpResponse);
        } catch (IOException ex) {
            log.error("Failed to execute get request", ex);
        }

        return null;
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

    /**
     * Parse the JSON body of an HttpServiceResponse and return its "response" node.
     *
     * If the provided serviceResponse is null or does not contain a parsable JSON body, this method returns null.
     *
     * @param serviceResponse the HttpServiceResponse whose entity will be parsed
     * @return the JsonNode corresponding to the top-level "response" field, or null if unavailable
     * @throws JsonProcessingException if the response body cannot be parsed as JSON
     */
    public JsonNode getResponseJsonNode(HttpServiceResponse serviceResponse) throws JsonProcessingException {
        JsonNode jsonNode = null;

        if (serviceResponse == null) {
            return jsonNode;
        }

        return getResponseJsonNode(getResponseEntityString(serviceResponse), "response");
    }

    /**
     * Extracts the HTTP response body as UTF-8 text and enforces an OK (200) response status.
     *
     * @param serviceResponse the HttpServiceResponse wrapper containing the HttpResponse to read; may be null
     * @return the response body decoded as UTF-8, or {@code null} if {@code serviceResponse}, the contained HttpResponse, or its entity is null
     * @throws WebApplicationException if the HttpResponse status code is not 200; the exception message is formatted as "<status>:<body>"
     */
    public String getResponseEntityString(HttpServiceResponse serviceResponse) {
        String jsonString = null;

        if (serviceResponse == null) {
            return jsonString;
        }
        HttpResponse httpResponse = serviceResponse.getHttpResponse();
        if (httpResponse != null) {
            HttpEntity entity = httpResponse.getEntity();
            log.debug("entity:{}, httpResponse.getStatusLine().getStatusCode():{}", entity,
                    httpResponse.getStatusLine().getStatusCode());
            if (entity == null) {
                return jsonString;
            }
            try {
                jsonString = EntityUtils.toString(entity, "UTF-8");
            } catch (Exception ex) {
                log.error("Error while getting entity using EntityUtils is ", ex);
            }

            if (httpResponse.getStatusLine() != null
                    && httpResponse.getStatusLine().getStatusCode() == Status.OK.getStatusCode()) {
                return jsonString;
            } else {
                throw new WebApplicationException(httpResponse.getStatusLine().getStatusCode() + ":" + jsonString);
            }
        }
        return jsonString;
    }

    public JsonNode getResponseJsonNode(String jsonSring, String nodeName) throws JsonProcessingException {
        JsonNode jsonNode = null;

        if (StringUtils.isBlank(jsonSring)) {
            return jsonNode;
        }
        jsonNode = Jackson.asJsonNode(jsonSring);
        if (StringUtils.isNotBlank(nodeName) && jsonNode != null && jsonNode.get(nodeName) != null) {
            jsonNode = jsonNode.get("response");
        }
        return jsonNode;
    }

    /**
     * Map an HttpServiceResponse's HTTP status code to a JAX-RS Status.
     *
     * @param serviceResponse the HttpServiceResponse whose HTTP response status will be mapped; may be null
     * @return the `Status` corresponding to the response's HTTP status code, or `Status.INTERNAL_SERVER_ERROR` if the response is missing or the code cannot be mapped
     */
    public Status getResponseStatus(HttpServiceResponse serviceResponse) {
        Status status = Status.INTERNAL_SERVER_ERROR;

        if (serviceResponse == null || serviceResponse.getHttpResponse() == null || serviceResponse.getHttpResponse().getStatusLine() == null) {
            return status;
        }

        int statusCode = serviceResponse.getHttpResponse().getStatusLine().getStatusCode();

        status = Status.fromStatusCode(statusCode);
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        return status;
    }

    /**
     * Read and return the UTF-8 text content of the given HttpEntity as a StringBuilder.
     *
     * @param httpEntity the HTTP entity whose content will be read; may be null
     * @return a StringBuilder containing the entity content (empty if the entity is null or has no content)
     * @throws IOException if an I/O error occurs while reading the entity content
     */
    public StringBuilder readEntity(HttpEntity httpEntity) throws IOException {

        StringBuilder result = new StringBuilder();

        if (httpEntity == null) {
            return result;
        }
        try (InputStream inputStream = httpEntity.getContent();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            log.error("Response:{}", result);
        }

        return result;
    }

    /**
     * Read and return the UTF-8 string content of the provided HttpEntity.
     *
     * @param httpEntity the HttpEntity to read; if null, the method returns null
     * @return the entity content decoded as a UTF-8 string, or null if {@code httpEntity} is null
     * @throws WebApplicationException if an I/O error occurs while reading the entity
     */
    public String getContent(HttpEntity httpEntity) {
        String jsonString = null;
        InputStream inputStream = null;
        try {

            if (httpEntity == null) {
                return jsonString;
            }
            inputStream = httpEntity.getContent();
            log.trace("  httpEntity.getContentLength():{}, httpEntity.getContent():{}", httpEntity.getContentLength(),
                    httpEntity.getContent());

            jsonString = IOUtils.toString(httpEntity.getContent(), StandardCharsets.UTF_8);
            log.debug("Data jsonString:{}", jsonString);

        } catch (Exception ex) {
            throw new WebApplicationException("Failed to read data '{" + httpEntity + "}'", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return jsonString;
    }

}