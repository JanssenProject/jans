/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BackchannelAuthenticationClient;
import io.jans.as.client.BackchannelAuthenticationRequest;
import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version May 28, 2020
 */
public class BackchannelAuthenticationPollMode extends BaseTest {

    String idTokenHintRS256;
    String idTokenHintRS384;
    String idTokenHintRS512;
    String idTokenHintES256;
    String idTokenHintES384;
    String idTokenHintES512;
    String idTokenHintPS256;
    String idTokenHintPS384;
    String idTokenHintPS512;
    String idTokenHintAlgA128KWEncA128GCM;
    String idTokenHintAlgA256KWEncA256GCM;
    String idTokenHintAlgRSA15EncA128CBCPLUSHS256;
    String idTokenHintAlgRSA15EncA256CBCPLUSHS512;
    String idTokenHintAlgRSAOAEPEncA256GCM;

    String loginHintTokenRS256;
    String loginHintTokenRS384;
    String loginHintTokenRS512;
    String loginHintTokenES256;
    String loginHintTokenES384;
    String loginHintTokenES512;
    String loginHintTokenPS256;
    String loginHintTokenPS384;
    String loginHintTokenPS512;

    @Parameters({"clientJwksUri", "userId", "backchannelUserCode"})
    @Test
    public void backchannelTokenDeliveryModePollLoginHint1(
            final String clientJwksUri, final String userId, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHint1");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String bindingMessage = RandomStringUtils.randomAlphanumeric(6);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Arrays.asList("openid", "profile", "email", "address", "phone"));
        backchannelAuthenticationRequest.setLoginHint(userId);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAcrValues(Arrays.asList("auth_ldap_server", "basic"));
        backchannelAuthenticationRequest.setBindingMessage(bindingMessage);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();

        //String authReqId = backchannelAuthenticationResponse.getAuthReqId();

        // 3. Token Request Using CIBA Grant Type
        // Uncomment for manual testing
        /*
        TokenResponse tokenResponse = null;
        int pollCount = 0;
        do {
            Thread.sleep(5000);

            TokenRequest tokenRequest = new TokenRequest(GrantType.CIBA);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthReqId(authReqId);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            pollCount++;
        } while (tokenResponse.getStatus() == 400 && pollCount < 5);

        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");

        String accessToken = tokenResponse.getAccessToken();

        // 4. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertUserInfoPersonalDataNotNull(userInfoResponse);
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.UPDATED_AT));
        */
    }

