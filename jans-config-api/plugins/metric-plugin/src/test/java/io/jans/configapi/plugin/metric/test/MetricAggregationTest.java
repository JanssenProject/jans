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

public class MetricAggregationTest extends MetricBaseTest {

    // The aggregation producer runs on application nodes and is implemented as a separate task, so
    // this endpoint is expected to return an empty page (200 OK) rather than data until it exists.
    @Parameters({ "test.issuer", "metricAggregationsUrl" })
    @Test
    public void getMetricAggregations(final String issuer, final String metricAggregationsUrl) {
        log.debug("getMetricAggregations() - issuer:{}, metricAggregationsUrl:{}", issuer, metricAggregationsUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricAggregationsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricAggregations() - response:{}, response.getStatus():{}", response,
                    response.getStatus());
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Parameters({ "test.issuer", "metricAggregationsInvalidTypeUrl" })
    @Test
    public void getMetricAggregationsWithInvalidAggregationType(final String issuer,
            final String metricAggregationsInvalidTypeUrl) {
        log.debug("getMetricAggregationsWithInvalidAggregationType() - issuer:{}, metricAggregationsInvalidTypeUrl:{}",
                issuer, metricAggregationsInvalidTypeUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricAggregationsInvalidTypeUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("getMetricAggregationsWithInvalidAggregationType() - response:{}, response.getStatus():{}",
                    response, response.getStatus());
            assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
        }
    }

}
