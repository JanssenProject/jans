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
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * OC5:FeatureTest-Can Provide Encrypted UserInfo Response
 *
 * @author Javier Rojas Blum
 * @version March 8, 2019
 */
public class CanProvideEncryptedUserInfoResponse extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedUserInfoResponseAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted UserInfo Response A128KW A128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
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

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedUserInfoResponseAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted UserInfo Response A256KW A256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
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

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .notNullClaimsAddressData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedUserInfoResponseAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted UserInfo Response RSA1_5 A128CBC_PLUS_HS256");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
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

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS).containsAll(Arrays.asList(
                    JwtClaimName.ADDRESS_STREET_ADDRESS,
                    JwtClaimName.ADDRESS_REGION,
                    JwtClaimName.ADDRESS_LOCALITY,
                    JwtClaimName.ADDRESS_COUNTRY)));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedUserInfoResponseAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted UserInfo Response RSA1_5 A256CBC_PLUS_HS512");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
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

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS).containsAll(Arrays.asList(
                    JwtClaimName.ADDRESS_STREET_ADDRESS,
                    JwtClaimName.ADDRESS_REGION,
                    JwtClaimName.ADDRESS_LOCALITY,
                    JwtClaimName.ADDRESS_COUNTRY)));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris",
            "clientJwksUri", "RSA_OAEP_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedUserInfoResponseAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String keyId, final String keyStoreFile, final String keyStoreSecret,
            final String sectorIdentifierUri) {
        try {
            showTitle("OC5:FeatureTest-Can Provide Encrypted UserInfo Response RSA_OAEP A256GCM");

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            // 1. Register client
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
            registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
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

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS).containsAll(Arrays.asList(
                    JwtClaimName.ADDRESS_STREET_ADDRESS,
                    JwtClaimName.ADDRESS_REGION,
                    JwtClaimName.ADDRESS_LOCALITY,
                    JwtClaimName.ADDRESS_COUNTRY)));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }
}