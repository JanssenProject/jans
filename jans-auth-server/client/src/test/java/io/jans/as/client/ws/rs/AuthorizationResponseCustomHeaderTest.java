/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.*;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;


import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version February 2, 2022
 */
public class AuthorizationResponseCustomHeaderTest extends BaseTest {

    @Test(dataProvider = "requestAuthorizationCustomHeaderDataProvider")
    public void requestAuthorizationCustomHeader(
            final List<ResponseType> responseTypes, final String userId, final String userSecret,
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("AuthorizationResponseCustomHeaderTest");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        Map<String, String> customResponseHeaders = new HashMap<String, String>();
        customResponseHeaders.put("CustomHeader1", "custom_header_value_1");
        customResponseHeaders.put("CustomHeader2", "custom_header_value_2");
        customResponseHeaders.put("CustomHeader3", "custom_header_value_3");

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setCustomResponseHeaders(customResponseHeaders);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302);
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getHeaders());
        assertTrue(authorizationResponse.getHeaders().containsKey("CustomHeader1"));
        assertTrue(authorizationResponse.getHeaders().containsKey("CustomHeader2"));
        assertTrue(authorizationResponse.getHeaders().containsKey("CustomHeader3"));
        assertTrue(authorizationResponse.getHeaders().get("CustomHeader1").contains("custom_header_value_1"));
        assertTrue(authorizationResponse.getHeaders().get("CustomHeader2").contains("custom_header_value_2"));
        assertTrue(authorizationResponse.getHeaders().get("CustomHeader3").contains("custom_header_value_3"));
    }

    @DataProvider(name = "requestAuthorizationCustomHeaderDataProvider")
    public Object[][] omittedResponseTypesFailDataProvider(ITestContext context) {
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUris = context.getCurrentXmlTest().getParameter("redirectUris");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");
        String sectorIdentifierUri = context.getCurrentXmlTest().getParameter("sectorIdentifierUri");

        return new Object[][]{
                {Arrays.asList(ResponseType.CODE), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
                {Arrays.asList(ResponseType.TOKEN), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
                {Arrays.asList(ResponseType.ID_TOKEN), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
                {Arrays.asList(ResponseType.CODE, ResponseType.TOKEN), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
                {Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
                {Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
                {Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN), userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri},
        };
    }
}
