/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Functional tests for End Session Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version November 30, 2016
 */
public class EndSessionRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "postLogoutRedirectUri", "logoutUri", "sectorIdentifierUri"})
    @Test
    public void requestEndSession(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String postLogoutRedirectUri, final String logoutUri, final String sectorIdentifierUri) throws Exception {
        showTitle("requestEndSession");

        // 1. OpenID Connect Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setLogoutUris(Lists.newArrayList(logoutUri));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);
        List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The access token is null");
        assertEquals(authorizationResponse.getState(), state);
        assertNotNull(authorizationResponse.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse.getScope(), "The scope must be null");
        assertNotNull(authorizationResponse.getSessionState(), "The session_state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. End session
        String endSessionState = UUID.randomUUID().toString();
        EndSessionRequest endSessionRequest = new EndSessionRequest(idToken, postLogoutRedirectUri, endSessionState);
        endSessionRequest.setSessionState(authorizationResponse.getSessionState());

        EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
        endSessionClient.setRequest(endSessionRequest);

        EndSessionResponse endSessionResponse = endSessionClient.exec();

        showClient(endSessionClient);
        assertEquals(endSessionResponse.getStatus(), 200, "Unexpected response code: " + endSessionResponse.getStatus());
        assertNotNull(endSessionResponse.getHtmlPage(), "The HTML page is null");

        // silly validation of html content returned by server but at least it verifies that logout_uri and post_logout_uri are present
        assertTrue(endSessionResponse.getHtmlPage().contains("<html>"), "The HTML page is null");
        assertTrue(endSessionResponse.getHtmlPage().contains(logoutUri), "logout_uri is not present on html page");
        assertTrue(endSessionResponse.getHtmlPage().contains(postLogoutRedirectUri), "postLogoutRedirectUri is not present on html page");
        // assertEquals(endSessionResponse.getState(), endSessionState); // commented out, for http-based logout we get html page
    }

    @Test
    public void requestEndSessionFail1() throws Exception {
        showTitle("requestEndSessionFail1");

        EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
        EndSessionResponse response = endSessionClient.execEndSession(null, null, null);

        showClient(endSessionClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"postLogoutRedirectUri"})
    @Test
    public void requestEndSessionFail2(final String postLogoutRedirectUri) throws Exception {
        showTitle("requestEndSessionFail2");

        String state = UUID.randomUUID().toString();

        EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
        EndSessionResponse response = endSessionClient.execEndSession("INVALID_ACCESS_TOKEN", postLogoutRedirectUri, state);

        showClient(endSessionClient);
        assertEquals(response.getStatus(), 401, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }
}