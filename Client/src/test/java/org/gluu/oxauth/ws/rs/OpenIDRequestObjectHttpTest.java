/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ws.rs;

import org.json.JSONObject;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.client.model.authorize.Claim;
import org.gluu.oxauth.client.model.authorize.ClaimValue;
import org.gluu.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.Algorithm;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.util.StringHelper;
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

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
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

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response2.getStatus(), 200, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response2.getClaim(JwtClaimName.NAME));
        assertNotNull(response2.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response2.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response2.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response2.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response2.getClaim(JwtClaimName.LOCALE));
        assertNotNull(response2.getClaim(JwtClaimName.ADDRESS));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod3(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod3");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod5(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod5");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

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

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethod6(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethod6");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setClaims(Arrays.asList(JwtClaimName.NAME));

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

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
        assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
        assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "PS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "PS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS256X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS256X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS384X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS384X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS512X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS512X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES256X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES256X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
        assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES384X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES384X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
        assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodES512X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodES512X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();

            // 2. Authorization Request
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setRequest("INVALID_OPENID_REQUEST_OBJECT");
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Authorization Request
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Authorization Request
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Authorization Request
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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
    @Test // This tests requires a place to publish a request object via HTTPS
    public void requestFileMethod(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            @Optional final String requestFileBasePath, final String requestFileBaseUrl, final String sectorIdentifierUri) throws Exception {
        showTitle("requestFileMethod");

        if (StringHelper.isEmpty(requestFileBasePath)) {
            return;
        }

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
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

        // 2. Request Authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String hash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(authJwt));
            String fileName = UUID.randomUUID().toString() + ".txt";
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

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
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

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "requestFileBaseUrl", "sectorIdentifierUri"})
    @Test
    public void requestFileMethodFail2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String requestFileBaseUrl, final String sectorIdentifierUri) {
        try {
            showTitle("requestFileMethodFail2");

            List<ResponseType> responseTypes = Arrays.asList(
                    ResponseType.TOKEN,
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

            // 2. Authorization Request
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            authorizationRequest.setRequestUri(requestFileBaseUrl + "/FAKE_REQUEST_URI");

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);
            AuthorizationResponse response = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getLocation(), "The location is null");
            // assertNotNull(response.getErrorType(), "The error type is null");
            // assertNotNull(response.getErrorDescription(), "The error description is null");
            assertNotNull(response.getState(), "The state is null");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "requestFileBasePath", "requestFileBaseUrl", "sectorIdentifierUri"})
    @Test // This tests requires a place to publish a request object via HTTPS
    public void requestFileMethodFail3(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            @Optional final String requestFileBasePath, final String requestFileBaseUrl, final String sectorIdentifierUri) throws Exception {
        showTitle("requestFileMethodFail3");

        if (StringHelper.isEmpty(requestFileBasePath)) {
            return;
        }

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
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

        // 2. Authorization Request
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        try {
            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String hash = "INVALID_HASH";
            String fileName = UUID.randomUUID().toString() + ".txt";
            String filePath = requestFileBasePath + File.separator + fileName;
            String fileUrl = requestFileBaseUrl + "/" + fileName + "#" + hash;
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
        }

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodAlgNone(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("requestParameterMethodAlgNone");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.NONE);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.NONE, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
            assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
            assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(response3.getClaim(JwtClaimName.NAME));
            assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
            assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
            assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
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

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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

            // 2. Choose encryption key
            JwkClient jwkClient = new JwkClient(jwksUri);
            JwkResponse jwkResponse = jwkClient.exec();
            String keyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
            assertNotNull(keyId);

            // 3. Request authorization
            JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                    KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt(jwks);
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
            assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
            assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(response3.getClaim(JwtClaimName.NAME));
            assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
            assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
            assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
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

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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

            // 2. Choose encryption key
            JwkClient jwkClient = new JwkClient(jwksUri);
            JwkResponse jwkResponse = jwkClient.exec();
            String keyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
            assertNotNull(keyId);

            // 3. Request authorization
            JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                    KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt(jwks);
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
            assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
            assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(response3.getClaim(JwtClaimName.NAME));
            assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
            assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
            assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
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

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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

            // 2. Choose encryption key
            JwkClient jwkClient = new JwkClient(jwksUri);
            JwkResponse jwkResponse = jwkClient.exec();
            String keyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
            assertNotNull(keyId);

            // 3. Request authorization
            JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            request.setState(state);
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                    KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt(jwks);
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
            assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
            assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(response3.getClaim(JwtClaimName.NAME));
            assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
            assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
            assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
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

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                    request, KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, clientSecret);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
            assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
            assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(response3.getClaim(JwtClaimName.NAME));
            assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
            assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
            assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
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

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
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

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                    request, KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM, clientSecret);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
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
            assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
            assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(response3.getClaim(JwtClaimName.NAME));
            assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
            assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
            assertNotNull(response3.getClaim(JwtClaimName.ADDRESS));
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }
}