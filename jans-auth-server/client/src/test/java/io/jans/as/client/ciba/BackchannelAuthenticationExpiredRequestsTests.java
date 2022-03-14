/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.BackchannelAuthenticationClient;
import io.jans.as.client.BackchannelAuthenticationRequest;
import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.token.TokenErrorResponseType;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Milton BO
 * @version May 25, 2020
 */
public class BackchannelAuthenticationExpiredRequestsTests extends BaseTest {

    /**
     * Test poll flow when a request expires, response from the server should be expired_token and 400 status.
     */
    @Parameters({"clientJwksUri", "backchannelUserCode", "userId"})
    @Test
    public void backchannelTokenDeliveryModePollExpiredRequest(
            final String clientJwksUri, final String backchannelUserCode, final String userId) throws InterruptedException {
        showTitle("backchannelTokenDeliveryModePollExpiredRequest");

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
                .notNullRegistrationClientUri()
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
        backchannelAuthenticationRequest.setRequestedExpiry(1);
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

        // 3. Request token - expected expiration error

        TokenResponse tokenResponse;
        int pollCount = 0;
        do {
            Thread.sleep(3500);

            TokenRequest tokenRequest = new TokenRequest(GrantType.CIBA);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthReqId(backchannelAuthenticationResponse.getAuthReqId());

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            pollCount++;
        } while (pollCount < 5 && tokenResponse.getStatus() == 400
                && tokenResponse.getErrorType() == TokenErrorResponseType.AUTHORIZATION_PENDING);

        AssertBuilder.tokenResponse(tokenResponse).bad(TokenErrorResponseType.EXPIRED_TOKEN).check();
    }

    /**
     * Test ping flow when a request expires, response from the server should be expired_token and 400 status.
     */
    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint", "backchannelUserCode", "userId"})
    @Test
    public void backchannelTokenDeliveryModePingExpiredRequest(
            final String clientJwksUri, final String backchannelClientNotificationEndpoint, final String backchannelUserCode,
            final String userId) throws InterruptedException {
        showTitle("backchannelTokenDeliveryModePingExpiredRequest");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .notNullRegistrationClientUri()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
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
        backchannelAuthenticationRequest.setRequestedExpiry(1);
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

        // 3. Request token - expected expiration error

        TokenResponse tokenResponse;
        int pollCount = 0;
        do {
            Thread.sleep(3500);

            TokenRequest tokenRequest = new TokenRequest(GrantType.CIBA);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthReqId(backchannelAuthenticationResponse.getAuthReqId());

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            pollCount++;
        } while (pollCount < 5 && tokenResponse.getStatus() == 400
                && tokenResponse.getErrorType() == TokenErrorResponseType.AUTHORIZATION_PENDING);

        AssertBuilder.tokenResponse(tokenResponse).bad(TokenErrorResponseType.EXPIRED_TOKEN).check();
    }

    /**
     * Test big expiration times are not allowed.
     */
    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint", "backchannelUserCode", "userId"})
    @Test
    public void backchannelBigExpirationTimeAreNotAlloed(
            final String clientJwksUri, final String backchannelClientNotificationEndpoint, final String backchannelUserCode,
            final String userId) throws InterruptedException {
        showTitle("backchannelBigExpirationTimeAreNotAlloed");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .notNullRegistrationClientUri()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
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
        backchannelAuthenticationRequest.setRequestedExpiry(10000000);
        backchannelAuthenticationRequest.setAcrValues(Arrays.asList("auth_ldap_server", "basic"));
        backchannelAuthenticationRequest.setBindingMessage(bindingMessage);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).bad(BackchannelAuthenticationErrorResponseType.INVALID_REQUEST)
                .nullAuthReqId()
                .nullExpiresIn()
                .check();
    }

}