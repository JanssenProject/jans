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

    @Parameters({ "issuer", "healthUrl" })
    @Test
    public void getHealthResponse(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + healthUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getHealthResponse -  response:{}", response);
    }

    @Parameters({ "issuer", "healthUrl" })
    @Test
    public void getServerStat(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + healthUrl + "/server-stat");
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getServerStat -  response:{}", response);
    }

    @Parameters({ "issuer", "healthUrl" })
    @Test
    public void getApplicationVersion(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + healthUrl + "/app-version");
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getApplicationVersion -  response:{}", response);
    }

    @Parameters({ "issuer", "healthUrl" })
    @Test
    public void getServiceStatus(final String issuer, final String healthUrl) {
        log.info("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + healthUrl + "/service-status");
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getServiceStatus -  response:{}", response);
    }
}
