/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui.test;


import io.jans.ca.plugin.adminui.AdminUIBaseTest;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class LicenseResourceTest extends AdminUIBaseTest {

    /**
     * Test License Details
     */
    @Parameters({ "test.issuer", "checkActiveLicenseURL" })
    @Test
    public void getLicenseDetails(final String issuer, final String checkActiveLicenseURL) {
        log.info("getLicenseDetails() - accessToken:{}, issuer:{}, checkActiveLicenseURL:{}", accessToken, issuer,
                checkActiveLicenseURL);
        Builder request = getResteasyService().getClientBuilder(issuer + checkActiveLicenseURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        //assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("\n\n Response for getLicenseDetails -  response:{}", response);
    }

}
