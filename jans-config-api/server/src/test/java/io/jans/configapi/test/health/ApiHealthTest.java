/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.health;

import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Parameters;

public class ApiHealthTest extends ConfigServerBaseTest {

    @Parameters({ "test.issuer", "healthUrl" })
    @Test
    public void getAppHealth(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
        Response response = getHealthResponse(issuer + healthUrl);
        log.info("Response for getAppHealth -  response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

    @Parameters({ "test.issuer", "healthUrl" })
    @Test
    public void getServerStat(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
        Response response = getHealthResponse(issuer + healthUrl + "/server-stat");
        log.info("Response for getServerStat -  response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

    @Parameters({ "test.issuer", "healthUrl" })
    @Test
    public void getApplicationVersion(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
        Response response = getHealthResponse(issuer + healthUrl + "/app-version");
        log.info("Response for getApplicationVersion -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

    @Parameters({ "test.issuer", "healthUrl" })
    @Test
    public void getServiceStatus(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
        Response response = getHealthResponse(issuer + healthUrl + "/service-status");
        log.info("Response for getServiceStatus -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

    private Response getHealthResponse(final String healthUrl) {
        log.info("accessToken:{}, healthUrl:{}", accessToken, healthUrl);

        Builder request = getResteasyService().getClientBuilder(healthUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        return request.get();
    }
}
