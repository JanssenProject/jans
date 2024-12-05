/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.core.test.BaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Parameters;

public class JansKcSAMLConfigResourceTest extends BaseTest {

    @Parameters({"issuer", "samlConfigUrl"})
    @Test
    public void getKcSAMLConfiguration(final String issuer, final String samlConfigUrl) {
        log.error("getKcSAMLConfiguration() - accessToken:{}, issuer:{}, samlConfigUrl:{}", accessToken, issuer, samlConfigUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + samlConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getKcSAMLConfiguration -  response:{}", response);
    }
    
	

}
