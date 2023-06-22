/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * OC5:FeatureTest-Can Make Access Token Request with private key jwt Authentication
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class CanMakeAccessTokenRequestWithPrivateKeyJwtAuthentication extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void canMakeAccessTokenRequestWithPrivateKeyJwtAuthentication(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Can Make Access Token Request with private key jwt Authentication");

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setScope(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullRefreshToken()
                .check();
    }
}