    @Parameters({"clientJwksUri", "userEmail", "backchannelUserCode"})
    @Test
    public void backchannelTokenDeliveryModePollLoginHint2(
            final String clientJwksUri, final String userEmail, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHint2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHint(userEmail);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "userInum", "backchannelUserCode"})
    @Test
    public void backchannelTokenDeliveryModePollLoginHint3(
            final String clientJwksUri, final String userInum, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHint3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHint(userInum);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "userId", "backchannelUserCode"})
    @Test(enabled = false) // Enable it for manual testing
    public void backchannelTokenDeliveryModePollLoginHint4(
            final String clientJwksUri, final String userId, final String backchannelUserCode) throws Exception {
        showTitle("backchannelTokenDeliveryModePollLoginHint4");

        RegisterResponse registerResponse1 = requestClientRegistration(clientJwksUri);
        RegisterResponse registerResponse2 = requestClientRegistration(clientJwksUri);

        String sub1 = requestBackchannelAuthentication(userId, registerResponse1.getClientId(), registerResponse1.getClientSecret(), backchannelUserCode);
        String sub2 = requestBackchannelAuthentication(userId, registerResponse2.getClientId(), registerResponse2.getClientSecret(), backchannelUserCode);

        assertEquals(sub1, sub2, "Each client must share the same sub value");

        String sub3 = requestBackchannelAuthentication(userId, registerResponse1.getClientId(), registerResponse1.getClientSecret(), backchannelUserCode);
        String sub4 = requestBackchannelAuthentication(userId, registerResponse2.getClientId(), registerResponse2.getClientSecret(), backchannelUserCode);

        assertEquals(sub1, sub3, "Same client must receive the same sub value");
        assertEquals(sub2, sub4, "Same client must receive the same sub value");
    }

    public RegisterResponse requestClientRegistration(
            final String clientJwksUri) {
        // Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        return registerResponse;
    }

    public String requestBackchannelAuthentication(
            final String userId, final String clientId, final String clientSecret, final String backchannelUserCode) throws Exception {
        // Authentication Request
        String bindingMessage = RandomStringUtils.randomAlphanumeric(6);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Arrays.asList("openid", "profile", "email", "address", "phone"));
        backchannelAuthenticationRequest.setLoginHint(userId);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAcrValues(Arrays.asList("auth_ldap_server", "basic"));
        backchannelAuthenticationRequest.setBindingMessage(bindingMessage);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();

        String authReqId = backchannelAuthenticationResponse.getAuthReqId();

        // Token Request Using CIBA Grant Type
        TokenResponse tokenResponse = null;
        int pollCount = 0;
        do {
            Thread.sleep(5000);

            TokenRequest tokenRequest = new TokenRequest(GrantType.CIBA);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthReqId(authReqId);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            pollCount++;
        } while (tokenResponse.getStatus() == 400 && pollCount < 5);
        AssertBuilder.tokenResponse(tokenResponse)
                .check();

        String accessToken = tokenResponse.getAccessToken();
        String idToken = tokenResponse.getIdToken();

        // Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.WEBSITE, JwtClaimName.ADDRESS, JwtClaimName.EMAIL_VERIFIED)
                .claimsPresence(JwtClaimName.GENDER, JwtClaimName.PROFILE, JwtClaimName.PHONE_NUMBER_VERIFIED)
                .claimsPresence(JwtClaimName.PREFERRED_USERNAME, JwtClaimName.MIDDLE_NAME, JwtClaimName.UPDATED_AT)
                .claimsPresence(JwtClaimName.NICKNAME, JwtClaimName.PHONE_NUMBER)
                .check();

        // Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        String sub = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);

        return sub;
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintRS256")
    public void backchannelTokenDeliveryModePollIdTokenHintRS256(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintRS256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintRS256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.RS256);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintRS384")
    public void backchannelTokenDeliveryModePollIdTokenHintRS384(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintRS384");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS384);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS384)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintRS384);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.RS384);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintRS512")
    public void backchannelTokenDeliveryModePollIdTokenHintRS512(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintRS512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS512);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS512)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintRS512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.RS512);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintES256")
    public void backchannelTokenDeliveryModePollIdTokenHintES256(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintES256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.ES256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.ES256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintES256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.ES256);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintES384")
    public void backchannelTokenDeliveryModePollIdTokenHintES384(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintES384");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.ES384);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.ES384)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintES384);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.ES384);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintES512")
    public void backchannelTokenDeliveryModePollIdTokenHintES512(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintES512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.ES512);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.ES512)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintES512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.ES512);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintPS256")
    public void backchannelTokenDeliveryModePollIdTokenHintPS256(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintPS256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintPS256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.PS256);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "PS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintPS384")
    public void backchannelTokenDeliveryModePollIdTokenHintPS384(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintPS384");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS384);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS384)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintPS384);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.PS384);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode",
            "PS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "idTokenHintPS512")
    public void backchannelTokenDeliveryModePollIdTokenHintPS512(
            final String clientJwksUri, final String backchannelUserCode,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintPS512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS512);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS512)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintPS512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        backchannelAuthenticationRequest.setAlgorithm(SignatureAlgorithm.PS512);
        backchannelAuthenticationRequest.setCryptoProvider(cryptoProvider);
        backchannelAuthenticationRequest.setKeyId(keyId);
        backchannelAuthenticationRequest.setAudience(tokenEndpoint);
        backchannelAuthenticationRequest.setAuthUsername(clientId);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "idTokenHintAlgA128KWEncA128GCM")
    public void backchannelTokenDeliveryModePollIdTokenHintAlgA128KWEncA128GCM(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintAlgA128KWEncA128GCM");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintAlgA128KWEncA128GCM);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "idTokenHintAlgA256KWEncA256GCM")
    public void backchannelTokenDeliveryModePollIdTokenHintAlgA256KWEncA256GCM(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintAlgA256KWEncA256GCM");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintAlgA256KWEncA256GCM);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "idTokenHintAlgRSA15EncA128CBCPLUSHS256")
    public void backchannelTokenDeliveryModePollIdTokenHintAlgRSA15EncA128CBCPLUSHS256(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintAlgRSA15EncA128CBCPLUSHS256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintAlgRSA15EncA128CBCPLUSHS256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "idTokenHintAlgRSA15EncA256CBCPLUSHS512")
    public void backchannelTokenDeliveryModePollIdTokenHintAlgRSA15EncA256CBCPLUSHS512(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintAlgRSA15EncA256CBCPLUSHS512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintAlgRSA15EncA256CBCPLUSHS512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "idTokenHintAlgRSAOAEPEncA256GCM")
    public void backchannelTokenDeliveryModePollIdTokenHintAlgRSAOAEPEncA256GCM(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollIdTokenHintAlgRSAOAEPEncA256GCM");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHintAlgRSAOAEPEncA256GCM);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenRS256")
    public void backchannelTokenDeliveryModePollLoginHintTokenRS256(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenRS256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenRS256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenRS384")
    public void backchannelTokenDeliveryModePollLoginHintTokenRS384(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenRS384");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS384);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS384)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenRS384);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenRS512")
    public void backchannelTokenDeliveryModePollLoginHintTokenRS512(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenRS512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS512);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS512)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenRS512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenES256")
    public void backchannelTokenDeliveryModePollLoginHintTokenES256(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenES256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.ES256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.ES256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenES256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenES384")
    public void backchannelTokenDeliveryModePollLoginHintTokenES384(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenES384");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.ES384);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.ES384)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenES384);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenES512")
    public void backchannelTokenDeliveryModePollLoginHintTokenES512(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenES512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.ES512);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.ES512)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenES512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenPS256")
    public void backchannelTokenDeliveryModePollLoginHintTokenPS256(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenPS256");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenPS256);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenPS384")
    public void backchannelTokenDeliveryModePollLoginHintTokenPS384(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenPS384");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS384);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS384)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenPS384);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri", "backchannelUserCode"})
    @Test(dependsOnMethods = "loginHintTokenPS512")
    public void backchannelTokenDeliveryModePollLoginHintTokenPS512(
            final String clientJwksUri, final String backchannelUserCode) {
        showTitle("backchannelTokenDeliveryModePollLoginHintTokenPS512");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS512);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS512)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintTokenPS512);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePollFail1(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePollFail1");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        // 2. Authentication Request
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.INVALID_REQUEST).check();
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePollFail2(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePollFail2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();

        // 2. Authentication Request
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword("INVALID_CLIENT_SECRET");

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).unauthorized(BackchannelAuthenticationErrorResponseType.INVALID_CLIENT).check();
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePollFail3(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePollFail3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(null); // Invalid Scope
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID)
                .check();
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePollFail4(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePollFail4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHint(null); // Invalid login hint.
        backchannelAuthenticationRequest.setLoginHintToken(null); // Invalid login hint token.
        backchannelAuthenticationRequest.setIdTokenHint(null); // Invalid id token hint
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID)
                .check();
    }

    @Parameters({"clientJwksUri", "userId"})
    @Test
    public void backchannelTokenDeliveryModePollFail5(final String clientJwksUri, final String userId) {
        showTitle("backchannelTokenDeliveryModePollFail5");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHint(userId);
        backchannelAuthenticationRequest.setClientNotificationToken(null); // Invalid client notification token.
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.INVALID_USER_CODE)
                .check();
    }

    @Parameters({"clientJwksUri", "userId"})
    @Test
    public void backchannelTokenDeliveryModePollFail6(final String clientJwksUri, final String userId) {
        showTitle("backchannelTokenDeliveryModePollFail6");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Collections.singletonList("openid"));
        backchannelAuthenticationRequest.setLoginHint(userId);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(null); // Invalid user code.
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.INVALID_USER_CODE)
                .check();
    }

    @Parameters({"clientJwksUri", "userId", "backchannelUserCode"})
    @Test
    public void backchannelTokenDeliveryModePollFail7(
            final String clientJwksUri, final String userId, final String backchannelUserCode) throws InterruptedException {
        showTitle("backchannelTokenDeliveryModePollFail7");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .backchannelUserCodeParameter(true)
                .check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Authentication Request
        String clientNotificationToken = UUID.randomUUID().toString();

        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(Arrays.asList("openid", "profile", "email", "address", "phone"));
        backchannelAuthenticationRequest.setLoginHint(userId);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setUserCode(backchannelUserCode);
        backchannelAuthenticationRequest.setRequestedExpiry(1200);
        backchannelAuthenticationRequest.setAcrValues(Arrays.asList("auth_ldap_server", "basic"));
        backchannelAuthenticationRequest.setBindingMessage("####"); // Invalid binding message
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.INVALID_BINDING_MESSAGE)
                .check();

    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintRS256 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS384)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintRS384 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS512);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS512)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintRS512 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES256)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        idTokenHintES256 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES384);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES384)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        idTokenHintES384 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES512);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureECDSA(jwksUri, SignatureAlgorithm.ES512)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();

        idTokenHintES512 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS256)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintPS256 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintPS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS384);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS384)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintPS384 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintPS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS512);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.PS512)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintPS512 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();

        idTokenHintAlgA128KWEncA128GCM = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(StandardCharsets.UTF_8));
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();

        idTokenHintAlgA256KWEncA256GCM = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void idTokenHintAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("idTokenHintAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();

        idTokenHintAlgRSA15EncA128CBCPLUSHS256 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri",
            "clientJwksUri", "RSA1_5_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void idTokenHintAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("idTokenHintAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();

        idTokenHintAlgRSA15EncA256CBCPLUSHS512 = idToken;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri",
            "clientJwksUri", "RSA_OAEP_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void idTokenHintAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("idTokenHintAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
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

        // 3. Validate id_token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        Jwe jwe = Jwe.parse(idToken, privateKey, null);
        AssertBuilder.jwe(jwe)
                .notNullAccesTokenHash()
                .check();

        idTokenHintAlgRSAOAEPEncA256GCM = idToken;
    }

    @Parameters({"RS256_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenRS256(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenRS256");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.RS256);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenRS256 = jwt.toString();
    }

    @Parameters({"RS384_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenRS384(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenRS384");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS384);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.RS384);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenRS384 = jwt.toString();
    }

    @Parameters({"RS512_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenRS512(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenRS512");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS512);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.RS512);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenRS512 = jwt.toString();
    }

    @Parameters({"ES256_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenES256(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenES256");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.ES256);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.ES256);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenES256 = jwt.toString();
    }

    @Parameters({"ES384_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenES384(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenES384");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.ES384);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.ES384);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenES384 = jwt.toString();
    }

    @Parameters({"ES512_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenES512(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenES512");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.ES512);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.ES512);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenES512 = jwt.toString();
    }

    @Parameters({"PS256_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenPS256(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenPS256");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.PS256);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.PS256);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenPS256 = jwt.toString();
    }

    @Parameters({"PS384_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenPS384(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenPS384");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.PS384);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.PS384);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenPS384 = jwt.toString();
    }

    @Parameters({"PS512_keyId", "userEmail", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void loginHintTokenPS512(
            final String keyId, final String userEmail,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("loginHintTokenPS512");

        JSONObject subjectValue = new JSONObject();
        subjectValue.put("subject_type", "email");
        subjectValue.put("email", userEmail);

        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.PS512);
        jwt.getHeader().setKeyId(keyId);
        jwt.getClaims().setClaim("subject", subjectValue);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(jwt.getSigningInput(), keyId, null, SignatureAlgorithm.PS512);
        jwt.setEncodedSignature(encodedSignature);

        loginHintTokenPS512 = jwt.toString();
    }
}