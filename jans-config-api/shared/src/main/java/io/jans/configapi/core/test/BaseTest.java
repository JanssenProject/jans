/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.test;

import io.jans.as.model.common.GrantType;
import io.jans.as.model.util.Util;
import io.jans.configapi.core.test.service.HttpService;
import io.jans.configapi.core.test.service.ResteasyService;
import io.jans.configapi.core.test.service.TokenService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.net.URLEncoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;


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
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        log.info("Invoked initTestSuite propertiesFile '{}'", propertiesFile);
        propertiesMap = context.getSuite().getXmlSuite().getParameters();
        log.info("End initTestSuite propertiesMap: {}", propertiesMap);
    }

    @AfterSuite
    public void finalize() {
        // cleanup
        log.info("After Suite finalize'");
    }

    @BeforeMethod
    public void getAccessToken() throws Exception {
        log.info("getAccessToken - propertiesMap:{}", propertiesMap);
        String tokenUrl = propertiesMap.get("tokenEndpoint");
        String strGrantType = propertiesMap.get("tokenGrantType");
        String clientId = propertiesMap.get("clientId");
        String clientSecret = propertiesMap.get("clientSecret");
        String scopes = propertiesMap.get("scopes");
        GrantType grantType = GrantType.fromString(strGrantType);
        this.accessToken = getToken(tokenUrl, clientId, clientSecret, grantType, scopes);
        log.info("accessToken:{}", accessToken);
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
        log.info("\n\n decodeFileValue");
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

        log.info("\n\n decodeFileValue - decoded:{}", decoded);
        return decoded;
    }
    
    protected boolean isServiceDeployed(String serviceName) {
        log.info("\n\n\n *** Check if  service is deployed - serviceName:{}", serviceName+" *** \n\n\n");
        boolean isDeployed = false;
        try {
            Class.forName(serviceName);
            isDeployed = true;
        } catch (ClassNotFoundException ex) {
            log.error("*** \n\n\n'{}' service is NOT deployed ***\n\n", serviceName);
            return isDeployed;
        }
        return isDeployed;
    }
    
}
