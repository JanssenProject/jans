/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.*;
import org.apache.commons.lang.RandomStringUtils;
import io.jans.as.client.BaseTest;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.token.TokenErrorResponseType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.UUID;

import static io.jans.as.model.register.RegisterRequestParam.*;
import static org.testng.Assert.*;

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
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(backchannelAuthenticationResponse.getStatus(), 200, "Unexpected response code: " + backchannelAuthenticationResponse.getEntity());
        assertNotNull(backchannelAuthenticationResponse.getAuthReqId());
        assertNotNull(backchannelAuthenticationResponse.getExpiresIn());
        assertNotNull(backchannelAuthenticationResponse.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.

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

        assertEquals(tokenResponse.getStatus(), 400, "Unexpected HTTP status resposne: " + tokenResponse.getEntity());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.EXPIRED_TOKEN, "Unexpected error type, should be expired_token.");
        assertNotNull(tokenResponse.getErrorDescription());
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
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(backchannelAuthenticationResponse.getStatus(), 200, "Unexpected response code: " + backchannelAuthenticationResponse.getEntity());
        assertNotNull(backchannelAuthenticationResponse.getAuthReqId());
        assertNotNull(backchannelAuthenticationResponse.getExpiresIn());
        assertNotNull(backchannelAuthenticationResponse.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.

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

        assertEquals(tokenResponse.getStatus(), 400, "Unexpected HTTP status resposne: " + tokenResponse.getEntity());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.EXPIRED_TOKEN, "Unexpected error type, should be expired_token.");
        assertNotNull(tokenResponse.getErrorDescription());
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
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(backchannelAuthenticationResponse.getStatus(), 400, "Unexpected response code: " + backchannelAuthenticationResponse.getEntity());
        assertNull(backchannelAuthenticationResponse.getAuthReqId());
        assertNull(backchannelAuthenticationResponse.getExpiresIn());
        assertEquals(backchannelAuthenticationResponse.getErrorType(), BackchannelAuthenticationErrorResponseType.INVALID_REQUEST);
        assertNotNull(backchannelAuthenticationResponse.getErrorDescription());
    }

}