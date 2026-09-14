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
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class SupportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsRS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
                "OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (RS256)",
                redirectUris, redirectUri, userId, userSecret, sectorIdentifierUri,
                Algorithm.RS256, SignatureAlgorithm.RS256);
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsRS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
                "OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (RS384)",
                redirectUris, redirectUri, userId, userSecret, sectorIdentifierUri,
                Algorithm.RS384, SignatureAlgorithm.RS384);
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsRS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
                "OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (RS512)",
                redirectUris, redirectUri, userId, userSecret, sectorIdentifierUri,
                Algorithm.RS512, SignatureAlgorithm.RS512);
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsES256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
                "OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (ES256)",
                redirectUris, redirectUri, userId, userSecret, sectorIdentifierUri,
                Algorithm.ES256, SignatureAlgorithm.ES256);
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsES384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
                "OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (ES384)",
                redirectUris, redirectUri, userId, userSecret, sectorIdentifierUri,
                Algorithm.ES384, SignatureAlgorithm.ES384);
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTsES512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
                "OC5:FeatureTest-Support Authentication to Token Endpoint with Asymmetrically Signed JWTs (ES512)",
                redirectUris, redirectUri, userId, userSecret, sectorIdentifierUri,
                Algorithm.ES512, SignatureAlgorithm.ES512);
    }

    private void supportAuthenticationToTokenEndpointWithAsymmetricallySignedJWTs(
            final String title, final String redirectUris, final String redirectUri, final String userId,
            final String userSecret, final String sectorIdentifierUri,
            final Algorithm algorithm, final SignatureAlgorithm signatureAlgorithm) throws Exception {
        showTitle(title);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();
        String keyId = cryptoContext.getKeyId(algorithm);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
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

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(signatureAlgorithm);
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
