/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.xdi.oxauth.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static org.xdi.oxauth.model.register.RegisterRequestParam.CLIENT_NAME;
import static org.xdi.oxauth.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REDIRECT_URIS;
import static org.xdi.oxauth.model.register.RegisterRequestParam.RESPONSE_TYPES;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * Functional tests for checking Sessions in Authorize Web Services workflow (HTTP) 
 *
 * @author Yuriy Movchan
 * @version 0.1, 12/21/2015
 */
public class AuthorizeSessionStateRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void requestSessionStateAuthorizationCode1(
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri) throws Exception {
		showTitle("requestSessionStateAuthorizationCode1");

        requestSessionStateAuthorizationCode(userId, userSecret, redirectUris, redirectUri, authorizationEndpoint, authorizationEndpoint);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void requestSessionStateAuthorizationCode2(
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri) throws Exception {
		showTitle("requestSessionStateAuthorizationCode2");

        requestSessionStateAuthorizationCode(userId, userSecret, redirectUris, redirectUri, authorizationPageEndpoint, authorizationEndpoint);
    }

	private void requestSessionStateAuthorizationCode(final String userId, final String userSecret, final String redirectUris, final String redirectUri,
			final String authorizationEndpoint1, final String authorizationEndpoint2) {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

        // 3. Request authorization but not enter credentials.
        // Store session_state parameter value
        List<String> scopes1 = Arrays.asList("openid", "profile", "address", "email");
        String state1 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes1, redirectUri, null);
        authorizationRequest1.setState(state1);
        String sessionState = waitForResourceOwnerAndGrantLoginForm(authorizationEndpoint1, authorizationRequest1, false);
        assertNotNull(sessionState, "The sessionState is null");

        // 4. Request authorization and receive the authorization code.
        // Application should returns new session_state
        List<String> scopes2 = Arrays.asList("openid", "profile", "address", "email");
        String state2 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(responseTypes, clientId, scopes2, redirectUri, null);
        authorizationRequest2.setState(state2);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint2, authorizationRequest2, userId, userSecret, false);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNotEquals(sessionState, authorizationResponse.getSessionState(), "The session_state is the same for 2 different authorization requests");
	}

}