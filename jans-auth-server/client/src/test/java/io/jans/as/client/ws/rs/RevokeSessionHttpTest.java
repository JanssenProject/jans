/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.RevokeSessionClient;
import io.jans.as.client.RevokeSessionRequest;
import io.jans.as.client.RevokeSessionResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RevokeSessionHttpTest extends BaseTest {

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri", "umaPatClientId", "umaPatClientSecret"})
    @Test
    public void revokeSession(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri, String umaPatClientId, String umaPatClientSecret) throws Exception {
        showTitle("revokeSession");

        final AuthenticationMethod authnMethod = AuthenticationMethod.CLIENT_SECRET_BASIC;

        // 1. Register client
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        registerRequest.setTokenEndpointAuthMethod(authnMethod);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        // 3. Request authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The ID Token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");

        RevokeSessionRequest revokeSessionRequest = new RevokeSessionRequest("uid", "test");
        revokeSessionRequest.setAuthenticationMethod(authnMethod);
        revokeSessionRequest.setAuthUsername(umaPatClientId); // it must be client with revoke_session scope
        revokeSessionRequest.setAuthPassword(umaPatClientSecret);

        RevokeSessionClient revokeSessionClient = newRevokeSessionClient(revokeSessionRequest);
        final RevokeSessionResponse revokeSessionResponse = revokeSessionClient.exec();

        showClient(revokeSessionClient);

        assertEquals(revokeSessionResponse.getStatus(), 200);
    }
}
