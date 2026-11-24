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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import org.testng.annotations.Parameters;

public class MetricAggregationTest extends MetricBaseTest {

    // The page may be empty on a server without aggregation rows; when it has entries, each
    // entry's 'data' must be a JSON object (or, for a damaged row, a string), never an escaped string
    // that holds a JSON object.
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
            assertEntriesDataIsJson(response.readEntity(String.class));
        }
    }

    @Parameters({ "test.issuer", "metricAggregationsDateOnlyEndUrl" })
    @Test
    public void getMetricAggregationsWithDateOnlyEnd(final String issuer, final String metricAggregationsDateOnlyEndUrl) {
        log.debug("getMetricAggregationsWithDateOnlyEnd() - issuer:{}, url:{}", issuer,
                metricAggregationsDateOnlyEndUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + metricAggregationsDateOnlyEndUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            assertEntriesDataIsJson(response.readEntity(String.class));
        }
    }

    private void assertEntriesDataIsJson(String body) {
        try {
            JsonNode entries = new ObjectMapper().readTree(body).get("entries");
            if (entries == null) {
                return;
            }
            for (JsonNode entry : entries) {
                JsonNode data = entry.get("data");
                if (data != null && data.isTextual()) {
                    String text = data.asText().trim();
                    assertFalse(text.startsWith("{"), "data is an escaped JSON string: " + text);
                }
            }
        } catch (JsonProcessingException e) {
            throw new AssertionError("Response is not JSON: " + e.getMessage(), e);
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
