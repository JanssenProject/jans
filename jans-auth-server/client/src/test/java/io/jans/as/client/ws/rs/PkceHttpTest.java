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
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.authorize.CodeVerifier;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version March 3, 2017
 */

public class PkceHttpTest extends BaseTest {

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void tokenWithPkceCheck(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenWithPkceCheck");

        // 1. Register client
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        // 3. Request authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        CodeVerifier verifier = authorizationRequest.generateAndSetCodeChallengeWithMethod();

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The ID Token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 5. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(registerResponse.getClientId());
        tokenRequest.setAuthPassword(registerResponse.getClientSecret());
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.setCodeVerifier(verifier.getCodeVerifier());

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void invalidCodeVerifier(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("invalidCodeVerifier");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.generateAndSetCodeChallengeWithMethod(); // PKCE is set !!!

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token with invalid code verifier
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(registerResponse.getClientId());
        tokenRequest.setAuthPassword(registerResponse.getClientSecret());
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.setCodeVerifier("invalid_code_verifier");

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 400, "Unexpected response code: " + tokenResponse.getStatus());
        assertNull(tokenResponse.getAccessToken(), "The access token is null");

        // 5. Get Access Token without code verifier
        tokenRequest.setCodeVerifier(null);

        tokenClient.setRequest(tokenRequest);
        tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 400, "Unexpected response code: " + tokenResponse.getStatus());
        assertNull(tokenResponse.getAccessToken(), "The access token is null");

    }
}
