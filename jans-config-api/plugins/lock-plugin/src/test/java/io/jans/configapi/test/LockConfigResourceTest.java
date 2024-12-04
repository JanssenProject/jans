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

public class LockConfigResourceTest extends ConfigServerBaseTest{

    @Parameters({"issuer", "lockConfigUrl"})
    @Test
    public void getLockConfigUrlData(final String issuer, final String lockConfigUrl) {
        log.error("getLockConfigUrlData() - accessToken:{}, issuer:{}, lockConfigUrl:{}", accessToken, issuer, lockConfigUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + lockConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getLockConfigUrlData -  response:{}", response);
    }
    
	

}
