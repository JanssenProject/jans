/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import io.jans.util.StringHelper;
import org.json.JSONObject;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Functional tests for OpenID Request Object (HTTP)
 *
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class OpenIDRequestObjectHttpTest extends BaseTest {

    public static final String ACR_VALUE = "basic";

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod1(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod1");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod2");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod3(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod3");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid");
        String state = "STATE0";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("name", ClaimValue.createNull()));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod4(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod4");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS384, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue(userId)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod5(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod5");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS512, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue(userId)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod6(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod6");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);
        registerRequest.setClaims(Arrays.asList(JwtClaimName.NAME));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("name", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS256");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.RS256);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS384");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.RS384);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                request, SignatureAlgorithm.RS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS512");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.RS512);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES256");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.ES256);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES384");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.ES384);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientEngine(true));
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        authorizeClient.setExecutor(clientEngine(true));
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES512");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.ES512);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS256");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.PS256);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.PS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS384");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.PS384);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.PS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS512");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.PS512);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.PS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS256X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS256X509Cert");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.RS256);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS384X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS384X509Cert");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.RS384);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS512X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS512X509Cert");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.RS512);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES256X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES256X509Cert");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.ES256);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES384X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES384X509Cert");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.ES384);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES512X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES512X509Cert");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String keyId = cryptoContext.getKeyId(Algorithm.ES512);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        String clientId = response.getClientId();

        // 2. Request authorization
        AbstractCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodFail1(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodFail1");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            // 2. Authorization Request
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setRequest("INVALID_REQUEST_OBJECT");
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);
            AuthorizationResponse response = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getLocation(), "The location is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            assertNotNull(response.getErrorDescription(), "The error description is null");
            assertNotNull(response.getState(), "The state is null");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodFail2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodFail2");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Authorization Request
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt + "INVALID_KEY");

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);
            AuthorizationResponse response = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getLocation(), "The location is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            assertNotNull(response.getErrorDescription(), "The error description is null");
            assertNotNull(response.getState(), "The state is null");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodFail3(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodFail3");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Authorization Request
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            jwtAuthorizationRequest.setClientId("INVALID_CLIENT_ID");
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getLocation(), "The location is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            assertNotNull(response.getErrorDescription(), "The error description is null");
            assertNotNull(response.getState(), "The state is null");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodFail4(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodFail4");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Authorization Request
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue("INVALID_USER_ID")));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getLocation(), "The location is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            assertNotNull(response.getErrorDescription(), "The error description is null");
            assertNotNull(response.getState(), "The state is null");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "requestFileBasePath", "requestFileBaseUrl", "sectorIdentifierUri"})
    @Test(enabled = false) // This tests requires a place to publish a request object via HTTPS
    public void requestFileMethod(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            @Optional final String requestFileBasePath, final String requestFileBaseUrl, final String sectorIdentifierUri) throws Exception {
        showTitle("requestFileMethod");

        if (StringHelper.isEmpty(requestFileBasePath)) {
            return;
        }

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.standardScopes);
        registerRequest.setJwks(cryptoContext.getJwksAsString());

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Authorization
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        try {
            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String fileName = UUID.randomUUID() + ".txt";
            String filePath = requestFileBasePath + File.separator + fileName;
            String fileUrl = requestFileBaseUrl + "/" + fileName;// + "#" + hash;
            FileWriter fw = new FileWriter(filePath);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(authJwt);
            bw.close();
            fw.close();
            authorizationRequest.setRequestUri(fileUrl);
            System.out.println("Request JWT: " + authJwt);
            System.out.println("Request File Path: " + filePath);
            System.out.println("Request File URL: " + fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestFileMethodFail1(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestFileMethodFail1");

            List<ResponseType> responseTypes = Arrays.asList(
                    ResponseType.TOKEN,
                    ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            // 2. Request Authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            authorizationRequest.setRequest("FAKE_REQUEST");
            authorizationRequest.setRequestUri("FAKE_REQUEST_URI");

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);
            AuthorizationResponse response = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getLocation(), "The location is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            assertNotNull(response.getErrorDescription(), "The error description is null");
            assertNotNull(response.getState(), "The state is null");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodAlgRSAOAEPEncA256GCM");

            TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);
            registerRequest.setJwks(cryptoContext.getJwksAsString());

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).created().check();

            String clientId = response.getClientId();

            // 2. Choose encryption key
            JwkClient jwkClient = new JwkClient(jwksUri);
            JwkResponse jwkResponse = jwkClient.exec();
            String keyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
            assertNotNull(keyId);

            // 3. Request authorization
            JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
            AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            String authJwt = TestCryptoContext.createJweWithNestedJwsAndRsaEncryption(
                    request,
                    SignatureAlgorithm.RS256,
                    cryptoContext.getKeyId(Algorithm.RS256),
                    KeyEncryptionAlgorithm.RSA_OAEP,
                    BlockEncryptionAlgorithm.A256GCM,
                    keyId,
                    jwks,
                    cryptoProvider,
                    Lists.newArrayList(
                            new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE}))
                    ),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                            new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                            new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                            new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                    )
            );
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response1 = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
            assertNotNull(response1.getLocation(), "The location is null");
            assertNotNull(response1.getAccessToken(), "The accessToken is null");
            assertNotNull(response1.getTokenType(), "The tokenType is null");
            assertNotNull(response1.getIdToken(), "The idToken is null");
            assertNotNull(response1.getState(), "The state is null");

            String accessToken = response1.getAccessToken();

            // 4. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(response3)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                    .check();
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodAlgRSA15EncA128CBCPLUSHS256");

            TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);
            registerRequest.setJwks(cryptoContext.getJwksAsString());

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).created().check();

            String clientId = response.getClientId();

            // 2. Choose encryption key
            JwkClient jwkClient = new JwkClient(jwksUri);
            JwkResponse jwkResponse = jwkClient.exec();
            String keyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
            assertNotNull(keyId);

            // 3. Request authorization
            JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
            AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            String authJwt = TestCryptoContext.createJweWithNestedJwsAndRsaEncryption(
                    request,
                    SignatureAlgorithm.RS256,
                    cryptoContext.getKeyId(Algorithm.RS256),
                    KeyEncryptionAlgorithm.RSA1_5,
                    BlockEncryptionAlgorithm.A128CBC_PLUS_HS256,
                    keyId,
                    jwks,
                    cryptoProvider,
                    Lists.newArrayList(
                            new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE}))
                    ),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                            new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                            new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                            new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                    )
            );
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response1 = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
            assertNotNull(response1.getLocation(), "The location is null");
            assertNotNull(response1.getAccessToken(), "The accessToken is null");
            assertNotNull(response1.getTokenType(), "The tokenType is null");
            assertNotNull(response1.getIdToken(), "The idToken is null");
            assertNotNull(response1.getState(), "The state is null");

            String accessToken = response1.getAccessToken();

            // 4. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(response3)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                    .check();
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodAlgRSA15EncA256CBCPLUSHS512");

            TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);
            registerRequest.setJwks(cryptoContext.getJwksAsString());

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).created().check();

            String clientId = response.getClientId();

            // 2. Choose encryption key
            JwkClient jwkClient = new JwkClient(jwksUri);
            JwkResponse jwkResponse = jwkClient.exec();
            String keyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
            assertNotNull(keyId);

            // 3. Request authorization
            JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
            AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            String authJwt = TestCryptoContext.createJweWithNestedJwsAndRsaEncryption(
                    request,
                    SignatureAlgorithm.RS256,
                    cryptoContext.getKeyId(Algorithm.RS256),
                    KeyEncryptionAlgorithm.RSA1_5,
                    BlockEncryptionAlgorithm.A256CBC_PLUS_HS512,
                    keyId,
                    jwks,
                    cryptoProvider,
                    Lists.newArrayList(
                            new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE}))
                    ),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                            new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                            new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                            new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                    )
            );
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response1 = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
            assertNotNull(response1.getLocation(), "The location is null");
            assertNotNull(response1.getAccessToken(), "The accessToken is null");
            assertNotNull(response1.getTokenType(), "The tokenType is null");
            assertNotNull(response1.getIdToken(), "The idToken is null");
            assertNotNull(response1.getState(), "The state is null");

            String accessToken = response1.getAccessToken();

            // 4. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(response3)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                    .check();
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodAlgA128KWEncA128GCM");

            TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);
            registerRequest.setJwks(cryptoContext.getJwksAsString());

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).created().check();

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            String authJwt = TestCryptoContext.createJweWithNestedJws(
                    request,
                    SignatureAlgorithm.RS256,
                    cryptoContext.getKeyId(Algorithm.RS256),
                    KeyEncryptionAlgorithm.A128KW,
                    BlockEncryptionAlgorithm.A128GCM,
                    clientSecret,
                    cryptoContext.getCryptoProvider(),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE}))
                    ),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                            new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                            new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                            new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                    )
            );
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response1 = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
            assertNotNull(response1.getLocation(), "The location is null");
            assertNotNull(response1.getAccessToken(), "The accessToken is null");
            assertNotNull(response1.getTokenType(), "The tokenType is null");
            assertNotNull(response1.getIdToken(), "The idToken is null");
            assertNotNull(response1.getState(), "The state is null");

            String accessToken = response1.getAccessToken();

            // 3. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(response3)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                    .check();
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodAlgA256KWEncA256GCM");


            TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setScope(Tester.standardScopes);
            registerRequest.setJwks(cryptoContext.getJwksAsString());

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).created().check();

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            String authJwt = TestCryptoContext.createJweWithNestedJws(
                    request,
                    SignatureAlgorithm.RS256,
                    cryptoContext.getKeyId(Algorithm.RS256),
                    KeyEncryptionAlgorithm.A256KW,
                    BlockEncryptionAlgorithm.A256GCM,
                    clientSecret,
                    cryptoContext.getCryptoProvider(),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{ACR_VALUE}))
                    ),
                    Lists.newArrayList(
                            new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                            new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                            new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                            new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                            new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                    )
            );
            request.setRequest(authJwt);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(request);
            AuthorizationResponse response1 = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
            assertNotNull(response1.getLocation(), "The location is null");
            assertNotNull(response1.getAccessToken(), "The accessToken is null");
            assertNotNull(response1.getTokenType(), "The tokenType is null");
            assertNotNull(response1.getIdToken(), "The idToken is null");
            assertNotNull(response1.getState(), "The state is null");

            String accessToken = response1.getAccessToken();

            // 3. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(response3)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                    .check();
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }
}