/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.core.test.BaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Parameters;

public class ScimConfigResourceTest extends BaseTest {

    @Parameters({"issuer", "scimConfigUrl"})
    @Test
    public void getScimConfigData(final String issuer, final String scimConfigUrl) {
        log.error("getScimConfigData() - accessToken:{}, issuer:{}, scimConfigUrl:{}", accessToken, issuer, scimConfigUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + scimConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getScimConfigData -  response:{}", response);
    }
    
	

}
