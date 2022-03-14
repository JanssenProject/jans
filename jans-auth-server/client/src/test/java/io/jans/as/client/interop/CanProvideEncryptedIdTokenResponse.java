/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
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
import static org.testng.Assert.fail;

/**
 * OC5:FeatureTest-Can Provide Encrypted ID Token Response
 *
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class CanProvideEncryptedIdTokenResponse extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response A128KW A128GCM");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
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

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
            AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response A256KW A256GCM");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();
            String clientSecret = registerResponse.getClientSecret();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
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

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
            AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response RSA1_5 A128CBC_PLUS_HS256");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
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

            // 3. Read Encrypted ID Token
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            AssertBuilder.jwe(jwe).check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response RSA1_5 A256CBC_PLUS_HS512");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation(), "The location is null");
            assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
            assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
            assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
            assertNotNull(authorizationResponse.getState(), "The state is null");

            String idToken = authorizationResponse.getIdToken();

            // 3. Read Encrypted ID Token
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            AssertBuilder.jwe(jwe).check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA_OAEP_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response RSA_OAEP A256GCM");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
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

            // 3. Read Encrypted ID Token
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            AssertBuilder.jwe(jwe).check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }
}