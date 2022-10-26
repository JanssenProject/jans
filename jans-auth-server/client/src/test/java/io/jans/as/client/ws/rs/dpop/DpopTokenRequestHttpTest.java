/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.dpop;

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
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.common.TokenType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.KeyType;
import io.jans.as.model.jwt.DPoP;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.HttpMethod;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version October 5, 2021
 */
public class DpopTokenRequestHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_RS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_RS256");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        RSAPublicKey publicKey = (RSAPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.RSA);
        jsonWebKey.setN(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getModulus()));
        jsonWebKey.setE(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getPublicExponent()));
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.RS256, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.RS256, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_RS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_RS384");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        RSAPublicKey publicKey = (RSAPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.RSA);
        jsonWebKey.setN(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getModulus()));
        jsonWebKey.setE(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getPublicExponent()));
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.RS384, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.RS384, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_RS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_RS512");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        RSAPublicKey publicKey = (RSAPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.RSA);
        jsonWebKey.setN(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getModulus()));
        jsonWebKey.setE(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getPublicExponent()));
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.RS512, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.RS512, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_ES256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_ES256");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        ECPublicKey publicKey = (ECPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.EC);
        jsonWebKey.setX(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineX()));
        jsonWebKey.setY(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineY()));
        jsonWebKey.setCrv(EllipticEdvardsCurve.P_256);
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.ES256, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.ES256, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_ES384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_ES384");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        ECPublicKey publicKey = (ECPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.EC);
        jsonWebKey.setX(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineX()));
        jsonWebKey.setY(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineY()));
        jsonWebKey.setCrv(EllipticEdvardsCurve.P_384);
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.ES384, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.ES384, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_ES512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_ES512");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        ECPublicKey publicKey = (ECPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.EC);
        jsonWebKey.setX(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineX()));
        jsonWebKey.setY(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineY()));
        jsonWebKey.setCrv(EllipticEdvardsCurve.P_521);
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.ES512, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.ES512, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_PS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_PS256");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        RSAPublicKey publicKey = (RSAPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.RSA);
        jsonWebKey.setN(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getModulus()));
        jsonWebKey.setE(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getPublicExponent()));
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.PS256, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.PS256, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "PS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_PS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_PS384");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        RSAPublicKey publicKey = (RSAPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.RSA);
        jsonWebKey.setN(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getModulus()));
        jsonWebKey.setE(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getPublicExponent()));
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.PS384, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.PS384, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "PS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testDPoP_PS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("testDPoP_PS512");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        // 1. Dynamic Registration
        String clientId = dynamicRegistration(redirectUris, sectorIdentifierUri, clientJwksUri, responseTypes);

        // 2. Request authorization
        String authorizationCode = requestAuthorizationCode(userId, userSecret, redirectUri, responseTypes, clientId);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        RSAPublicKey publicKey = (RSAPublicKey) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.RSA);
        jsonWebKey.setN(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getModulus()));
        jsonWebKey.setE(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getPublicExponent()));
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.PS512, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenResponse tokenResponse = requestAccessToken(redirectUri, authorizationCode, dpop1);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        thumbprintConfirmationMethod(jwkThumbprint, accessToken);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        tokenIntrospection(jwkThumbprint, accessToken);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.PS512, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);
        dpop2.setAth(accessTokenHash);

        requestAccessTokenWithRefreshToken(refreshToken, dpop2);
    }

    private void requestAccessTokenWithRefreshToken(String refreshToken, DPoP dpop) {
        TokenRequest tokenRequest = new TokenRequest(GrantType.REFRESH_TOKEN);
        tokenRequest.setRefreshToken(refreshToken);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.NONE);
        tokenRequest.setDpop(dpop);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
        assertEquals(tokenResponse.getTokenType(), TokenType.DPOP);
    }

    private void thumbprintConfirmationMethod(String jwkThumbprint, String accessToken) throws InvalidJwtException {
        Jwt accessTokenJwt = Jwt.parse(accessToken);
        assertNotNull(accessTokenJwt.getClaims());
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.ISSUER));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.NOT_BEFORE));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.EXPIRATION_TIME));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.CNF));
        assertTrue(accessTokenJwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).has(JwtClaimName.JKT));
        assertEquals(accessTokenJwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).get(JwtClaimName.JKT), jwkThumbprint);
    }

    private void tokenIntrospection(String jwkThumbprint, String accessToken) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException, InvalidJwtException {
        IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, clientEngine(true));
        String jwtAsString = introspectionService.introspectTokenWithResponseAsJwt("Bearer " + accessToken, accessToken, true);

        Jwt jwt = Jwt.parse(jwtAsString);
        assertNotNull(jwt.getClaims());
        assertTrue(Boolean.parseBoolean(jwt.getClaims().getClaimAsString("active")));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.ISSUER));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.NOT_BEFORE));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.EXPIRATION_TIME));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.CNF));
        assertTrue(jwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).has(JwtClaimName.JKT));
        assertEquals(jwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).get(JwtClaimName.JKT), jwkThumbprint);
    }

    @NotNull
    private TokenResponse requestAccessToken(String redirectUri, String authorizationCode, DPoP dpop) {
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.NONE);
        tokenRequest.setDpop(dpop);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
        assertEquals(tokenResponse.getTokenType(), TokenType.DPOP);
        return tokenResponse;
    }

    private String requestAuthorizationCode(String userId, String userSecret, String redirectUri, List<ResponseType> responseTypes, String clientId) {
        List<String> scope = Collections.singletonList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scope, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authorizationResponse).check();

        return authorizationResponse.getCode();
    }

    private String dynamicRegistration(String redirectUris, String sectorIdentifierUri, String clientJwksUri, List<ResponseType> responseTypes) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setAccessTokenAsJwt(true); // Enable for JWK Thumbprint Confirmation Method

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        return registerResponse.getClientId();
    }
}
