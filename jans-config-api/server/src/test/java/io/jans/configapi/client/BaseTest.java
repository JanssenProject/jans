/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.client.dev.HostnameVerifierType;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;


import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;


public class BaseTest {

    protected Logger logger = LogManager.getLogger(getClass());
    protected ObjectMapper mapper = new ObjectMapper();
    private static Map<String, String> propertiesMap = null;
    private ApacheHttpClient43Engine engine;


    @BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {

        logger.info("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        Properties prop = new Properties();
        prop.load(Files.newBufferedReader(Paths.get(propertiesFile), UTF_8));

        propertiesMap = new Hashtable<>();
        prop.forEach((key, value) -> propertiesMap.put(key.toString(), value.toString()));
        context.getSuite().getXmlSuite().setParameters(propertiesMap);
        
        this.engine = createEngine();
    }

    @AfterSuite
    public void finalize() {
        //cleanup
        logger.info("After Suite finalize'");
    }


    public String getAccessToken() throws Exception {
        String tokenUrl =  propertiesMap.get("token.endpoint");
        String grantType =  propertiesMap.get("token.grant.type");
        String clientId =  propertiesMap.get("test.client.id");
        String clientSecret =  propertiesMap.get("test.client.secret");
        String scopes =  propertiesMap.get("test.scopes");
        String authStr = clientId+':'+clientSecret;

        String token = new String(Base64.decodeBase64(authStr), StandardCharsets.UTF_8);
        String encodedScopes = URLDecoder.decode(scopes, "UTF-8");
        
        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(tokenUrl));

       
    }
    


    
    
    private ApacheHttpClient43Engine createEngine() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        engine.setFollowRedirects(true);
        
        return engine;
    }
}
