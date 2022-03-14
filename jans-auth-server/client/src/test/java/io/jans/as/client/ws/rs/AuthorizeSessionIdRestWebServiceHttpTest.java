/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Functional tests for checking Sessions in Authorize Web Services workflow (HTTP)
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class AuthorizeSessionIdRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestSessionIdAuthorizationCode1(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestSessionIdAuthorizationCode1");

        requestSessionIdAuthorizationCode(userId, userSecret, redirectUris, redirectUri, authorizationEndpoint,
                authorizationEndpoint, sectorIdentifierUri);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestSessionIdAuthorizationCode2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestSessionIdAuthorizationCode2");

        requestSessionIdAuthorizationCode(userId, userSecret, redirectUris, redirectUri, authorizationPageEndpoint,
                authorizationEndpoint, sectorIdentifierUri);
    }

    private void requestSessionIdAuthorizationCode(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String authorizationEndpoint1, final String authorizationEndpoint2, final String sectorIdentifierUri) {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization but not enter credentials.
        // Store session_id parameter value
        List<String> scopes1 = Arrays.asList("openid", "profile", "address", "email");
        String state1 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes1, redirectUri, null);
        authorizationRequest1.setState(state1);
        String sessionId = waitForResourceOwnerAndGrantLoginForm(authorizationEndpoint1, authorizationRequest1, false);
        assertNotNull(sessionId, "The session_id is null");

        // 4. Request authorization and receive the authorization code.
        // Application should returns new session_id
        List<String> scopes2 = Arrays.asList("openid", "profile", "address", "email");
        String state2 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(responseTypes, clientId, scopes2, redirectUri, null);
        authorizationRequest2.setState(state2);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint2, authorizationRequest2, userId, userSecret, false);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNotEquals(sessionId, authorizationResponse.getSessionId(), "The session_id is the same for 2 different authorization requests");
    }

}