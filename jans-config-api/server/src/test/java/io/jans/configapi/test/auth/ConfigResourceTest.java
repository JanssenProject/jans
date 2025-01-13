/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import static org.testng.Assert.assertEquals;

import io.jans.configapi.ConfigServerBaseTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class ConfigResourceTest extends ConfigServerBaseTest {

    @Parameters({ "test.issuer", "apiConfigtionUrl" })
    @Test
    public void getApiConfigtion(final String issuer, final String apiConfigtionUrl) {
        log.info("getApiConfigtion() - accessToken:{}, issuer:{}, apiConfigtionUrl:{}", accessToken, issuer,
                apiConfigtionUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + apiConfigtionUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.info("Response for getApiConfigtion -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

    }

    @Parameters({ "test.issuer", "apiConfigtionUrl", "api_config_patch_1" })
    @Test
    public void patchgetApiConfigtion(final String issuer, final String apiConfigtionUrl, final String json) {
        log.info("getApiConfigtion() - accessToken:{}, issuer:{}, apiConfigtionUrl:{}, json:{}", accessToken, issuer,
                apiConfigtionUrl, json);
        Builder request = getResteasyService().getClientBuilder(issuer + apiConfigtionUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);

        Response response = request.method(HttpMethod.PATCH,
                Entity.entity(json, MediaType.APPLICATION_JSON_PATCH_JSON));
        log.info("Response for getApiConfigtion -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }
}
