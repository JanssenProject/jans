/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;

import javax.ws.rs.HttpMethod;
import java.util.Arrays;

import static org.testng.Assert.*;
import static org.gluu.oxauth.model.register.RegisterRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class RegistrationTest extends BaseTest {

    @Parameters({"clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll1(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll1");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(JWKS_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll2(final String sectorIdentifierUri, final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
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

        assertTrue(registerResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(registerResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(JWKS_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Test
    public void backchannelTokenDeliveryModePoll3() {
        showTitle("backchannelTokenDeliveryModePoll3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll4(final String sectorIdentifierUri, final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        // 3. Client Update
        RegisterRequest clientUpdateRequest = new RegisterRequest(registrationAccessToken);
        clientUpdateRequest.setHttpMethod(HttpMethod.PUT);

        clientUpdateRequest.setSectorIdentifierUri(sectorIdentifierUri);
        clientUpdateRequest.setJwksUri(clientJwksUri);
        clientUpdateRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        clientUpdateRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        clientUpdateRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        clientUpdateRequest.setBackchannelUserCodeParameter(true);

        RegisterClient clientUpdateClient = new RegisterClient(registrationClientUri);
        clientUpdateClient.setRequest(clientUpdateRequest);

        RegisterResponse clientUpdateResponse = clientUpdateClient.exec();

        showClient(clientUpdateClient);
        assertEquals(clientUpdateResponse.getStatus(), 200, "Unexpected response code: " + clientUpdateResponse.getEntity());
        assertNotNull(clientUpdateResponse.getClientId());
        assertNotNull(clientUpdateResponse.getClientSecret());
        assertNotNull(clientUpdateResponse.getRegistrationAccessToken());
        assertNotNull(clientUpdateResponse.getRegistrationClientUri());
        assertNotNull(clientUpdateResponse.getClientSecretExpiresAt());
        assertNotNull(clientUpdateResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(JWKS_URI.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientUpdateResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing1(final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing1");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(JWKS_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing2(final String sectorIdentifierUri, final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
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

        assertTrue(registerResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(registerResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(JWKS_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing3(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing4(final String sectorIdentifierUri, final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        // 3. Client Update
        RegisterRequest clientUpdateRequest = new RegisterRequest(registrationAccessToken);
        clientUpdateRequest.setHttpMethod(HttpMethod.PUT);

        clientUpdateRequest.setSectorIdentifierUri(sectorIdentifierUri);
        clientUpdateRequest.setJwksUri(clientJwksUri);
        clientUpdateRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        clientUpdateRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        clientUpdateRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        clientUpdateRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        clientUpdateRequest.setBackchannelUserCodeParameter(true);

        RegisterClient clientUpdateClient = new RegisterClient(registrationClientUri);
        clientUpdateClient.setRequest(clientUpdateRequest);

        RegisterResponse clientUpdateResponse = clientUpdateClient.exec();

        showClient(clientUpdateClient);
        assertEquals(clientUpdateResponse.getStatus(), 200, "Unexpected response code: " + clientUpdateResponse.getEntity());
        assertNotNull(clientUpdateResponse.getClientId());
        assertNotNull(clientUpdateResponse.getClientSecret());
        assertNotNull(clientUpdateResponse.getRegistrationAccessToken());
        assertNotNull(clientUpdateResponse.getRegistrationClientUri());
        assertNotNull(clientUpdateResponse.getClientSecretExpiresAt());
        assertNotNull(clientUpdateResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(JWKS_URI.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientUpdateResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush1(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush1");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"sectorIdentifierUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush2(final String sectorIdentifierUri, final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
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

        assertTrue(registerResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush3(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"sectorIdentifierUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush4(final String sectorIdentifierUri, final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertEquals(clientReadResponse.getStatus(), 200, "Unexpected response code: " + clientReadResponse.getEntity());
        assertNotNull(clientReadResponse.getClientId());
        assertNotNull(clientReadResponse.getClientSecret());
        assertNotNull(clientReadResponse.getRegistrationAccessToken());
        assertNotNull(clientReadResponse.getRegistrationClientUri());
        assertNotNull(clientReadResponse.getClientSecretExpiresAt());
        assertNotNull(clientReadResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        // 3. Client Update
        RegisterRequest clientUpdateRequest = new RegisterRequest(registrationAccessToken);
        clientUpdateRequest.setHttpMethod(HttpMethod.PUT);

        clientUpdateRequest.setSectorIdentifierUri(sectorIdentifierUri);
        clientUpdateRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        clientUpdateRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
        clientUpdateRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        clientUpdateRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        clientUpdateRequest.setBackchannelUserCodeParameter(true);

        RegisterClient clientUpdateClient = new RegisterClient(registrationClientUri);
        clientUpdateClient.setRequest(clientUpdateRequest);

        RegisterResponse clientUpdateResponse = clientUpdateClient.exec();

        showClient(clientUpdateClient);
        assertEquals(clientUpdateResponse.getStatus(), 200, "Unexpected response code: " + clientUpdateResponse.getEntity());
        assertNotNull(clientUpdateResponse.getClientId());
        assertNotNull(clientUpdateResponse.getClientSecret());
        assertNotNull(clientUpdateResponse.getRegistrationAccessToken());
        assertNotNull(clientUpdateResponse.getRegistrationClientUri());
        assertNotNull(clientUpdateResponse.getClientSecretExpiresAt());
        assertNotNull(clientUpdateResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientUpdateResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientUpdateResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientUpdateResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PUSH.getValue());
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.RS256.getValue());
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), new Boolean(true).toString());
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail1(final String clientJwksUri) {
        showTitle("registrationFail1");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(null); // Missing backchannel_token_delivery_mode
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail2(final String clientJwksUri) {
        showTitle("registrationFail2");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(null); // Missing backchannel_client_notification_endpoint
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail3(final String clientJwksUri) {
        showTitle("registration3");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
        registerRequest.setBackchannelClientNotificationEndpoint(null); // Missing backchannel_client_notification_endpoint
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail4(final String clientJwksUri, final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail4");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Arrays.asList()); // Missing  grant type urn:openid:params:grant-type:ciba

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail5(final String clientJwksUri, final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail5");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Arrays.asList()); // Missing  grant type urn:openid:params:grant-type:ciba

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail6(final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail6");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(null); // Missing jwks_uri
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail7(final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail7");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(null); // Missing jwks_uri
        registerRequest.setGrantTypes(Arrays.asList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }
}