/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.dev.manual;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AccessTokenManualTest extends BaseTest {

    private static void sleepSeconds(int i) throws InterruptedException {
        Thread.sleep(i * 1000);
    }

    @Parameters({"userId", "userSecret", "redirectUri", "clientId"})
    @Test
    public void accessTokenExpiration(final String userId, final String userSecret, final String redirectUri, String clientId) throws Exception {
        showTitle("accessTokenExpiration");

        // Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN, ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String accessToken = authorizationResponse.getAccessToken();

        System.out.println("access_token: " + accessToken);

        for (int i = 0; i < 100; i++) {
            requestUserInfo(accessToken);

            sleepSeconds(10);

            System.out.println("Obtained user info successfully, seconds: " + ((i + 1) * 10));
        }
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
    }
}
