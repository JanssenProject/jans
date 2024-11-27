/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

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

import static io.restassured.RestAssured.given;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ConfigResourceTest extends ConfigServerBaseTest {

    @Parameters({ "issuer", "apiConfigtionUrl" })
    @Test
    public void getApiConfigtion(final String issuer, final String apiConfigtionUrl) {
        log.error("getApiConfigtion() - accessToken:{}, issuer:{}, apiConfigtionUrl:{}", accessToken, issuer,
                apiConfigtionUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + apiConfigtionUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getApiConfigtion -  response:{}", response);

    }

    @Parameters({ "issuer", "apiConfigtionUrl", "api_config-patch" })
    // @Test
    public void patchgetApiConfigtion1(final String issuer, final String apiConfigtionUrl) {
        given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 0ea2ce99-b741-4f5a-8fd7-26f52d057c19", null)
                .body("[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\" } ]")
                .patch("/jans-config-api/api/v1/jans-auth-server/config").then().statusCode(200);

    }

    @Parameters({ "issuer", "apiConfigtionUrl", "api_config_patch_1" })
    @Test
    public void patchgetApiConfigtion(final String issuer, final String apiConfigtionUrl, final String json) {
        log.error("getApiConfigtion() - accessToken:{}, issuer:{}, apiConfigtionUrl:{}, json:{}", accessToken, issuer,
                apiConfigtionUrl, json);
        Builder request = getResteasyService().getClientBuilder(issuer + apiConfigtionUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);

        Response response = request.method(HttpMethod.PATCH, Entity.entity(json, MediaType.APPLICATION_JSON));

        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getApiConfigtion -  response:{}", response);

    }
}
