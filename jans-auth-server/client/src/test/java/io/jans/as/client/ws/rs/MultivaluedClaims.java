/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoRequest;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.HMACSigner;
import io.jans.as.model.jws.PlainTextSignature;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Note: In order to run this tests, set legacyIdTokenClaims to true.
 *
 * @author Javier Rojas Blum
 * @version March 8, 2019
 */
public class MultivaluedClaims extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimNone(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimNone");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.NONE);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.NONE);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignaturePlainText()
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureHMAC(SignatureAlgorithm.HS256, clientSecret)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimHS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimHS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureHMAC(SignatureAlgorithm.HS384, clientSecret)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureHMAC(SignatureAlgorithm.HS512, clientSecret)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
        assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
        assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS384)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
        assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
        assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS512)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
        assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
        assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES256)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES384)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES512)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS256)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS384)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS512)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA_OAEP_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestWithMultivaluedClaimAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestWithMultivaluedClaimAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimNone(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimNone");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.NONE);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.NONE);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.NONE);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = createCryptoProviderWithAllowedNone();

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.NONE, null, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignaturePlainText()
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureHMAC(SignatureAlgorithm.HS256, clientSecret)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimHS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimHS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS384);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS384, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureHMAC(SignatureAlgorithm.HS384, clientSecret)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS512);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS512, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureHMAC(SignatureAlgorithm.HS512, clientSecret)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
        assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
        assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);


        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS384_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS384)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
        assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
        assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS512_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS512)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
        assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
        assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "ES256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.ES256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES256)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "ES384_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.ES384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES384)
                .claimMemberOfNoEmpty()
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "ES512_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.ES512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        ECDSAPublicKey publicKey = JwkClient.getECDSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        ECDSASigner ecdsaSigner = new ECDSASigner(SignatureAlgorithm.ES512, publicKey);

        assertTrue(ecdsaSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "PS256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.PS256, publicKey);

        assertTrue(rsaSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "PS384_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS384)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "PS512_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token

        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS512)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest,
                KeyEncryptionAlgorithm.A128KW,
                BlockEncryptionAlgorithm.A128GCM,
                clientSecret);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest,
                KeyEncryptionAlgorithm.A256KW,
                BlockEncryptionAlgorithm.A256GCM,
                clientSecret);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 3. Validate id_token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "dnName", "keyStoreFile", "keyStoreSecret", "RSA1_5_keyId",
            "clientJwksUri", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String clientKeyId,
            final String clientJwksUri, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Choose encryption key
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
        assertNotNull(serverKeyId);

        // 3. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(serverKeyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt(jwks);
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 4. Validate id_token
        PrivateKey privateKey = cryptoProvider.getPrivateKey(clientKeyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 5. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "dnName", "keyStoreFile", "keyStoreSecret", "RSA1_5_keyId",
            "clientJwksUri", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String clientKeyId,
            final String clientJwksUri, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Choose encryption key
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
        assertNotNull(serverKeyId);

        // 3. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(serverKeyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt(jwks);
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 4. Validate id_token
        PrivateKey privateKey = cryptoProvider.getPrivateKey(clientKeyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 5. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "dnName", "keyStoreFile", "keyStoreSecret", "RSA_OAEP_keyId",
            "clientJwksUri", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectWithMultivaluedClaimAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String clientKeyId,
            final String clientJwksUri, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectWithMultivaluedClaimAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setClaims(Arrays.asList("member_of"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Choose encryption key
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        // 3. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(serverKeyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("member_of", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt(jwks);
        authorizationRequest.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();
        String accessToken = authorizationResponse.getAccessToken();

        // 4. Validate id_token
        PrivateKey privateKey = cryptoProvider.getPrivateKey(clientKeyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .claimMemberOfNoEmpty()
                .check();

        // 5. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim("member_of"));
        assertTrue(userInfoResponse.getClaim("member_of").size() > 1);
    }
}