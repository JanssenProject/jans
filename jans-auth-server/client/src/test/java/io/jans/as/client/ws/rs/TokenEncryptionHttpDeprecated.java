/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Javier Rojas Blum
 * @version September 3, 2018
 */
@Deprecated
public class TokenEncryptionHttpDeprecated extends BaseTest {

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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).ok().check();

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
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            AssertBuilder.jwe(jwe)
                    .claimsPresence(JwtClaimName.JANS_OPENID_CONNECT_VERSION)
                    .check();
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).ok().check();

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
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            AssertBuilder.jwe(jwe)
                    .claimsPresence(JwtClaimName.JANS_OPENID_CONNECT_VERSION)
                    .check();
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).ok().check();

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
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            Jwe jwe = Jwe.parse(idToken, privateKey, null);
            AssertBuilder.jwe(jwe)
                    .claimsPresence(JwtClaimName.JANS_OPENID_CONNECT_VERSION)
                    .check();
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).ok().check();

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
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
            AssertBuilder.jwe(jwe)
                    .claimsPresence(JwtClaimName.JANS_OPENID_CONNECT_VERSION)
                    .check();
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
            registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setGrantTypes(grantTypes);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse response = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(response).ok().check();

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
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
            assertNotNull(tokenResponse.getScope(), "The scope is null");
            assertNotNull(tokenResponse.getIdToken(), "The id token is null");

            String idToken = tokenResponse.getIdToken();

            // 3. Read Encrypted ID Token
            Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
            AssertBuilder.jwe(jwe)
                    .claimsPresence(JwtClaimName.JANS_OPENID_CONNECT_VERSION)
                    .check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }
}