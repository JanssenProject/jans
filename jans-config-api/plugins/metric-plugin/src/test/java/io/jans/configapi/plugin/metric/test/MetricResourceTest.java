/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.test;

import io.jans.configapi.plugin.metric.MetricBaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;

public class MetricResourceTest extends MetricBaseTest {

    @Parameters({ "test.issuer", "metricTypesUrl" })
    @Test
    public void getMetricTypesWithInvalidToken(final String issuer, final String metricTypesUrl) {
        log.debug("getMetricTypesWithInvalidToken() - issuer:{}, metricTypesUrl:{}", issuer, metricTypesUrl);

        String invalidToken = this.getAccessTokenForGivenScope("https://jans.io/oauth/config/attributes.readonly");
        Builder request = getResteasyService().getClientBuilder(issuer + metricTypesUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + invalidToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricTypesWithInvalidToken() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricTypesUrl" })
    @Test
    public void getMetricTypes(final String issuer, final String metricTypesUrl) {
        log.debug("getMetricTypes() - issuer:{}, metricTypesUrl:{}", issuer, metricTypesUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricTypesUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricTypes() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricAppTypesUrl" })
    @Test
    public void getAppTypes(final String issuer, final String metricAppTypesUrl) {
        log.debug("getAppTypes() - issuer:{}, metricAppTypesUrl:{}", issuer, metricAppTypesUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricAppTypesUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getAppTypes() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricEntriesUrl" })
    @Test
    public void getMetricEntries(final String issuer, final String metricEntriesUrl) {
        log.debug("getMetricEntries() - issuer:{}, metricEntriesUrl:{}", issuer, metricEntriesUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricEntriesUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricEntries() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricEntriesLegacyDateUrl" })
    @Test
    public void getMetricEntriesWithLegacyDateFormat(final String issuer, final String metricEntriesLegacyDateUrl) {
        log.debug("getMetricEntriesWithLegacyDateFormat() - issuer:{}, metricEntriesLegacyDateUrl:{}", issuer,
                metricEntriesLegacyDateUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricEntriesLegacyDateUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricEntriesWithLegacyDateFormat() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricEntriesMissingTypeUrl" })
    @Test
    public void getMetricEntriesWithoutMetricType(final String issuer, final String metricEntriesMissingTypeUrl) {
        log.debug("getMetricEntriesWithoutMetricType() - issuer:{}, metricEntriesMissingTypeUrl:{}", issuer,
                metricEntriesMissingTypeUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricEntriesMissingTypeUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricEntriesWithoutMetricType() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricEntriesInvalidDateUrl" })
    @Test
    public void getMetricEntriesWithInvalidDate(final String issuer, final String metricEntriesInvalidDateUrl) {
        log.debug("getMetricEntriesWithInvalidDate() - issuer:{}, metricEntriesInvalidDateUrl:{}", issuer,
                metricEntriesInvalidDateUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricEntriesInvalidDateUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricEntriesWithInvalidDate() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
        }
    }

}
