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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.net.URLEncoder;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.testng.ITestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public class BaseTest {

    private static final String FILE_PREFIX = "file:";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
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
    public void initTestSuite(ITestContext context) {
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        log.info("Invoked initTestSuite propertiesFile '{}'", propertiesFile);
        propertiesMap = context.getSuite().getXmlSuite().getParameters();
        log.info("End initTestSuite propertiesMap: {}", propertiesMap);
    }

    @BeforeMethod
    public void getAccessToken() {
        log.info("getAccessToken - propertiesMap:{}", propertiesMap);
        String tokenUrl = propertiesMap.get("token.endpoint");
        String strGrantType = propertiesMap.get("token.grant.type");
        String clientId = propertiesMap.get("test.client.id");
        String clientSecret = propertiesMap.get("test.client.secret");
        String scopes = propertiesMap.get("test.scopes");
        GrantType grantType = GrantType.fromString(strGrantType);
        this.accessToken = getToken(tokenUrl, clientId, clientSecret, grantType, scopes);
        log.info("\n\n accessToken:{}", accessToken);
    }

    protected String getToken(final String tokenUrl, final String clientId, final String clientSecret,
            GrantType grantType, final String scopes) {
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
            String clientId = propertiesMap.get("test.client.id");
            String clientSecret = propertiesMap.get("test.client.secret");
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
        log.info("\n\n\n *** Check if  service is deployed - serviceName:{} :{}", serviceName , " *** \n\n\n");
        boolean isDeployed = false;
        try {
            Class.forName(serviceName);
            isDeployed = true;
            log.error("*** \n\n\n'{}' service is deployed :{} {}", serviceName,"\n\n\n");
        } catch (ClassNotFoundException ex) {
            log.error("*** \n\n\n'{}' service is NOT deployed :{} {}", serviceName,"\n\n\n");
            isDeployed = false;
            return isDeployed;
        }
        return isDeployed;
    }

    protected Response executeGet(final String url, final String clientId, final String clientSecret,
            final String authType, final String authCode, final Map<String, String> parameters,
            ContentType contentType) {
        log.info(
                "Data for executing GET request -  url:{}, clientId:{}, clientSecret:{} , authType:{}, authCode:{} , parameters:{}, contentType:{}",
                url, clientId, clientSecret, authType, authCode, parameters, contentType);
        return getResteasyService().executeGet(url, clientId, clientSecret, authType, authCode, parameters,
                contentType);
    }

}
