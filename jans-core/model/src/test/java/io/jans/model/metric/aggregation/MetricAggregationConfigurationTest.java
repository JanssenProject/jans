/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.aggregation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MetricAggregationConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static void assertDefaults(MetricAggregationConfiguration conf) {
        assertTrue(conf.isEnabled());
        assertEquals(conf.getRunIntervalSeconds(), 300);
        assertEquals(conf.getMaxBucketsPerRun(), 48);
        assertEquals(conf.getRecomputeHourly(), 2);
        assertEquals(conf.getRecomputeDaily(), 1);
        assertEquals(conf.getRecomputeWeekly(), 1);
        assertEquals(conf.getRecomputeMonthly(), 1);
        assertEquals(conf.getKeepHourlyDays(), 90);
        assertEquals(conf.getKeepDailyDays(), 730);
        assertEquals(conf.getKeepWeeklyDays(), 1825);
        assertEquals(conf.getKeepMonthlyDays(), 3650);
        assertEquals(conf.getLeaseSeconds(), 600);
    }

    private static void setAllNumbers(MetricAggregationConfiguration conf, int value) {
        conf.setRunIntervalSeconds(value);
        conf.setMaxBucketsPerRun(value);
        conf.setRecomputeHourly(value);
        conf.setRecomputeDaily(value);
        conf.setRecomputeWeekly(value);
        conf.setRecomputeMonthly(value);
        conf.setKeepHourlyDays(value);
        conf.setKeepDailyDays(value);
        conf.setKeepWeeklyDays(value);
        conf.setKeepMonthlyDays(value);
        conf.setLeaseSeconds(value);
    }

    @Test
    public void newInstanceReturnsDefaults() {
        assertDefaults(new MetricAggregationConfiguration());
    }

    @Test
    public void zeroValuesFallBackToDefaults() {
        MetricAggregationConfiguration conf = new MetricAggregationConfiguration();
        setAllNumbers(conf, 0);
        assertDefaults(conf);
    }

    @Test
    public void negativeValuesFallBackToDefaults() {
        MetricAggregationConfiguration conf = new MetricAggregationConfiguration();
        setAllNumbers(conf, -5);
        assertDefaults(conf);
    }

    @Test
    public void positiveValuesAreReturned() {
        MetricAggregationConfiguration conf = new MetricAggregationConfiguration();
        setAllNumbers(conf, 7);
        conf.setEnabled(false);

        assertFalse(conf.isEnabled());
        assertEquals(conf.getRunIntervalSeconds(), 7);
        assertEquals(conf.getMaxBucketsPerRun(), 7);
        assertEquals(conf.getRecomputeHourly(), 7);
        assertEquals(conf.getRecomputeDaily(), 7);
        assertEquals(conf.getRecomputeWeekly(), 7);
        assertEquals(conf.getRecomputeMonthly(), 7);
        assertEquals(conf.getKeepHourlyDays(), 7);
        assertEquals(conf.getKeepDailyDays(), 7);
        assertEquals(conf.getKeepWeeklyDays(), 7);
        assertEquals(conf.getKeepMonthlyDays(), 7);
        assertEquals(conf.getLeaseSeconds(), 7);
    }

    @Test
    public void emptyJsonGivesDefaultsAndEnabled() throws Exception {
        MetricAggregationConfiguration conf = objectMapper.readValue("{}", MetricAggregationConfiguration.class);
        assertDefaults(conf);
    }

    @Test
    public void nullEnabledInJsonMeansEnabled() throws Exception {
        MetricAggregationConfiguration conf = objectMapper.readValue("{\"enabled\":null}", MetricAggregationConfiguration.class);
        assertTrue(conf.isEnabled());
    }

    @Test
    public void enabledFalseInJsonIsKept() throws Exception {
        MetricAggregationConfiguration conf = objectMapper.readValue("{\"enabled\":false}", MetricAggregationConfiguration.class);
        assertFalse(conf.isEnabled());
        assertEquals(conf.getRunIntervalSeconds(), 300);
    }

    @Test
    public void zeroAndNegativeValuesInJsonFallBackToDefaults() throws Exception {
        String json = "{\"runIntervalSeconds\":0,\"maxBucketsPerRun\":-1,\"recomputeHourly\":0,\"recomputeDaily\":-2,"
                + "\"recomputeWeekly\":0,\"recomputeMonthly\":0,\"keepHourlyDays\":-90,\"keepDailyDays\":0,"
                + "\"keepWeeklyDays\":0,\"keepMonthlyDays\":0,\"leaseSeconds\":-600}";
        MetricAggregationConfiguration conf = objectMapper.readValue(json, MetricAggregationConfiguration.class);
        assertDefaults(conf);
    }

    @Test
    public void unknownPropertiesAreIgnored() throws Exception {
        MetricAggregationConfiguration conf = objectMapper.readValue("{\"futureProperty\":1,\"leaseSeconds\":120}",
                MetricAggregationConfiguration.class);
        assertEquals(conf.getLeaseSeconds(), 120);
    }

    @Test
    public void jsonRoundTripKeepsValues() throws Exception {
        MetricAggregationConfiguration conf = new MetricAggregationConfiguration();
        conf.setEnabled(false);
        conf.setRunIntervalSeconds(60);
        conf.setKeepMonthlyDays(400);

        String json = objectMapper.writeValueAsString(conf);
        JsonNode node = objectMapper.readTree(json);
        assertFalse(node.get("enabled").asBoolean());
        assertEquals(node.get("runIntervalSeconds").asInt(), 60);
        assertEquals(node.get("leaseSeconds").asInt(), 600);

        MetricAggregationConfiguration read = objectMapper.readValue(json, MetricAggregationConfiguration.class);
        assertFalse(read.isEnabled());
        assertEquals(read.getRunIntervalSeconds(), 60);
        assertEquals(read.getKeepMonthlyDays(), 400);
        assertEquals(read.getKeepHourlyDays(), 90);
    }

}
