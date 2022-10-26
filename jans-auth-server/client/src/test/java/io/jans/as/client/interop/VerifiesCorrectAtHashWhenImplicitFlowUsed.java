/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;

/**
 * OC5:FeatureTest-Verifies Correct at hash when Implicit Flow Used
 *
 * @author Javier Rojas Blum
 * @version November 3, 2016
 */
public class VerifiesCorrectAtHashWhenImplicitFlowUsed extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void verifiesCorrectAtHashWhenImplicitFlowUsed(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Verifies Correct at hash when Implicit Flow Used");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getAccessToken());
        assertNotNull(authorizationResponse.getTokenType());
        assertNotNull(authorizationResponse.getIdToken());
        assertNotNull(authorizationResponse.getState());

        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();

        // 3. Validate access_token and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .accessToken(accessToken)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
    }
}