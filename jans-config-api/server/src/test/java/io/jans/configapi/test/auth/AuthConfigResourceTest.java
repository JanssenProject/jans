/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import jakarta.ws.rs.HttpMethod;
import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Parameters;


public class AuthConfigResourceTest extends ConfigServerBaseTest{

    @Parameters({"issuer", "authConfigurationUrl"})
    @Test
    public void getAuthConfigurationProperty(final String issuer, final String authConfigurationUrl) {
        log.error("accessToken:{}, issuer:{}, authConfigurationUrl:{}", accessToken, issuer, authConfigurationUrl);

            Builder request = getResteasyService().getClientBuilder(issuer + authConfigurationUrl);
            request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

            Response response = request.get();
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            log.error("Response for getDefaultAuthenticationMethod -  response:{}", response);
    }
    
    @Parameters({"issuer", "authConfigurationUrl", "auth_config_patch_1"})
    @Test
    public void patchAuthConfigurationProperty(final String issuer, final String authConfigurationUrl, final String json) {
        log.error("getApiConfigtion() - accessToken:{}, issuer:{}, authConfigurationUrl:{}, json:{}", accessToken, issuer,
                authConfigurationUrl, json);
        Builder request = getResteasyService().getClientBuilder(issuer + authConfigurationUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);

        Response response = request.method(HttpMethod.PATCH, Entity.entity(json, MediaType.APPLICATION_JSON));

        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getApiConfigtion -  response:{}", response);
    }
}
