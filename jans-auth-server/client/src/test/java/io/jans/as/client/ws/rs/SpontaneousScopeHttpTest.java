/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;



import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SpontaneousScopeHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test
    public void spontaneousScope(final String userId, final String userSecret, final String redirectUri) throws Exception {
        showTitle("spontaneousScope");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN, ResponseType.TOKEN);

        RegisterResponse registerResponse = registerClient(redirectUri, responseTypes);

        String clientId = registerResponse.getClientId();

        // Request authorization and receive the authorization code.
        List<String> scopes = Lists.newArrayList("openid", "profile", "address", "email", "phone", "user_name",
                "transaction:245", "transaction:8645");
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId);

        final String[] responseScopes = authorizationResponse.getScope().split(" ");

        // Validate spontaneous scopes are present
        assertTrue(Arrays.asList(responseScopes).contains("transaction:245"));
        assertTrue(Arrays.asList(responseScopes).contains("transaction:8645"));
        assertFalse(Arrays.asList(responseScopes).contains("transaction:not_requested"));
    }

    private RegisterResponse registerClient(String redirectUris, List<ResponseType> responseTypes) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "Spontaneous scope test", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(Lists.newArrayList("openid", "profile", "address", "email", "phone", "user_name"));

        // 1. allow spontaneous scopes (off by default)
        // 2. set spontaneous scope regular expression. In this example `transaction:345236456`
        registerRequest.setAllowSpontaneousScopes(true);
        registerRequest.setSpontaneousScopes(Lists.newArrayList("^transaction:.+$"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientEngine(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId) {
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
    }

    /*
    public static void main(String[] args) throws JsonProcessingException {
        System.out.println(Pattern.matches("^transaction:.+$", "openid"));
        System.out.println(Pattern.matches("^transaction:.+$", "transaction"));
        System.out.println(Pattern.matches("^transaction:.+$", "transaction:"));
        System.out.println(Pattern.matches("^transaction:.+$", "transaction:bla"));
    }*/
}
