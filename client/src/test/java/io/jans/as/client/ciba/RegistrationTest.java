/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.Collections;

import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_TOKEN_DELIVERY_MODE;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_USER_CODE_PARAMETER;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.JWKS;
import static io.jans.as.model.register.RegisterRequestParam.JWKS_URI;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static io.jans.as.model.register.RegisterRequestParam.SECTOR_IDENTIFIER_URI;
import static io.jans.as.model.register.RegisterRequestParam.SUBJECT_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD;
import static io.jans.as.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_SIGNING_ALG;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version May 20, 2020
 */
public class RegistrationTest extends BaseTest {

    @Parameters({"clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll1(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll1");

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
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll2(final String sectorIdentifierUri, final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Test
    public void backchannelTokenDeliveryModePoll3() {
        showTitle("backchannelTokenDeliveryModePoll3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll4(final String sectorIdentifierUri, final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        clientUpdateRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"clientJwksUri"})
    public void backchannelTokenDeliveryModePoll5(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll5");

        // 1. Dynamic Client Registration
        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwks(jwkResponse.getJwks().toString());
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS256);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setBackchannelUserCodeParameter(false);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
        assertNotNull(registerResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(registerResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(registerResponse.getClaims().get(JWKS.toString()));
        assertNotNull(registerResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(registerResponse.getClaims().get(SCOPE.toString()));

        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(registerResponse.getClaims().containsKey(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.PS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.FALSE.toString());
        assertEquals(registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());

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
        assertNotNull(clientReadResponse.getClaims().get(JWKS.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.POLL.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.PS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.FALSE.toString());
        assertEquals(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(clientReadResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(clientReadResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing1(final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing1");

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
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing2(final String sectorIdentifierUri, final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
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
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing3(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing4(final String sectorIdentifierUri, final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        clientUpdateRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    public void backchannelTokenDeliveryModePing5(final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing5");

        // 1. Dynamic Client Registration
        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwks(jwkResponse.getJwks().toString());
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.PS256);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setBackchannelUserCodeParameter(false);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
        assertNotNull(registerResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(registerResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(registerResponse.getClaims().get(JWKS.toString()));
        assertNotNull(registerResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(registerResponse.getClaims().get(SCOPE.toString()));

        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(registerResponse.getClaims().containsKey(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.PS256.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.FALSE.toString());
        assertEquals(registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());

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
        assertNotNull(clientReadResponse.getClaims().get(JWKS.toString()));
        assertNotNull(clientReadResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(clientReadResponse.getClaims().get(SCOPE.toString()));

        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertTrue(clientReadResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), BackchannelTokenDeliveryMode.PING.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), AsymmetricSignatureAlgorithm.PS256.getValue());
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.FALSE.toString());
        assertEquals(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(clientReadResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(clientReadResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush1(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush1");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"sectorIdentifierUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush2(final String sectorIdentifierUri, final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush2");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush3(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush3");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.RS256);
        registerRequest.setBackchannelUserCodeParameter(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));

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
        assertEquals(clientReadResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"sectorIdentifierUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush4(final String sectorIdentifierUri, final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush4");

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
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
        clientUpdateRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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
        assertEquals(clientUpdateResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), Boolean.toString(true));
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail1(final String clientJwksUri) {
        showTitle("registrationFail1");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(null); // Missing jwks_uri
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(null); // Missing jwks_uri
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

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