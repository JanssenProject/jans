/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.jarm;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.JwtUtil;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version November 26, 2021
 */
public class AuthorizationResponseModeJwtResponseTypeCodeSignedEncryptedHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectPS256RSA_OAEPA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwsAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithoutExpFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithoutExpFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(null); // FAPI: exp param is required
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertTrue(Arrays.asList("invalid_request", "invalid_request_object", "invalid_request_uri", "access_denied")
                .contains(response.getClaims().getClaimAsString("error")));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithoutNbfFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithoutNbfFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setResponseTypes(responseTypes);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setScopes(scope);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf(null); // FAPI: nbf param is required
        jwsAuthorizationRequest.setExp((int) Instant.now().getEpochSecond() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertTrue(Arrays.asList("invalid_request", "invalid_request_object", "invalid_request_uri", "access_denied")
                .contains(response.getClaims().getClaimAsString("error")));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithoutScopeFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithoutScopeFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setScopes(null); // FAPI: scope param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertTrue(Arrays.asList("invalid_request", "invalid_request_object", "invalid_request_uri", "access_denied")
                .contains(response.getClaims().getClaimAsString("error")));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithoutNonceFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithoutNonceFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, nonce);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setScopes(scope);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setNonce(null); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertTrue(Arrays.asList("invalid_request", "invalid_request_object", "invalid_request_uri")
                .contains(response.getClaims().getClaimAsString("error")));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithoutRedirectUriFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithoutRedirectUriFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(null);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertTrue(Arrays.asList("invalid_request", "invalid_request_object")
                .contains(response.getClaims().getClaimAsString("error")));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureExpiredRequestObjectFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureExpiredRequestObjectFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() - 3600); // Address expired exp
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertEquals(response.getClaims().getClaimAsString("error"), "invalid_request_object");

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithBadAudFails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithBadAudFails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setAud("https://www.other1.example.com/"); // Added bad aud to request object claims
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertEquals(response.getClaims().getClaimAsString("error"), "invalid_request_object");

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithExpOver60Fails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithExpOver60Fails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 4200); // Added invalid exp value to request object which is 70 minutes in the future
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertEquals(response.getClaims().getClaimAsString("error"), "invalid_request_object");

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectWithNbfOver60Ffails(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectWithNbfOver60Ffails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond() - 4200); // Added invalid nbf value to request object which is 70 minutes in the past
        jwsAuthorizationRequest.setExp((int) Instant.now().getEpochSecond() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
        assertEquals(response.getClaims().getClaimAsString("error"), "invalid_request_object");

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"audience", "redirectUri", "redirectUris", "clientJwksUri", "RS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureSignedRequestObjectWithRS256Fails(
            final String audience, final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String signingKeyId, final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("ensureSignedRequestObjectWithRS256Fails");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.RS256, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider); // RS256 Request Object is not permitted by the FAPI-RW specification.
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setAud(audience);
        jwsAuthorizationRequest.setIss(clientId);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim

        authorizationRequest.setRequest(jwsAuthorizationRequest.getEncodedJwt());

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRequestObjectSignatureAlgorithmIsNotNone(
            final String redirectUri, final String redirectUris, final String clientJwksUri, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRequestObjectSignatureAlgorithmIsNotNone");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.RS256, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, null);
        authorizationRequest.setState(state);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.NONE, cryptoProvider); // none Request Object is not permitted by the FAPI-RW specification.
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim

        authorizationRequest.setRequest(jwsAuthorizationRequest.getEncodedJwt());

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertNotNull(authorizationResponse.getResponse());

        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));
    }

    @Parameters({"redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId", "PS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test(enabled = false) // Enable FAPI to run this test!
    public void ensureRedirectUriInAuthorizationRequest(
            final String redirectUri, final String redirectUris, final String clientJwksUri,
            final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("ensureRedirectUriInAuthorizationRequest");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, null, null);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.setRedirectUri(null);
        jwsAuthorizationRequest.setResponseMode(ResponseMode.JWT);
        jwsAuthorizationRequest.setState(state);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);

        assertEquals(authorizationResponse.getStatus(), 400);
        assertNotNull(authorizationResponse.getEntity());
        assertNotNull(authorizationResponse.getErrorType());
        assertNotNull(authorizationResponse.getErrorDescription());
        assertEquals(authorizationResponse.getErrorType(), AuthorizeErrorResponseType.INVALID_REQUEST);

        privateKey = null; // Clear private key to do not affect to other tests
    }
}
