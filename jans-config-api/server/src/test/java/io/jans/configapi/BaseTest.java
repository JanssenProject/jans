/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.configapi.service.ResteasyService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.model.net.HttpServiceResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.charset.Charset;
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

import jakarta.servlet.http.HttpServletRequest;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;

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

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
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

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final long serialVersionUID = -2398422090669045605L;
    protected Logger log = LogManager.getLogger(getClass());
    private Base64 base64;
    private static Map<String, String> propertiesMap = null;
    private ResteasyService resteasyService;

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {

        resteasyService = new ResteasyService();
        log.info("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        Properties prop = new Properties();
        prop.load(Files.newBufferedReader(Paths.get(propertiesFile), UTF_8));

        propertiesMap = new Hashtable<>();
        prop.forEach((key, value) -> propertiesMap.put(key.toString(), value.toString()));
        context.getSuite().getXmlSuite().setParameters(propertiesMap);

    }

    @AfterSuite
    public void finalize() {
        // cleanup
        log.info("After Suite finalize'");
    }

    @BeforeTest
    public String getAccessToken() throws Exception {
        String tokenUrl = propertiesMap.get("token.endpoint");
        String strGrantType = propertiesMap.get("token.grant.type");
        String clientId = propertiesMap.get("test.client.id");
        String clientSecret = propertiesMap.get("test.client.secret");
        String scopes = propertiesMap.get("test.scopes");
        String authStr = clientId + ':' + clientSecret;

        String token = new String(Base64.decodeBase64(authStr), StandardCharsets.UTF_8);
        String encodedScopes = URLDecoder.decode(scopes, "UTF-8");
        GrantType grantType = GrantType.fromString(strGrantType);
        String accessToken = getToken(tokenUrl, clientId, clientSecret, grantType, scopes);
       return accessToken;
    }
    
    public String getToken(final String tokenUrl, final String clientId, final String clientSecret,
            GrantType grantType, final String scopes) {
        log.info("Request for token tokenUrl:{}, clientId:{}, grantType:{}, scopes:{}", tokenUrl, clientId, grantType, scopes);
        String accessToken = null;
        TokenResponse tokenResponse = this.requestAccessToken(tokenUrl, clientId, clientSecret, grantType, scopes);
        if (tokenResponse != null) {
            accessToken = tokenResponse.getAccessToken();
            log.trace("accessToken:{}, ", accessToken);           
        }

        return accessToken;
    }

    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            GrantType grantType, final String scope) {
        log.info("Request for access token tokenUrl:{}, clientId:{},scope:{}", tokenUrl, clientId, scope);
        Response response = null;
        try {
            if (grantType==null) {
                grantType = GrantType.CLIENT_CREDENTIALS;
            }
            TokenRequest tokenRequest = new TokenRequest(grantType);
            tokenRequest.setScope(scope);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            Builder request = resteasyService.getClientBuilder(tokenUrl);
            request.header(AUTHORIZATION, "Basic " + tokenRequest.getEncodedCredentials());
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>(
                    tokenRequest.getParameters());
            response = request.post(Entity.form(multivaluedHashMap));
            log.trace("Response for Access Token -  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                TokenResponse tokenResponse = new TokenResponse();
                tokenResponse.setEntity(entity);
                tokenResponse.injectDataFromJson(entity);
                return tokenResponse;
            }
        } finally {

            if (response != null) {
                response.close();
            }
        }
        return null;
    }

}
