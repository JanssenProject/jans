/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;

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