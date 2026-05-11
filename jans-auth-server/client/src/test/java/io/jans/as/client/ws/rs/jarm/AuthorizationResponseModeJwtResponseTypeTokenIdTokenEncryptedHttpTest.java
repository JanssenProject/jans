/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.jarm;

import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.JwtUtil;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version August 26, 2021
 */
public class AuthorizationResponseModeJwtResponseTypeTokenIdTokenEncryptedHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                null, KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.FRAGMENT_JWT, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                null, KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.FRAGMENT_JWT, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        privateKey = TestCryptoContext.getInstance().getPrivateKey(Algorithm.RSA1_5);

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.FRAGMENT_JWT, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
        privateKey = null;
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        privateKey = TestCryptoContext.getInstance().getPrivateKey(Algorithm.RSA1_5);

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.FRAGMENT_JWT, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
        privateKey = null;
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        privateKey = TestCryptoContext.getInstance().getPrivateKey(Algorithm.RSA_OAEP);

        authorizationRequest(responseTypes, ResponseMode.JWT, ResponseMode.FRAGMENT_JWT, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
        privateKey = null;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String authJwt = TestCryptoContext.createJweWithNestedJws(
                authorizationRequest,
                SignatureAlgorithm.RS256,
                cryptoContext.getKeyId(Algorithm.RS256),
                KeyEncryptionAlgorithm.A128KW,
                BlockEncryptionAlgorithm.A128GCM,
                sharedKey,
                cryptoContext.getCryptoProvider(),
                Lists.newArrayList(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull())),
                Lists.newArrayList(
                    new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                    new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                    new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                    new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                    new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                )
        );
        authorizationRequest.setRequest(authJwt);

        authorizationRequest(authorizationRequest, ResponseMode.FRAGMENT_JWT, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        String authJwt = TestCryptoContext.createJweWithNestedJws(
                authorizationRequest,
                SignatureAlgorithm.RS256,
                cryptoContext.getKeyId(Algorithm.RS256),
                KeyEncryptionAlgorithm.A256KW,
                BlockEncryptionAlgorithm.A256GCM,
                sharedKey,
                cryptoContext.getCryptoProvider(),
                Lists.newArrayList(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull())),
                Lists.newArrayList(
                    new Claim(JwtClaimName.NAME, ClaimValue.createNull()),
                    new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)),
                    new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()),
                    new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()),
                    new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false))
                )
        );
        authorizationRequest.setRequest(authJwt);

        authorizationRequest(authorizationRequest, ResponseMode.FRAGMENT_JWT, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);

        String clientId = registerResponse.getClientId();

        // 2. Choose encryption key
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
        assertNotNull(serverKeyId);

        // 3. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        privateKey = cryptoContext.getPrivateKey(Algorithm.RSA1_5);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        String authJwt = TestCryptoContext.createJweWithNestedJwsAndRsaEncryption(
                authorizationRequest,
                SignatureAlgorithm.RS256,
                cryptoContext.getKeyId(Algorithm.RS256),
                KeyEncryptionAlgorithm.RSA1_5,
                BlockEncryptionAlgorithm.A128CBC_PLUS_HS256,
                serverKeyId,
                jwks,
                TestCryptoContext.getInstance().getCryptoProvider(),
                Lists.newArrayList(),
                Lists.newArrayList());
        authorizationRequest.setRequest(authJwt);

        authorizationRequest(authorizationRequest, ResponseMode.FRAGMENT_JWT, userId, userSecret);
        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationRequestObjectAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);

        String clientId = registerResponse.getClientId();

        // 2. Choose encryption key
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA1_5);
        assertNotNull(serverKeyId);

        // 3. Request authorization
        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        privateKey = cryptoContext.getPrivateKey(Algorithm.RSA1_5);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        String authJwt = TestCryptoContext.createJweWithNestedJwsAndRsaEncryption(
                authorizationRequest,
                SignatureAlgorithm.RS256,
                cryptoContext.getKeyId(Algorithm.RS256),
                KeyEncryptionAlgorithm.RSA1_5,
                BlockEncryptionAlgorithm.A256CBC_PLUS_HS512,
                serverKeyId,
                jwks,
                TestCryptoContext.getInstance().getCryptoProvider(),
                Lists.newArrayList(),
                Lists.newArrayList());
        authorizationRequest.setRequest(authJwt);

        authorizationRequest(authorizationRequest, ResponseMode.FRAGMENT_JWT, userId, userSecret);
        privateKey = null; // Clear private key to do not affect to other tests
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClientWithJwks(redirectUris, responseTypes, sectorIdentifierUri, SignatureAlgorithm.RS256,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Choose encryption key
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        // 3. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        privateKey = cryptoContext.getPrivateKey(Algorithm.RSA_OAEP);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        String authJwt = TestCryptoContext.createJweWithNestedJwsAndRsaEncryption(
                authorizationRequest,
                SignatureAlgorithm.RS256,
                cryptoContext.getKeyId(Algorithm.RS256),
                KeyEncryptionAlgorithm.RSA_OAEP,
                BlockEncryptionAlgorithm.A256GCM,
                serverKeyId,
                jwks,
                TestCryptoContext.getInstance().getCryptoProvider(),
                Lists.newArrayList(),
                Lists.newArrayList());
        authorizationRequest.setRequest(authJwt);

        authorizationRequest(authorizationRequest, ResponseMode.FRAGMENT_JWT, userId, userSecret);
        privateKey = null; // Clear private key to do not affect to other tests
    }
}
