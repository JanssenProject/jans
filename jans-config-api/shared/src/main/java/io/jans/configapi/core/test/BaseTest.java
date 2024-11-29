/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.test;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.util.Util;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.core.test.service.HttpService;
import io.jans.configapi.core.test.service.ResteasyService;
import io.jans.configapi.core.test.service.TokenService;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import org.json.JSONObject;
import org.json.JSONException;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class BaseTest {

    private static final String FILE_PREFIX = "file:";
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static final String NEW_LINE = System.getProperty("line.separator");
    protected static final String CONTENT_TYPE = "Content-Type";
    protected static final String AUTHORIZATION = "Authorization";
    protected static final String AUTHORIZATION_TYPE = "Bearer";

    protected Logger log = LogManager.getLogger(getClass());
    protected Base64 base64;
    protected static Map<String, String> propertiesMap = null;
    protected ResteasyService resteasyService = new ResteasyService();;
    protected HttpService httpService = new HttpService();
	protected TokenService tokenService = new TokenService();
    protected String accessToken;

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {

        //log.error("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        log.error("Invoked initTestSuite propertiesFile '{}'", propertiesFile);

        //Properties prop = new Properties();
       // prop.load(Files.newBufferedReader(Paths.get(propertiesFile), UTF_8));

        propertiesMap = context.getSuite().getXmlSuite().getParameters();
        //prop.forEach((key, value) -> propertiesMap.put(key.toString(), value.toString()));
        //context.getSuite().getXmlSuite().setParameters(propertiesMap);

        log.error("End initTestSuite propertiesMap: {}", propertiesMap);

    }

    @AfterSuite
    public void finalize() {
        // cleanup
        log.info("After Suite finalize'");
    }

    @BeforeMethod
    public void getAccessToken() throws Exception {
        log.error("getAccessToken - propertiesMap:{}", propertiesMap);

        String tokenUrl = propertiesMap.get("tokenEndpoint");
        String strGrantType = propertiesMap.get("tokenGrantType");
        String clientId = propertiesMap.get("clientId");
        String clientSecret = propertiesMap.get("clientSecret");
        String scopes = propertiesMap.get("scopes");
        String authStr = clientId + ':' + clientSecret;

        GrantType grantType = GrantType.fromString(strGrantType);
        this.accessToken = getToken(tokenUrl, clientId, clientSecret, grantType, scopes);
        log.error("accessToken:{}", accessToken);
    }
    
    protected String getToken(final String tokenUrl, final String clientId, final String clientSecret, GrantType grantType,
            final String scopes) {
        return getTokenService().getToken(tokenUrl, clientId, clientSecret, grantType, scopes);
    }
   
    protected HttpService getHttpService() {
        return this.httpService;
    }

    protected ResteasyService getResteasyService() {
        return this.resteasyService;
    }
	
	protected TokenService getTokenService() {
        return this.tokenService;
    }

    protected String getCredentials(final String clientId, final String clientSecret)
            throws UnsupportedEncodingException {
        return URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING) + ":"
                + URLEncoder.encode(clientSecret, Util.UTF8_STRING_ENCODING);
    }

    /**
     * Returns the client credentials encoded using base64.
     *
     * @return The encoded client credentials.
     */
    protected String getEncodedCredentials() {
        try {
            String clientId = propertiesMap.get("clientId");
            String clientSecret = propertiesMap.get("clientSecret");
            if (StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(clientSecret)) {
                return Base64.encodeBase64String(Util.getBytes(getCredentials(clientId, clientSecret)));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected String decodeFileValue(String value) {
        log.error("\n\n decodeFileValue");
        String decoded = value;
        if (value.startsWith(FILE_PREFIX)) {
            value = value.substring(FILE_PREFIX.length()); // remove the prefix

            try (BufferedReader bfr = Files.newBufferedReader(Paths.get(value), DEFAULT_CHARSET)) { // create reader
                // appends every line after another
                decoded = bfr.lines().reduce("", (partial, next) -> partial + NEW_LINE + next);
                if (decoded.length() == 0)
                    log.warn("Key '{}' is empty", value);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                decoded = null;
            }
        }

        log.error("\n\n decodeFileValue - decoded:{}", decoded);
        return decoded;

    }
}
