/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.oxauth.model.util.Util;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
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

            String idToken = authorizationResponse.getIdToken();

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
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

            String idToken = authorizationResponse.getIdToken();

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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

            String idToken = authorizationResponse.getIdToken();

            // 3. Read Encrypted ID Token
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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

            String idToken = authorizationResponse.getIdToken();

            // 3. Read Encrypted ID Token
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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

            String idToken = authorizationResponse.getIdToken();

            // 3. Read Encrypted ID Token
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }
}