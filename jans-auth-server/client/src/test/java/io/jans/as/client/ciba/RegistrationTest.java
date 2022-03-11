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

import jakarta.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.Collections;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertTrue(registerResponse.getClaims().containsKey(JWKS_URI.toString()));
        assertTrue(registerResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);

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
        assertRegisterResponseOk(clientUpdateResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientUpdateResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientUpdateResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(clientUpdateResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.POLL, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);
        assertRegisterResponseClaimsNotNull(registerResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(registerResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);
        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.PS256, BackchannelTokenDeliveryMode.POLL, false);

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
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.PS256, BackchannelTokenDeliveryMode.POLL, false);

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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);
        assertRegisterResponseClaimsAreContained(registerResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);

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
        assertRegisterResponseOk(clientUpdateResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientUpdateResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientUpdateResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(clientUpdateResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PING, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);
        assertRegisterResponseClaimsNotNull(registerResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(registerResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);
        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.PS256, BackchannelTokenDeliveryMode.PING, false);

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
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.PS256, BackchannelTokenDeliveryMode.PING, true);

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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertTrue(registerResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));
        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        assertRegisterResponseClaimsBackChannel(registerResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsBackChannel(clientReadResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);
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
        assertRegisterResponseOk(registerResponse, 201, true);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest clientReadRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient clientReadClient = new RegisterClient(registrationClientUri);
        clientReadClient.setRequest(clientReadRequest);
        RegisterResponse clientReadResponse = clientReadClient.exec();

        showClient(clientReadClient);
        assertRegisterResponseOk(clientReadResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);

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
        assertRegisterResponseOk(clientUpdateResponse, 200, true);
        assertRegisterResponseClaimsNotNull(clientUpdateResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientUpdateResponse, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsBackChannel(clientUpdateResponse, AsymmetricSignatureAlgorithm.RS256, BackchannelTokenDeliveryMode.PUSH, true);
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
        assertRegisterResponseFail(response);
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
        assertRegisterResponseFail(response);
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
        assertRegisterResponseFail(response);
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
        assertRegisterResponseFail(response);
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
        assertRegisterResponseFail(response);
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
        assertRegisterResponseFail(response);
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
        assertRegisterResponseFail(response);
    }
}