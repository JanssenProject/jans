/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.GrantType;
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
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version November 22, 2017
 */
public class ProvidingAcrValues extends BaseTest {

    @Parameters({"redirectUri", "clientJwksUri", "userId", "userSecret"})
    @Test
    public void providingAcrValues(final String redirectUri, final String jwksUri, final String userId, final String userSecret) throws Exception {
        showTitle("providingAcrValues");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT);
        List<String> contacts = Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUri));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setContacts(contacts);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();
        assertNotNull(response.getResponseTypes());
        assertTrue(response.getResponseTypes().containsAll(responseTypes));
        assertNotNull(response.getGrantTypes());
        assertTrue(response.getGrantTypes().containsAll(grantTypes));

        String clientId = response.getClientId();

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid");
        List<String> acrValues = Arrays.asList("basic");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAcrValues(acrValues);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }
}
