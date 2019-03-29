/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

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
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
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
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
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
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void canProvideEncryptedUserInfoResponseAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted UserInfo Response A256KW A256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
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
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientIdIssuedAt());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

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
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientIdIssuedAt());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

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
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientIdIssuedAt());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

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
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
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