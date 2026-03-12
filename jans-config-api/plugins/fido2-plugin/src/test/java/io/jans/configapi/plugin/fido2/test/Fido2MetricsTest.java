/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.test;

import io.jans.configapi.plugin.fido2.Fido2BaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;

public class Fido2MetricsTest extends Fido2BaseTest {

    @Parameters({ "test.issuer", "fido2MetricsUrl" , "fido2MetricsEntries"})
    @Test
    public void getFido2MetricsEntry(final String issuer, final String fido2MetricsUrl, final String fido2MetricsEntries) {
        log.debug(" getFido2MetricsEntry() - issuer:{}, fido2MetricsUrl:{}, fido2MetricsEntries:{}", issuer, fido2MetricsUrl, fido2MetricsEntries);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2MetricsEntries);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("getFido2MetricsEntry() - response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        
    }
    
    @Parameters({ "test.issuer", "fido2MetricsUrl" , "fido2UserMetrics"})
    @Test
    public void getFido2UserMetrics(final String issuer, final String fido2MetricsUrl, final String fido2UserMetrics) {
        log.debug("\n\n getFido2UserMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2UserMetrics:{}", issuer, fido2MetricsUrl, fido2UserMetrics);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2UserMetrics);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("getFido2UserMetrics() -  response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        
    }
    
    @Parameters({ "test.issuer", "fido2MetricsUrl" , "fido2MetricsConfig"})
    @Test
    public void getFido2MetricsConfig(final String issuer, final String fido2MetricsUrl, final String fido2MetricsConfig) {
        log.debug("getFido2MetricsConfig() - issuer:{}, fido2MetricsUrl:{}, fido2UserMetrics:{}", issuer, fido2MetricsUrl, fido2MetricsConfig);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2MetricsConfig);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("getFido2Configuration() - response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        
    }

}
