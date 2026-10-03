/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for FIDO2 Fido2MetricType enum
 *
 * @author Janssen Project
 * @version 1.0
 */
class Fido2MetricTypeTest {

    /**
     * Authoritative mapping of enum constant to expected persisted metric name.
     * Driving the test from this map guarantees every enum value is asserted —
     * a new enum constant added without an entry here will fail the size check.
     */
    private static final Map<Fido2MetricType, String> EXPECTED_METRIC_NAMES;
    static {
        EnumMap<Fido2MetricType, String> map = new EnumMap<>(Fido2MetricType.class);
        map.put(Fido2MetricType.FIDO2_REGISTRATION_ATTEMPT, "fido2_registration_attempt");
        map.put(Fido2MetricType.FIDO2_REGISTRATION_SUCCESS, "fido2_registration_success");
        map.put(Fido2MetricType.FIDO2_REGISTRATION_FAILURE, "fido2_registration_failure");
        map.put(Fido2MetricType.FIDO2_REGISTRATION_DURATION, "fido2_registration_duration");
        map.put(Fido2MetricType.FIDO2_AUTHENTICATION_ATTEMPT, "fido2_authentication_attempt");
        map.put(Fido2MetricType.FIDO2_AUTHENTICATION_SUCCESS, "fido2_authentication_success");
        map.put(Fido2MetricType.FIDO2_AUTHENTICATION_FAILURE, "fido2_authentication_failure");
        map.put(Fido2MetricType.FIDO2_AUTHENTICATION_DURATION, "fido2_authentication_duration");
        map.put(Fido2MetricType.FIDO2_FALLBACK_EVENT, "fido2_fallback_event");
        map.put(Fido2MetricType.FIDO2_DEVICE_TYPE_USAGE, "fido2_device_type_usage");
        EXPECTED_METRIC_NAMES = map;
    }

    @Test
    void testGetMetricNameReturnsExpectedValueForEveryEnum() {
        assertEquals(Fido2MetricType.values().length, EXPECTED_METRIC_NAMES.size(),
                "Every enum constant must have an expected metric name entry");
        for (Fido2MetricType type : Fido2MetricType.values()) {
            assertTrue(EXPECTED_METRIC_NAMES.containsKey(type),
                    "Missing expected metric name for " + type.name());
            assertEquals(EXPECTED_METRIC_NAMES.get(type), type.getMetricName(),
                    "Unexpected metricName for " + type.name());
        }
    }

    @Test
    void testGetMetricNameIsUniqueAcrossAllEnumValues() {
        Set<String> uniqueNames = Arrays.stream(Fido2MetricType.values())
                .map(Fido2MetricType::getMetricName)
                .collect(Collectors.toSet());
        assertEquals(Fido2MetricType.values().length, uniqueNames.size(),
                "Metric names must be unique; duplicates would silently merge in DB aggregation");
    }

    @Test
    void testGetDescriptionIsNonNullAndNonEmptyForEveryEnum() {
        for (Fido2MetricType type : Fido2MetricType.values()) {
            String description = type.getDescription();
            assertNotNull(description, "Description must not be null for " + type.name());
            assertFalse(description.isEmpty(), "Description must not be empty for " + type.name());
        }
    }

    @Test
    void testToStringReturnsMetricName() {
        for (Fido2MetricType type : Fido2MetricType.values()) {
            assertEquals(type.getMetricName(), type.toString(),
                    "toString() must return metricName for " + type.name());
        }
    }

    @Test
    void testValuesLengthIsTen() {
        assertEquals(10, Fido2MetricType.values().length,
                "Fido2MetricType is expected to have exactly 10 values; consumers rely on this fixed set");
    }
}
