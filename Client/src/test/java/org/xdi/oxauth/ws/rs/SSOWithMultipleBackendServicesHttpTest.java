/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import java.util.Arrays;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Functional tests for SSO with Multiple Backend Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class SSOWithMultipleBackendServicesHttpTest extends BaseTest {

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void sessionWorkFlow1(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("sessionWorkFlow1");

        // Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
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
        String clientSecret = registerResponse.getClientSecret();

        String state1 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(
                Arrays.asList(ResponseType.CODE),
                clientId,
                Arrays.asList("openid", "profile", "email"),
                redirectUri,
                null);

        authorizationRequest1.setState(state1);
        authorizationRequest1.setRequestSessionId(true);

        AuthorizationResponse authorizationResponse1 = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest1, userId, userSecret);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse1.getSessionId(), "The session id is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertEquals(authorizationResponse1.getState(), state1);

        String code1 = authorizationResponse1.getCode();
        String sessionId = authorizationResponse1.getSessionId();

        // TV sends the code to the Backend
        // We don't use httpClient and cookieStore during this call

        ////////////////////////////////////////////////
        //             Backend  1 side. Code 1        //
        ////////////////////////////////////////////////

        // Get the access token
        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse1 = tokenClient1.execAuthorizationCode(code1, redirectUri, clientId, clientSecret);

        showClient(tokenClient1);
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        String accessToken1 = tokenResponse1.getAccessToken();

        // Get the user's claims
        UserInfoClient userInfoClient1 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse1 = userInfoClient1.execUserInfo(accessToken1);

        showClient(userInfoClient1);
        assertEquals(userInfoResponse1.getStatus(), 200, "Unexpected response code: " + userInfoResponse1.getStatus());
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.SUBJECT_IDENTIFIER), "Unexpected result: subject not found");
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NAME), "Unexpected result: name not found");
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GIVEN_NAME), "Unexpected result: given_name not found");
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.FAMILY_NAME), "Unexpected result: family_name not found");
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL), "Unexpected result: email not found");


        ////////////////////////////////////////////////
        //             TV side. Code 2                //
        ////////////////////////////////////////////////

        String state2 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(
                Arrays.asList(ResponseType.CODE),
                clientId,
                Arrays.asList("openid", "profile", "email"),
                redirectUri,
                null);

        authorizationRequest2.getPrompts().add(Prompt.NONE);
        authorizationRequest2.setState(state2);
        authorizationRequest2.setSessionId(sessionId);

        AuthorizeClient authorizeClient2 = new AuthorizeClient(authorizationEndpoint);
        authorizeClient2.setRequest(authorizationRequest2);
        AuthorizationResponse authorizationResponse2 = authorizeClient2.exec();

        showClient(authorizeClient2);
        assertEquals(authorizationResponse2.getStatus(), 302, "Unexpected response code: " + authorizationResponse2.getStatus());
        assertNotNull(authorizationResponse2.getLocation(), "The location is null");
        assertNotNull(authorizationResponse2.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse2.getScope(), "The scope is null");
        assertNotNull(authorizationResponse2.getState(), "The state is null");
        assertEquals(authorizationResponse2.getState(), state2);

        String code2 = authorizationResponse2.getCode();

        // TV sends the code to the Backend
        // We don't use httpClient and cookieStore during this call

        ////////////////////////////////////////////////
        //             Backend  2 side. Code 2        //
        ////////////////////////////////////////////////

        // Get the access token
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execAuthorizationCode(code2, redirectUri, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");

        String accessToken2 = tokenResponse2.getAccessToken();

        // Get the user's claims
        UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse2 = userInfoClient2.execUserInfo(accessToken2);

        showClient(userInfoClient2);
        assertEquals(userInfoResponse2.getStatus(), 200, "Unexpected response code: " + userInfoResponse2.getStatus());
        assertNotNull(userInfoResponse2.getClaim(JwtClaimName.SUBJECT_IDENTIFIER), "Unexpected result: subject not found");
        assertNotNull(userInfoResponse2.getClaim(JwtClaimName.NAME), "Unexpected result: name not found");
        assertNotNull(userInfoResponse2.getClaim(JwtClaimName.GIVEN_NAME), "Unexpected result: given_name not found");
        assertNotNull(userInfoResponse2.getClaim(JwtClaimName.FAMILY_NAME), "Unexpected result: family_name not found");
        assertNotNull(userInfoResponse2.getClaim(JwtClaimName.EMAIL), "Unexpected result: email not found");
    }

    @Parameters({"redirectUris", "redirectUri", "userInum", "userEmail", "sectorIdentifierUri"})
    @Test
    public void sessionWorkFlow2(
            final String redirectUris, final String redirectUri, final String userInum, final String userEmail,
            final String sectorIdentifierUri) throws Exception {
        showTitle("sessionWorkFlow2");

        // Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
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
        String clientSecret = registerResponse.getClientSecret();

        // Authorization code flow to authenticate on B1

        String state1 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(
                Arrays.asList(ResponseType.CODE),
                clientId,
                Arrays.asList("openid", "profile", "email"),
                redirectUri,
                null);

        authorizationRequest1.addCustomParameter("mail", userEmail);
        authorizationRequest1.addCustomParameter("inum", userInum);
        authorizationRequest1.getPrompts().add(Prompt.NONE);
        authorizationRequest1.setState(state1);
        authorizationRequest1.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);
        authorizationRequest1.setRequestSessionId(true);

        AuthorizationResponse authorizationResponse1 = authorizationRequestAndGrantAccess(
                authorizationEndpoint, authorizationRequest1);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse1.getSessionId(), "The session id is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertEquals(authorizationRequest1.getState(), state1);

        String authorizationCode1 = authorizationResponse1.getCode();
        String sessionId = authorizationResponse1.getSessionId();

        TokenRequest tokenRequest1 = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest1.setCode(authorizationCode1);
        tokenRequest1.setRedirectUri(redirectUri);
        tokenRequest1.setAuthUsername(clientId);
        tokenRequest1.setAuthPassword(clientSecret);
        tokenRequest1.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest1);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        // User wants to authenticate on B2 (without sending its credentials)

        String state2 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(
                Arrays.asList(ResponseType.CODE),
                clientId,
                Arrays.asList("openid", "profile", "email"),
                redirectUri,
                null);

        authorizationRequest2.getPrompts().add(Prompt.NONE);
        authorizationRequest2.setState(state2);
        authorizationRequest2.setSessionId(sessionId);

        AuthorizeClient authorizeClient2 = new AuthorizeClient(authorizationEndpoint);
        authorizeClient2.setRequest(authorizationRequest2);
        AuthorizationResponse authorizationResponse2 = authorizeClient2.exec();

        showClient(authorizeClient2);
        assertEquals(authorizationResponse2.getStatus(), 302, "Unexpected response code: " + authorizationResponse2.getStatus());
        assertNotNull(authorizationResponse2.getLocation(), "The location is null");
        assertNotNull(authorizationResponse2.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse2.getScope(), "The scope is null");
        assertNotNull(authorizationResponse2.getState(), "The state is null");
        assertEquals(authorizationResponse2.getState(), state2);

        String authorizationCode2 = authorizationResponse2.getCode();

        TokenRequest tokenRequest2 = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest2.setCode(authorizationCode2);
        tokenRequest2.setRedirectUri(redirectUri);
        tokenRequest2.setAuthUsername(clientId);
        tokenRequest2.setAuthPassword(clientSecret);
        tokenRequest2.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        tokenClient2.setRequest(tokenRequest2);
        TokenResponse tokenResponse2 = tokenClient2.exec();

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");

        // User wants to authenticate on B3 (without sending its credentials)

        String state3 = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest3 = new AuthorizationRequest(
                Arrays.asList(ResponseType.CODE),
                clientId,
                Arrays.asList("openid", "profile", "email"),
                redirectUri,
                null);

        authorizationRequest3.getPrompts().add(Prompt.NONE);
        authorizationRequest3.setState(state3);
        authorizationRequest3.setSessionId(sessionId);

        AuthorizeClient authorizeClient3 = new AuthorizeClient(authorizationEndpoint);
        authorizeClient3.setRequest(authorizationRequest3);
        AuthorizationResponse authorizationResponse3 = authorizeClient3.exec();

        showClient(authorizeClient3);
        assertEquals(authorizationResponse3.getStatus(), 302, "Unexpected response code: " + authorizationResponse3.getStatus());
        assertNotNull(authorizationResponse3.getLocation(), "The location is null");
        assertNotNull(authorizationResponse3.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse3.getScope(), "The scope is null");
        assertNotNull(authorizationResponse3.getState(), "The state is null");
        assertEquals(authorizationResponse3.getState(), state3);

        String authorizationCode3 = authorizationResponse3.getCode();

        TokenRequest tokenRequest3 = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest3.setCode(authorizationCode3);
        tokenRequest3.setRedirectUri(redirectUri);
        tokenRequest3.setAuthUsername(clientId);
        tokenRequest3.setAuthPassword(clientSecret);
        tokenRequest3.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient3 = new TokenClient(tokenEndpoint);
        tokenClient3.setRequest(tokenRequest3);
        TokenResponse tokenResponse3 = tokenClient3.exec();

        showClient(tokenClient3);
        assertEquals(tokenResponse3.getStatus(), 200, "Unexpected response code: " + tokenResponse3.getStatus());
        assertNotNull(tokenResponse3.getEntity(), "The entity is null");
        assertNotNull(tokenResponse3.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse3.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse3.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse3.getRefreshToken(), "The refresh token is null");
    }
}