/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui;

import io.jans.configapi.core.test.BaseTest;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import jakarta.ws.rs.core.Response;

import org.apache.http.entity.ContentType;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class AdminUIBaseTest extends BaseTest {

    protected boolean isAvailable(final String url, Map<String, String> headers,
            final Map<String, String> parameters) {
        return isEndpointAvailable(url, headers, parameters);
    }

    // Execute before each test is run
    @BeforeMethod
    protected void before() {
        boolean isAvailable = isAvailable(propertiesMap.get("auditLoggingURL"), null, null);
        log.error("\n\n\n *** ADMIN-UI Plugin isAvailable:{} {}", isAvailable, "\n\n\n");
        // check condition, note once you condition is met the rest of the tests will be

        // skipped as well
        if (!isAvailable) {
            throw new SkipException("ADMIN-UI Plugin Not deployed");
        } else {
            log.error("\n\n\n *** ADMIN-UI Plugin is Deployed{} {}", "\n\n");
        }
    }

    public void authorize() {
        log.info("AdminUI - getAccessToken - propertiesMap:{}", propertiesMap);

        String authzurl = propertiesMap.get("test.authzurl");
        String strGrantType = propertiesMap.get("test.grant.type");
        String clientId = propertiesMap.get("test.client.id");
        String clientSecret = propertiesMap.get("test.client.secret");
        String scopes = propertiesMap.get("test.scopes");
        String responseType = propertiesMap.get("test.response.type");
        String redirectUri = propertiesMap.get("test.redirect.uri");
        log.error(
                "\n\n\n\n ************ AdminUI- authzurl:{}, strGrantType:{}, clientId:{},  clientSecret:{}, scopes:{}, responseType:{}, propertiesMap.get(auditLoggingURL)",
                authzurl, strGrantType, clientId, clientSecret, scopes, responseType,
                propertiesMap.get("auditLoggingURL"));
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("scope", scopes);
        params.put("grant_type", strGrantType);
        params.put("response_type", responseType);
        params.put("redirect_uri", redirectUri);
        Response response = authorize(authzurl, clientId, clientSecret, null, getAuthCode(clientId, clientSecret),
                params, null);
        log.info("\n\n\n\n AdminUI- response:{} :{}", response, "**********");
    }

    private String getAuthCode(final String clientId, final String clientSecret) {
        String code = null;
        try {
            code = getCredentials(clientId, clientSecret);
        } catch (UnsupportedEncodingException ex) {
            log.error("Error while encoding credentials is ", ex);
        }
        return code;
    }

    private Response authorize(final String authzurl, final String clientId, final String clientSecret,
            final String authType, final String authCode, final Map<String, String> parameters,
            ContentType contentType) {
        return executeGet(authzurl, clientId, clientSecret, authType, authCode, parameters, contentType);
    }

}
