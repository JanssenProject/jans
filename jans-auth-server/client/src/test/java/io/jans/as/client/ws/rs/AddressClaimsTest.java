/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.HMACSigner;
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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Note: In order to run this tests, set legacyIdTokenClaims to true.
 *
 * @author Javier Rojas Blum
 * @version March 8, 2019
 */
public class AddressClaimsTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestDefault(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestDefault");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .notNullClaimsAddressdata()
                .check();

        HMACSigner hmacSigner = new HMACSigner(SignatureAlgorithm.HS256, clientSecret);
        assertTrue(hmacSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestHS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestHS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS384);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS384, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .notNullClaimsAddressdata()
                .check();

        HMACSigner hmacSigner = new HMACSigner(SignatureAlgorithm.HS384, clientSecret);
        assertTrue(hmacSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void authorizationRequestHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS512, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .notNullClaimsAddressdata()
                .check();

        HMACSigner hmacSigner = new HMACSigner(SignatureAlgorithm.HS512, clientSecret);
        assertTrue(hmacSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS384_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS384)
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "RS512_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS512)
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "ES256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.ES256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .notNullClaimsAddressdata()
                .check();

        ECDSAPublicKey publicKey = JwkClient.getECDSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        ECDSASigner ecdsaSigner = new ECDSASigner(SignatureAlgorithm.ES256, publicKey);

        assertTrue(ecdsaSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "ES384_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.ES384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .notNullClaimsAddressdata()
                .check();

        ECDSAPublicKey publicKey = JwkClient.getECDSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        ECDSASigner ecdsaSigner = new ECDSASigner(SignatureAlgorithm.ES384, publicKey);

        assertTrue(ecdsaSigner.validate(jwt));

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "ES512_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.ES512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .notNullClaimsAddressdata()
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
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "PS256_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS256)
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "PS384_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS384, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri", "PS512_keyId", "clientJwksUri"})
    @Test
    public void authorizationRequestPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("authorizationRequestPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS512)
                .notNullClaimsAddressdata()
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "address");
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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullClaimsAddressdata()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "address");
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
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullClaimsAddressdata()
                .check();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "dnName", "keyStoreFile", "keyStoreSecret", "RSA1_5_keyId",
            "clientJwksUri", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String clientKeyId,
            final String clientJwksUri, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

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

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(serverKeyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullClaimsAddressdata()
                .check();

        // 5. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "dnName", "keyStoreFile", "keyStoreSecret", "RSA1_5_keyId",
            "clientJwksUri", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String clientKeyId,
            final String clientJwksUri, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

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

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(serverKeyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullClaimsAddressdata()
                .check();

        // 5. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "dnName", "keyStoreFile", "keyStoreSecret", "RSA_OAEP_keyId",
            "clientJwksUri", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String clientKeyId,
            final String clientJwksUri, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(Tester.addressScopes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.addCustomAttribute("jansInclClaimsInIdTkn", "true");

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

        List<String> scopes = Arrays.asList("openid", "address");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(serverKeyId);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_STREET_ADDRESS, ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.ADDRESS_COUNTRY, ClaimValue.createEssential(true)));
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
                .notNullClaimsAddressdata()
                .check();

        // 5. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        userInfoClient.setPrivateKey(privateKey);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.ADDRESS_STREET_ADDRESS, JwtClaimName.ADDRESS_COUNTRY)
                .check();
    }
}
