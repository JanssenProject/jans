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
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static io.jans.as.client.client.Asserter.assertRegisterResponseClaimsAreContained;
import static io.jans.as.client.client.Asserter.assertRegisterResponseClaimsNotNull;
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
import static org.testng.Assert.assertTrue;

public class RegistrationTest extends BaseTest {

    @Parameters({"clientJwksUri"})
    @Test

    // todo all our names should be names as X_Y_Z, where X is what we call, Y under what condition, Z what do we expect.
    public void backchannelTokenDeliveryModePoll_whenCallWithValidData_shouldRegisterSuccessfully(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll_whenCallWithValidData_shouldRegisterSuccessfully");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .withBackchannelUserCodeParameter(true)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelUserCodeParameter(true)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll2(final String sectorIdentifierUri, final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll2");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withSectorIdentifierUri(sectorIdentifierUri)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .withBackchannelUserCodeParameter(true)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsAreContained(registerResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelUserCodeParameter(true)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
    }

    @Test
    public void backchannelTokenDeliveryModePoll3() {
        showTitle("backchannelTokenDeliveryModePoll3");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withSubjectType(SubjectType.PUBLIC)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .withBackchannelUserCodeParameter(true)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelUserCodeParameter(true)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri"})
    @Test
    public void backchannelTokenDeliveryModePoll4(final String sectorIdentifierUri, final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll4");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse).checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .checkAsserts();
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);

        // 3. Client Update
        RegisterResponse clientUpdateResponse = RegisterClient.builder()
                .withRegistrationAccessToken(registrationAccessToken)
                .withRegistrationEndpoint(registrationClientUri)
                .withSectorIdentifierUri(sectorIdentifierUri)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .isUpdateMode()
                .execute();
        AssertBuilder.registerResponse(clientUpdateResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientUpdateResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientUpdateResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
    }

    @Parameters({"clientJwksUri"})
    public void backchannelTokenDeliveryModePoll5(final String clientJwksUri) {
        showTitle("backchannelTokenDeliveryModePoll5");

        // 1. Dynamic Client Registration
        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwks(jwkResponse.getJwks().toString())
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(false)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .withTokenSignedResponseAlgorithm(SignatureAlgorithm.PS256)
                .withTokenEndPointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT)
                .withTokenEndPointAuthSigningAlgorithm(SignatureAlgorithm.PS256)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(false)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(registerResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(registerResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);

        assertEquals(registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelUserCodeParameter(false)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);

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
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing2(final String sectorIdentifierUri, final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing2");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withSectorIdentifierUri(sectorIdentifierUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();
        assertRegisterResponseClaimsAreContained(registerResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing3(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing3");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withSubjectType(SubjectType.PUBLIC)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
    }

    @Parameters({"sectorIdentifierUri", "clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePing4(final String sectorIdentifierUri, final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing4");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse).checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .notNullRegistrationClientUri()
                .checkAsserts();
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);

        // 3. Client Update
        RegisterResponse clientUpdateResponse = RegisterClient.builder()
                .withRegistrationAccessToken(registrationAccessToken)
                .withRegistrationEndpoint(registrationClientUri)
                .withSectorIdentifierUri(sectorIdentifierUri)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .isUpdateMode()
                .execute();
        AssertBuilder.registerResponse(clientUpdateResponse)
                .status(200)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientUpdateResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientUpdateResponse, JWKS_URI, SECTOR_IDENTIFIER_URI);
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    public void backchannelTokenDeliveryModePing5(final String clientJwksUri,
                                                  final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePing5");

        // 1. Dynamic Client Registration
        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwks(jwkResponse.getJwks().toString())
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(false)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withTokenSignedResponseAlgorithm(SignatureAlgorithm.PS256)
                .withTokenEndPointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT)
                .withTokenEndPointAuthSigningAlgorithm(SignatureAlgorithm.PS256)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(false)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(registerResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(registerResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);

        assertEquals(registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .backchannelUserCodeParameter(false)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.PS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, ID_TOKEN_SIGNED_RESPONSE_ALG, TOKEN_ENDPOINT_AUTH_SIGNING_ALG, TOKEN_ENDPOINT_AUTH_METHOD);

        assertEquals(clientReadResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(clientReadResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()), SignatureAlgorithm.PS256.getName());
        assertEquals(clientReadResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), AuthenticationMethod.PRIVATE_KEY_JWT.toString());
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush1(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush1");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();
        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
    }

    @Parameters({"sectorIdentifierUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush2(final String sectorIdentifierUri, final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush2");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withSectorIdentifierUri(sectorIdentifierUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();
        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();
        assertTrue(registerResponse.getClaims().containsKey(SECTOR_IDENTIFIER_URI.toString()));

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientReadResponse, SECTOR_IDENTIFIER_URI);
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush3(final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush3");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withSubjectType(SubjectType.PUBLIC)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();
        AssertBuilder.registerResponseCreated(registerResponse)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();
        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
    }

    @Parameters({"sectorIdentifierUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void backchannelTokenDeliveryModePush4(final String sectorIdentifierUri, final String backchannelClientNotificationEndpoint) {
        showTitle("backchannelTokenDeliveryModePush4");

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .execute();

        AssertBuilder.registerResponseCreated(registerResponse).checkAsserts();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Read
        RegisterResponse clientReadResponse = RegisterClient.builder()
                .withRegistrationEndpoint(registrationClientUri)
                .withRegistrationAccessToken(registrationAccessToken)
                .isReadMode()
                .execute();
        AssertBuilder.registerResponse(clientReadResponse)
                .status(200)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientReadResponse, APPLICATION_TYPE, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);

        // 3. Client Update
        RegisterResponse clientUpdateResponse = RegisterClient.builder()
                .withRegistrationAccessToken(registrationAccessToken)
                .withRegistrationEndpoint(registrationClientUri)
                .withSectorIdentifierUri(sectorIdentifierUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .isUpdateMode()
                .execute();

        AssertBuilder.registerResponse(clientUpdateResponse)
                .status(200)
                .backchannelUserCodeParameter(true)
                .backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .checkAsserts();

        assertRegisterResponseClaimsNotNull(clientUpdateResponse, APPLICATION_TYPE, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, SCOPE);
        assertRegisterResponseClaimsAreContained(clientUpdateResponse, SECTOR_IDENTIFIER_URI);
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail1(final String clientJwksUri) { // todo not clear why it should fail, we need X_Y_Z pattern here and in all names
        showTitle("registrationFail1");

        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(null)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response)
                .status(400).
                checkAsserts();
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail2(final String clientJwksUri) {
        showTitle("registrationFail2");
        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelClientNotificationEndPoint(null) // Missing backchannel_client_notification_endpoint
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response)
                .status(400)
                .checkAsserts();
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void registrationFail3(final String clientJwksUri) {
        showTitle("registration3");
        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PUSH)
                .withBackchannelClientNotificationEndPoint(null) // Missing backchannel_client_notification_endpoint
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response).status(400).checkAsserts();
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail4(final String clientJwksUri, final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail4");
        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Arrays.asList())// Missing  grant type urn:openid:params:grant-type:ciba
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response)
                .status(400)
                .checkAsserts();
    }

    @Parameters({"clientJwksUri", "backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail5(final String clientJwksUri, final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail5");
        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(clientJwksUri)
                .withGrantTypes(Arrays.asList())// Missing  grant type urn:openid:params:grant-type:ciba
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response) // todo we should think how to make it shorter, maybe just  AssertBuilder.registerResponseIsBadRequest(response) ?
                .status(400)
                .checkAsserts();
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail6(final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail6");
        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(null) // Missing jwks_uri // todo it is missed by default, no ?
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.PING)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response)
                .status(400)
                .checkAsserts();
    }

    @Parameters({"backchannelClientNotificationEndpoint"})
    @Test
    public void registrationFail7(final String backchannelClientNotificationEndpoint) {
        showTitle("registrationFail7");

        RegisterResponse response = RegisterClient.builder()
                .withRegistrationEndpoint(registrationEndpoint)
                .withJwksUri(null) // Missing jwks_uri // todo it is missed by default, no ?
                .withGrantTypes(Collections.singletonList(GrantType.CIBA))
                .withBackchannelUserCodeParameter(true)
                .withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.POLL)
                .withBackchannelClientNotificationEndPoint(backchannelClientNotificationEndpoint)
                .withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm.RS256)
                .execute();

        AssertBuilder.registerResponse(response).status(400).checkAsserts();
    }
}