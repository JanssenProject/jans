/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.jarm;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TestCryptoContext;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.JwtClaimName;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @version August 26, 2021
 */
public class AuthorizationResponseModeJwtResponseTypeCodeSignedHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testDefault(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testDefault");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                null, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testHS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.HS256, null, null);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testHS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testHS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.HS384, null, null);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testHS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.HS512, null, null);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testRS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.RS256, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testRS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.RS384, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testRS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.RS512, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testES256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.ES256, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testES384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.ES384, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testES512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.ES512, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testPS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.PS256, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testPS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.PS384, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testPS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                SignatureAlgorithm.PS512, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.QUERY_JWT, clientId, scopes, redirectUri, null,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS256");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.RS256, SignatureAlgorithm.RS256);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS384");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.RS384, SignatureAlgorithm.RS384);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS512");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.RS512, SignatureAlgorithm.RS512);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES256");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.ES256, SignatureAlgorithm.ES256);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES384");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.ES384, SignatureAlgorithm.ES384);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES512");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.ES512, SignatureAlgorithm.ES512);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS256");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.PS256, SignatureAlgorithm.PS256);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS256");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.PS384, SignatureAlgorithm.PS384);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS256");
        authorizationRequestObjectSigned(userId, userSecret, redirectUri, redirectUris, sectorIdentifierUri,
                Algorithm.PS512, SignatureAlgorithm.PS512);
    }

    private void authorizationRequestObjectSigned(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri, final Algorithm algorithm, final SignatureAlgorithm signatureAlgorithm) throws Exception {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri,
                signatureAlgorithm, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();
        String keyId = cryptoContext.getKeyId(algorithm);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setResponseMode(ResponseMode.JWT);
        request.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, signatureAlgorithm, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        authorizationRequest(request, ResponseMode.QUERY_JWT, userId, userSecret);
    }
}
