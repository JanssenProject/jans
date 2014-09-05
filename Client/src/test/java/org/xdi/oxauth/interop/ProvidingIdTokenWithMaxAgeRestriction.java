/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

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
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * OC5:FeatureTest-Providing ID Token with max age Restriction
 *
 * @author Javier Rojas Blum Date: 07.23.2013
 */
public class ProvidingIdTokenWithMaxAgeRestriction extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void providingIdTokenWithMaxAgeRestriction(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Providing ID Token with max age Restriction");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
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

        String sessionId = null;
        {
            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = "STATE_XYZ";

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getIdToken());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        }

        Thread.sleep(60000);

        {
            // 4. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = "STATE_XYZ";

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setMaxAge(30);
            authorizationRequest.setSessionId(sessionId);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getIdToken());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());

            String authorizationCode = authorizationResponse.getCode();

            // 5. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void providingIdTokenWithMaxAgeRestrictionJwtAuthorizationRequest(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Providing ID Token with max age Restriction (JWT Authorization Request)");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
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

        String sessionId = null;
        {
            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = "STATE_XYZ";

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getIdToken());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        }

        Thread.sleep(60000);

        {
            // 4. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = "STATE_XYZ";

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret);
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(30);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getIdToken());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());

            String authorizationCode = authorizationResponse.getCode();

            // 5. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        }
    }
}