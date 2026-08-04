/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.jans.model.metric.MetricType;

/**
 * @author Yuriy Movchan Date: 08/04/2026
 */
public class MetricEntryTest {

    @Test
    public void setMetricTypeFromDeclarationShouldStoreValue() {
        MetricEntry entry = new MetricEntry();
        entry.setMetricType(MetricType.USER_AUTHENTICATION_SUCCESS.getValue());

        assertEquals("user_authentication_success", entry.getMetricType());
    }

    @Test
    public void setMetricTypeFromNullDeclarationShouldStoreNull() {
        MetricEntry entry = new MetricEntry();
        entry.setMetricType(null);

        assertNull(entry.getMetricType());
    }

    @Test
    public void metricSubTypeShouldBeStored() {
        MetricEntry entry = new MetricEntry();
        entry.setMetricSubType("client1");

        assertEquals("client1", entry.getMetricSubType());
    }

    @Test
    public void metricSubTypeIsOptionalAndDefaultsToNull() {
        MetricEntry entry = new MetricEntry();

        assertNull(entry.getMetricSubType());

        entry.setMetricSubType("client1");
        entry.setMetricSubType(null);

        assertNull(entry.getMetricSubType());
        // toString must not fail when optional fields are not set
        assertNotNull(entry.toString());
    }

    @Test
    public void nodeIdentifierShouldBeStored() {
        MetricEntry entry = new MetricEntry();
        entry.setNodeIdentifier("node1");

        assertEquals("node1", entry.getNodeIdentifier());
    }

}
