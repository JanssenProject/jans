/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui.test;

import io.jans.as.common.model.registration.Client;

import static io.restassured.RestAssured.given;
import io.jans.ca.plugin.adminui.AdminUIBaseTest;
import io.jans.model.net.HttpServiceResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

import org.apache.http.entity.ContentType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class LicenseResourceTest extends AdminUIBaseTest {

    /**
     * Test License Details
     */
    @Parameters({ "issuer", "licenseDetailsURL" })
    @Test
    public void getLicenseDetails(final String issuer, final String licenseDetailsURL) {
        log.error("getLicenseDetails() - accessToken:{}, issuer:{}, licenseDetailsURL:{}", accessToken, issuer,
                licenseDetailsURL);
        Builder request = getResteasyService().getClientBuilder(issuer + licenseDetailsURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getLicenseDetails -  response:{}", response);
    }

}
