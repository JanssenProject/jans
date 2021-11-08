/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.jarm;

import io.jans.as.client.*;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Javier Rojas Blum
 * @version November 5, 2021
 */
public class AuthorizationResponseModeJwtResponseTypeCodeSignedEncryptedHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectPS256RSA_OAEPA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
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

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail1(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail1");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, null, null);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scope, redirectUri, nonce);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
//        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail2(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail2");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail3(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail3");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail4(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail4");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail5(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail5");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(null);
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

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail6(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail6");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail7(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail7");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setAud("https://www.other1.example.com/"); // Added bad aud to request object claims
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail8(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail8");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail9(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail9");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
//        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
//                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, null, null);

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
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
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

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        assertNotNull(authorizationResponse.getResponse());

        //Jwe response = Jwe.parse(authorizationResponse.getResponse(), privateKey, null);
        Jwt response = Jwt.parse(authorizationResponse.getResponse());

        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.ISS));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.AUD));
        assertNotNull(response.getClaims().getClaimAsInteger(AuthorizeResponseParam.EXP));
        assertNotNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.STATE));
        assertNull(response.getClaims().getClaimAsString(AuthorizeResponseParam.CODE));
        assertNotNull(response.getClaims().getClaimAsString("error"));
        assertNotNull(response.getClaims().getClaimAsString("error_description"));

        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail10(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail10");

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
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider1); // RS256 Request Object is not permitted by the FAPI-RW specification.
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwsAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim

        authorizationRequest.setRequest(jwsAuthorizationRequest.getEncodedJwt());

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
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

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Enable FAPI to run this test!
    public void authorizationRequestObjectPS256RSA_OAEPA256GCMFail11(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectPS256RSA_OAEPA256GCMFail11");

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
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.NONE, cryptoProvider1); // none Request Object is not permitted by the FAPI-RW specification.
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwsAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        jwsAuthorizationRequest.setRedirectUri(redirectUri);
        jwsAuthorizationRequest.setNonce(nonce); // FAPI: nonce param is required
        jwsAuthorizationRequest.setNbf((int) Instant.now().getEpochSecond()); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim
        jwsAuthorizationRequest.setExp(jwsAuthorizationRequest.getNbf() + 3600); // FAPI: require the request object to contain an exp claim that has a lifetime of no longer than 60 minutes after the nbf claim

        authorizationRequest.setRequest(jwsAuthorizationRequest.getEncodedJwt());

        AuthorizationResponse authorizationResponse = authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
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
}
