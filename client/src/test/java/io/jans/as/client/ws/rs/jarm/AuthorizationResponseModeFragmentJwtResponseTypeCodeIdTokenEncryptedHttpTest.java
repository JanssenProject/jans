/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.jarm;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @version July 28, 2021
 */
public class AuthorizationResponseModeFragmentJwtResponseTypeCodeIdTokenEncryptedHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                null, KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.FRAGMENT_JWT, null, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void testAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, null,
                null, KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();
        sharedKey = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        authorizationRequest(responseTypes, ResponseMode.FRAGMENT_JWT, null, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void testAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String clientJwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                null, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        privateKey = cryptoProvider.getPrivateKey(keyId);

        authorizationRequest(responseTypes, ResponseMode.FRAGMENT_JWT, null, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void testAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String clientJwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                null, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        privateKey = cryptoProvider.getPrivateKey(keyId);

        authorizationRequest(responseTypes, ResponseMode.FRAGMENT_JWT, null, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri",
            "clientJwksUri", "RSA_OAEP_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void testAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String clientJwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("testAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                null, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        privateKey = cryptoProvider.getPrivateKey(keyId);

        authorizationRequest(responseTypes, ResponseMode.FRAGMENT_JWT, null, clientId, scopes, redirectUri, nonce,
                state, userId, userSecret);
    }
}
