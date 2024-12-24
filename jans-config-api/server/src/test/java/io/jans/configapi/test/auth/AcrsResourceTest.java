/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import io.jans.configapi.ConfigServerBaseTest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class AcrsResourceTest extends ConfigServerBaseTest{

    @Parameters({"issuer", "acrsUrl"})
    @Test
    public void getDefaultAuthenticationMethod(final String issuer, final String acrsUrl) {
        log.info("accessToken:{}, issuer:{}, acrsUrl:{}", accessToken, issuer, acrsUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + acrsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getDefaultAuthenticationMethod -  response:{}", response);
    }
    
	
	@Parameters({"issuer", "acrsUrl", "default_acr_1"})
    @Test
    public void postClient(final String issuer, final String acrsUrl, final String json) {
        log.info("accessToken:{}, issuer:{}, acrsUrl:{}, json:{}", accessToken, issuer, acrsUrl, json);
        Builder request = getResteasyService().getClientBuilder(issuer + acrsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.put(Entity.entity(json, MediaType.APPLICATION_JSON));
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getApiConfigtion -  response:{}", response);
 
    }
}
