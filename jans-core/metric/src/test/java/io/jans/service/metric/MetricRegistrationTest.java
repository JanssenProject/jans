/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.jans.model.metric.MetricData;
import io.jans.model.metric.MetricTypeDeclaration;
import io.jans.model.metric.counter.CounterMetricData;
import io.jans.model.metric.counter.CounterMetricEntry;
import io.jans.model.metric.ldap.MetricEntry;

/**
 * @author Yuriy Movchan Date: 08/04/2026
 */
public class MetricRegistrationTest {

    enum TestMetricType implements MetricTypeDeclaration {
        TEST_COUNT;

        @Override
        public String getValue() {
            return "test_count";
        }

        @Override
        public String getDisplayName() {
            return "Test counter";
        }

        @Override
        public Class<? extends MetricData> getEventDataType() {
            return CounterMetricData.class;
        }

        @Override
        public Class<? extends MetricEntry> getMetricEntryType() {
            return CounterMetricEntry.class;
        }
    }

    @Test
    public void registryNameWithoutSubTypeShouldBeMetricValue() {
        MetricRegistration registration = new MetricRegistration(TestMetricType.TEST_COUNT, null);

        assertEquals("test_count", registration.getRegistryName());
        assertNull(registration.getMetricSubType());
    }

    @Test
    public void registryNameWithSubTypeShouldCombineBoth() {
        MetricRegistration registration = new MetricRegistration(TestMetricType.TEST_COUNT, "client1");

        assertEquals("test_count.client1", registration.getRegistryName());
        assertEquals("client1", registration.getMetricSubType());
    }

    @Test
    public void registrationsWithNullSubTypeShouldBeEqualAndPrintable() {
        MetricRegistration first = new MetricRegistration(TestMetricType.TEST_COUNT, null);
        MetricRegistration second = new MetricRegistration(TestMetricType.TEST_COUNT, null);
        MetricRegistration subTyped = new MetricRegistration(TestMetricType.TEST_COUNT, "client1");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, subTyped);
        // toString must not fail when the optional sub type is absent
        assertNotNull(first.toString());
    }

    @Test
    public void registrationsWithSameRegistryNameShouldBeEqual() {
        MetricRegistration first = new MetricRegistration(TestMetricType.TEST_COUNT, "client1");
        MetricRegistration second = new MetricRegistration(TestMetricType.TEST_COUNT, "client1");
        MetricRegistration other = new MetricRegistration(TestMetricType.TEST_COUNT, "client2");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

}
