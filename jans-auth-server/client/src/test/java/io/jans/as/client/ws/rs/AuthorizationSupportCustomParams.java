/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;



import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version February 2, 2022
 */
public class AuthorizationSupportCustomParams extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void authorizationSupportCustomParams(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationSupportCustomParams");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

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
        List<String> scopes = Arrays.asList("openid", "test");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.addCustomParameter("customParam1", "value1"); // returnInResponse = false
        authorizationRequest.addCustomParameter("customParam2", "value2"); // returnInResponse = false
        authorizationRequest.addCustomParameter("customParam3", "value3"); // returnInResponse = false
        authorizationRequest.addCustomParameter("customParam4", "value4"); // returnInResponse = true
        authorizationRequest.addCustomParameter("customParam5", "value5"); // returnInResponse = true

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        assertNotNull(authorizationResponse.getCustomParams());
        assertFalse(authorizationResponse.getCustomParams().containsKey("customParam1"));
        assertFalse(authorizationResponse.getCustomParams().containsKey("customParam2"));
        assertFalse(authorizationResponse.getCustomParams().containsKey("customParam3"));
        assertTrue(authorizationResponse.getCustomParams().containsKey("customParam4"));
        assertTrue(authorizationResponse.getCustomParams().containsKey("customParam5"));
        assertEquals(authorizationResponse.getCustomParams().get("customParam4"), "value4");
        assertEquals(authorizationResponse.getCustomParams().get("customParam5"), "value5");

        // NOTE: After complete successfully this test, check whether the stored session in LDAP has the 3 custom params
        // stored in its session attributes list.
    }
}
