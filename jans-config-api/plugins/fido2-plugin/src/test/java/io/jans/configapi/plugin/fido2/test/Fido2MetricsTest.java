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

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2MetricsEntriesUrl" })
    @Test
    public void getFido2MetricsEntryWithInvalidToken(final String issuer, final String fido2MetricsUrl,
            final String fido2MetricsEntriesUrl) {
        log.debug(" getFido2MetricsEntryWithInvalidToken() - issuer:{}, fido2MetricsUrl:{}, fido2MetricsEntriesUrl:{}", issuer,
                fido2MetricsUrl, fido2MetricsEntriesUrl);

        String invalidToken = this.getAccessTokenForGivenScope("https://jans.io/oauth/config/attributes.readonly");
        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2MetricsEntriesUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + invalidToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2MetricsEntryWithInvalidToken() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2MetricsEntriesUrl" })
    @Test
    public void getFido2MetricsEntry(final String issuer, final String fido2MetricsUrl,
            final String fido2MetricsEntriesUrl) {
        log.debug(" getFido2MetricsEntry() - issuer:{}, fido2MetricsUrl:{}, fido2MetricsEntriesUrl:{}", issuer,
                fido2MetricsUrl, fido2MetricsEntriesUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2MetricsEntriesUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2MetricsEntry() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2UserMetricsUrl" })
    @Test
    public void getFido2UserMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2UserMetricsUrl) {
        log.debug("\n\n getfido2UserMetricsUrl() - issuer:{}, fido2MetricsUrl:{}, fido2UserMetricsUrl:{}", issuer,
                fido2MetricsUrl, fido2UserMetricsUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2UserMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2UserMetrics() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2OperationMetricsUrl" })
    @Test
    public void getFido2OperationMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2OperationMetricsUrl) {
        log.debug("\n\n getFido2OperationMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2OperationMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2OperationMetricsUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2OperationMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2OperationMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AggregationMetricsUrl" })
    @Test
    public void getFido2AggregationMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AggregationMetricsUrl) {
        log.debug("\n\n getFido2AggregationMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AggregationMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AggregationMetricsUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2AggregationMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2AggregationMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AggregationSummaryMetricsUrl" })
    @Test
    public void getFido2AggregationSummaryMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AggregationSummaryMetricsUrl) {
        log.debug(
                "\n\n getFido2AggregationSummaryMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AggregationSummaryMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AggregationSummaryMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AggregationSummaryMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2AggregationSummaryMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AnalyticsAdoptionMetricsUrl" })
    @Test
    public void getAnalyticsAdoptionMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AnalyticsAdoptionMetricsUrl) {
        log.debug(
                "\n\n getAnalyticsAdoptionMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AnalyticsAdoptionMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AnalyticsAdoptionMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AnalyticsAdoptionMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getAnalyticsAdoptionMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AnalyticsPerformanceMetricsUrl" })
    @Test
    public void getAnalyticsPerformanceMetric(final String issuer, final String fido2MetricsUrl,
            final String fido2AnalyticsPerformanceMetricsUrl) {
        log.debug(
                "\n\n getAnalyticsPerformanceMetric() - issuer:{}, fido2MetricsUrl:{}, fido2AnalyticsPerformanceMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AnalyticsPerformanceMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AnalyticsPerformanceMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getAnalyticsPerformanceMetric() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AnalyticsDevicesMetricsUrl" })
    @Test
    public void getAnalyticsDevicesMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AnalyticsDevicesMetricsUrl) {
        log.debug(
                "\n\n getAnalyticsDevicesMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AnalyticsDevicesMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AnalyticsDevicesMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AnalyticsDevicesMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getAnalyticsDevicesMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AnalyticsErrorsMetricsUrl" })
    @Test
    public void getAnalyticsErrorsMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AnalyticsErrorsMetricsUrl) {
        log.debug("\n\n getAnalyticsErrorsMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AnalyticsErrorsMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AnalyticsErrorsMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AnalyticsErrorsMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getAnalyticsErrorsMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AnalyticsTrendsMetricsUrl" })
    @Test
    public void getAnalyticsTrendsMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AnalyticsTrendsMetricsUrl) {
        log.debug("\n\n getAnalyticsTrendsMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AnalyticsTrendsMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AnalyticsTrendsMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AnalyticsTrendsMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getAnalyticsTrendsMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2AnalyticsComparisonMetricsUrl" })
    @Test
    public void getfido2AnalyticsComparisonMetrics(final String issuer, final String fido2MetricsUrl,
            final String fido2AnalyticsComparisonMetricsUrl) {
        log.debug(
                "\n\n getfido2AnalyticsComparisonMetrics() - issuer:{}, fido2MetricsUrl:{}, fido2AnalyticsComparisonMetricsUrl:{}",
                issuer, fido2MetricsUrl, fido2AnalyticsComparisonMetricsUrl);

        Builder request = getResteasyService()
                .getClientBuilder(issuer + fido2MetricsUrl + fido2AnalyticsComparisonMetricsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getfido2AnalyticsComparisonMetrics() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2MetricsConfigUrl" })
    @Test
    public void getFido2MetricsConfig(final String issuer, final String fido2MetricsUrl,
            final String fido2MetricsConfigUrl) {
        log.debug("getFido2MetricsConfig() - issuer:{}, fido2MetricsUrl:{}, fido2MetricsConfigUrl:{}", issuer,
                fido2MetricsUrl, fido2MetricsConfigUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2MetricsConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2MetricsConfig() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "fido2MetricsUrl", "fido2MetricsHealthUrl" })
    @Test
    public void getFido2MetricsHealth(final String issuer, final String fido2MetricsUrl,
            final String fido2MetricsHealthUrl) {
        log.debug("getFido2MetricsHealth() - issuer:{}, fido2MetricsUrl:{}, fido2MetricsHealthUrl:{}", issuer,
                fido2MetricsUrl, fido2MetricsHealthUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2MetricsUrl + fido2MetricsHealthUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getFido2MetricsHealth() - response:{}, response.getStatus():{}", response, response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

}
