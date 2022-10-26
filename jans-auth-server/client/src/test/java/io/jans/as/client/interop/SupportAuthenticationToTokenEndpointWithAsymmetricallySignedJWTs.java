/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

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

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class SupportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsRS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (RS256)");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsRS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (RS384)");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsRS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (RS512)");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsES256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (ES256)");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsES384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (ES384)");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri", "clientJwksUri",
            "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsES512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (ES512)");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }
}