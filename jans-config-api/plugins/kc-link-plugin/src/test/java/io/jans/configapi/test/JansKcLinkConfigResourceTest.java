/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Parameters;

public class JansKcLinkConfigResourceTest extends ConfigServerBaseTest{

    @Parameters({"issuer", "kcLinkConfigUrl"})
    @Test
    public void getKcLinkConfiguration(final String issuer, final String kcLinkConfigUrl) {
        log.error("getKcLinkConfiguration() - accessToken:{}, issuer:{}, kcLinkConfigUrl:{}", accessToken, issuer, kcLinkConfigUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + kcLinkConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getKcLinkConfiguration -  response:{}", response);
    }
    
	

}
