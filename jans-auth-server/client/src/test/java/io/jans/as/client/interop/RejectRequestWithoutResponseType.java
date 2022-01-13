/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * OC5:FeatureTest-Reject Request Without response type
 *
 * @author Javier Rojas Blum Date: 07.31.2013
 */
public class RejectRequestWithoutResponseType extends BaseTest {

    @Parameters({"userId", "userSecret"})
    @Test
    public void rejectRequestWithoutResponseType(final String userId, final String userSecret) throws Exception {
        showTitle("OC5:FeatureTest-Reject Request Without response type");

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(null, null, null, null, null);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 400, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }
}