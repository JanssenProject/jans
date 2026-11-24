/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetricAggregationEntryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void validJsonIsParsedToObject() {
        MetricAggregationEntry entry = new MetricAggregationEntry();
        entry.setData("{\"kind\":\"counter\",\"count\":13,\"complete\":true}");

        JsonNode node = entry.getDataAsJson();

        assertTrue(node.isObject());
        assertEquals("counter", node.get("kind").asText());
        assertEquals(13, node.get("count").asInt());
    }

    @Test
    public void invalidJsonFallsBackToTextNode() {
        MetricAggregationEntry entry = new MetricAggregationEntry();
        entry.setData("not json {");

        JsonNode node = entry.getDataAsJson();

        assertTrue(node.isTextual());
        assertEquals("not json {", node.asText());
    }

    @Test
    public void nullDataGivesNull() {
        assertNull(new MetricAggregationEntry().getDataAsJson());
    }

    @Test
    public void serializationWritesDataAsObjectOnly() throws Exception {
        MetricAggregationEntry entry = new MetricAggregationEntry();
        entry.setId("id1");
        entry.setData("{\"kind\":\"counter\",\"count\":13}");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(entry));

        assertTrue(json.get("data").isObject());
        assertEquals(13, json.get("data").get("count").asInt());
        assertEquals(1, json.findValues("data").size());
        assertFalse(json.has("dataAsJson"));
    }

    @Test
    public void rawGetterStillReturnsString() {
        MetricAggregationEntry entry = new MetricAggregationEntry();
        entry.setData("{\"a\":1}");

        assertEquals("{\"a\":1}", entry.getData());
    }
}
