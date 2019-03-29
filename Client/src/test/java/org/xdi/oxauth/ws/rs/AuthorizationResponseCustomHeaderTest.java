/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.oxauth.ws.rs;

import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;

import java.util.*;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
public class AuthorizationResponseCustomHeaderTest extends BaseTest {

    @Test(dataProvider = "requestAuthorizationCustomHeaderDataProvider")
    public void requestAuthorizationCustomHeader(
            final List<ResponseType> responseTypes, final String userId, final String userSecret,
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("AuthorizationResponseCustomHeaderTest");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
