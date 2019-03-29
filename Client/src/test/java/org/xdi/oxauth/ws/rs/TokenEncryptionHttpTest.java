/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.gluu.oxauth.model.common.GrantType;
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
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version September 3, 2018
 */
@Deprecated
public class TokenEncryptionHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "clientJwksUri",
            "RS256_enc_keyId", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Before run this test, set openidScopeBackwardCompatibility to true
    @Deprecated
    public void requestIdTokenAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String jwksUri,
            final String keyId, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) {
        try {
            showTitle("requestIdTokenAlgRSAOAEPEncA256GCM");

            List<GrantType> grantTypes = Arrays.asList(
                    GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
            );

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClientId());
            assertNotNull(response.getClientSecret());
            assertNotNull(response.getRegistrationAccessToken());
            assertNotNull(response.getClientSecretExpiresAt());

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
            tokenRequest.setUsername(userId);
            tokenRequest.setPassword(userSecret);
            tokenRequest.setScope("openid");
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

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
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "clientJwksUri",
            "RS256_enc_keyId", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Before run this test, set openidScopeBackwardCompatibility to true
    @Deprecated
    public void requestIdTokenAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUris, final String jwksUri,
            final String keyId, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) {
        try {
            showTitle("requestIdTokenAlgRSA15EncA128CBCPLUSHS256");

            List<GrantType> grantTypes = Arrays.asList(
                    GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
            );

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClientId());
            assertNotNull(response.getClientSecret());
            assertNotNull(response.getRegistrationAccessToken());
            assertNotNull(response.getClientSecretExpiresAt());

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
            tokenRequest.setUsername(userId);
            tokenRequest.setPassword(userSecret);
            tokenRequest.setScope("openid");
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

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
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "clientJwksUri",
            "RS256_enc_keyId", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    //@Test // Before run this test, set openidScopeBackwardCompatibility to true
    @Deprecated
    public void requestIdTokenAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUris, final String jwksUri,
            final String keyId, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) {
        try {
            showTitle("requestIdTokenAlgRSA15EncA256CBCPLUSHS512");

            List<GrantType> grantTypes = Arrays.asList(
                    GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
            );

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClientId());
            assertNotNull(response.getClientSecret());
            assertNotNull(response.getRegistrationAccessToken());
            assertNotNull(response.getClientSecretExpiresAt());

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
            tokenRequest.setUsername(userId);
            tokenRequest.setPassword(userSecret);
            tokenRequest.setScope("openid");
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

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
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    //@Test // Before run this test, set openidScopeBackwardCompatibility to true
    @Deprecated
    public void requestIdTokenAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUris, final String sectorIdentifierUri) {
        try {
            showTitle("requestIdTokenAlgA128KWEncA128GCM");

            List<GrantType> grantTypes = Arrays.asList(
                    GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
            );

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClientId());
            assertNotNull(response.getClientSecret());
            assertNotNull(response.getRegistrationAccessToken());
            assertNotNull(response.getClientSecretExpiresAt());

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
            tokenRequest.setUsername(userId);
            tokenRequest.setPassword(userSecret);
            tokenRequest.setScope("openid");
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    //@Test // Before run this test, set openidScopeBackwardCompatibility to true
    @Deprecated
    public void requestIdTokenAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String sectorIdentifierUri) {
        try {
            showTitle("requestIdTokenAlgA256KWEncA256GCM");

            List<GrantType> grantTypes = Arrays.asList(
                    GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
            );

            // 1. Dynamic Client Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClientId());
            assertNotNull(response.getClientSecret());
            assertNotNull(response.getRegistrationAccessToken());
            assertNotNull(response.getClientSecretExpiresAt());

            String clientId = response.getClientId();
            String clientSecret = response.getClientSecret();

            // 2. Request authorization
            TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
            tokenRequest.setUsername(userId);
            tokenRequest.setPassword(userSecret);
            tokenRequest.setScope("openid");
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }
}