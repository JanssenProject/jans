/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.ldap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import io.jans.model.metric.MetricType;

/**
 * @author Yuriy Movchan Date: 08/04/2026
 */
public class MetricEntryTest {

    @Test
    public void setMetricTypeFromDeclarationShouldStoreValue() {
        MetricEntry entry = new MetricEntry();
        entry.setMetricType(MetricType.USER_AUTHENTICATION_SUCCESS);

        assertEquals(entry.getMetricType(), "user_authentication_success");
    }

    @Test
    public void setMetricTypeFromNullDeclarationShouldStoreNull() {
        MetricEntry entry = new MetricEntry();
        entry.setMetricType((io.jans.model.metric.MetricTypeDeclaration) null);

        assertNull(entry.getMetricType());
    }

    @Test
    public void metricSubTypeShouldBeStored() {
        MetricEntry entry = new MetricEntry();
        entry.setMetricSubType("client1");

        assertEquals(entry.getMetricSubType(), "client1");
    }

    @Test
    public void deprecatedNodeIdentifierBridgesShouldDelegate() {
        MetricEntry entry = new MetricEntry();
        entry.setNodeIndetifier("node1");

        assertEquals(entry.getNodeIdentifier(), "node1");
        assertEquals(entry.getNodeIndetifier(), "node1");
    }

}
