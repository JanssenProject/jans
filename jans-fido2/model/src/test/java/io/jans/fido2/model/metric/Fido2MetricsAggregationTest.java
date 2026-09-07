/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the small computed surface on {@link Fido2MetricsAggregation}:
 * 4-arg constructor id composition + UTC {@code lastUpdated} stamp,
 * {@code getPeriod()} derivation from {@code id}, null-safe {@code getLongMetric}/
 * {@code getDoubleMetric}, {@code Integer→Long} coercion on map metrics,
 * {@code incrementMetric} null-safety, and the {@code equals}/{@code hashCode}
 * contract over ({@code id}, {@code aggregationType}, {@code startTime}, {@code endTime}).
 *
 * @author Janssen Project
 * @version 1.0
 */
class Fido2MetricsAggregationTest {

    @Test
    void testDefaultConstructorInitializesEmptyMetricsData() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();

        assertNotNull(agg.getMetricsData(), "metricsData must be initialized, not null");
        assertTrue(agg.getMetricsData().isEmpty(), "metricsData must start empty");
        assertNull(agg.getId());
        assertNull(agg.getAggregationType());
        assertNull(agg.getStartTime());
        assertNull(agg.getEndTime());
        assertNull(agg.getLastUpdated());
    }

    @Test
    void testFourArgConstructorComposesIdAndStampsLastUpdated() {
        Date start = new Date(1_700_000_000_000L);
        Date end = new Date(1_700_086_400_000L);
        long beforeMillis = System.currentTimeMillis();

        Fido2MetricsAggregation agg = new Fido2MetricsAggregation("DAILY", "2026-05-22", start, end);

        long afterMillis = System.currentTimeMillis();

        assertEquals("DAILY_2026-05-22", agg.getId(),
                "id must be composed as aggregationType + \"_\" + period");
        assertEquals("DAILY", agg.getAggregationType());
        assertEquals(start, agg.getStartTime());
        assertEquals(end, agg.getEndTime());
        assertNotNull(agg.getLastUpdated(), "lastUpdated must be stamped by the 4-arg ctor");

        long stamped = agg.getLastUpdated().getTime();
        assertTrue(stamped >= beforeMillis - 1000 && stamped <= afterMillis + 1000,
                "lastUpdated must be within ±1s of now (stamped=" + stamped
                        + ", before=" + beforeMillis + ", after=" + afterMillis + ")");

        // Default-ctor invariants must still hold via this() delegation.
        assertNotNull(agg.getMetricsData());
        assertTrue(agg.getMetricsData().isEmpty());
    }

    @Test
    void testGetPeriodDerivesFromIdOnFirstUnderscore() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();

        agg.setId("DAILY_2026-05-22");
        assertEquals("2026-05-22", agg.getPeriod(),
                "period is the substring after the first underscore");

        // Multi-underscore period: split is on the FIRST underscore only,
        // so the remaining underscores belong to the period.
        agg.setId("HOURLY_2026-05-22_14");
        assertEquals("2026-05-22_14", agg.getPeriod());

        agg.setId("NOPERIOD");
        assertEquals("NOPERIOD", agg.getPeriod(),
                "id with no underscore returns the id unchanged");

        agg.setId(null);
        assertNull(agg.getPeriod(), "null id returns null period");
    }

    @Test
    void testGetLongMetricAndGetDoubleMetricNullSafety() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();

        // Absent key on an empty map.
        assertNull(agg.getLongMetric("missing"));
        assertNull(agg.getDoubleMetric("missing"));

        // metricsData itself null.
        agg.setMetricsData(null);
        assertNull(agg.getLongMetric("anything"));
        assertNull(agg.getDoubleMetric("anything"));
    }

    @Test
    void testGetLongMetricReturnsValueAndWidensIntegerToLong() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();
        Map<String, Object> data = new HashMap<>();
        data.put("long-key", 42L);
        data.put("int-key", 7);
        data.put("double-key", 3.5d);
        agg.setMetricsData(data);

        assertEquals(42L, agg.getLongMetric("long-key"));
        assertEquals(7L, agg.getLongMetric("int-key"),
                "Integer values must be widened to Long");
        // Double is also a Number — longValue() truncates toward zero.
        assertEquals(3L, agg.getLongMetric("double-key"));
    }

    @Test
    void testGetDoubleMetricReturnsValueForIntegerAndDouble() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();
        Map<String, Object> data = new HashMap<>();
        data.put("double-key", 0.85d);
        data.put("int-key", 7);
        agg.setMetricsData(data);

        assertEquals(0.85d, agg.getDoubleMetric("double-key"), 1e-9);
        assertEquals(7.0d, agg.getDoubleMetric("int-key"), 1e-9);
    }

    @Test
    void testGetLongMetricReturnsNullForNonNumberValue() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();
        Map<String, Object> data = new HashMap<>();
        data.put("string-key", "not-a-number");
        agg.setMetricsData(data);

        assertNull(agg.getLongMetric("string-key"),
                "non-Number value must return null, not throw ClassCastException");
        assertNull(agg.getDoubleMetric("string-key"));
    }

    @Test
    void testConvenienceSettersWireToConstantKeys() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();

        // Long path.
        agg.setRegistrationAttempts(42L);
        assertEquals(42L, agg.getRegistrationAttempts());
        assertEquals(42L,
                agg.getMetricsData().get(Fido2MetricsConstants.REGISTRATION_ATTEMPTS),
                "setRegistrationAttempts must store under the REGISTRATION_ATTEMPTS key");

        // Double path.
        agg.setAuthenticationSuccessRate(0.85d);
        assertEquals(0.85d, agg.getAuthenticationSuccessRate(), 1e-9);
        assertEquals(0.85d,
                (Double) agg.getMetricsData().get(Fido2MetricsConstants.AUTHENTICATION_SUCCESS_RATE),
                1e-9,
                "setAuthenticationSuccessRate must store under the AUTHENTICATION_SUCCESS_RATE key");
    }

    @Test
    void testGetDeviceTypesCoercesIntegerToLong() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();
        Map<String, Object> inner = new HashMap<>();
        inner.put("PLATFORM", 7);          // Integer — exactly what Jackson hands back for JSON numbers.
        inner.put("CROSS_PLATFORM", 3);
        Map<String, Object> data = new HashMap<>();
        data.put(Fido2MetricsConstants.DEVICE_TYPES, inner);
        agg.setMetricsData(data);

        Map<String, Long> deviceTypes = agg.getDeviceTypes();

        assertNotNull(deviceTypes);
        assertEquals(2, deviceTypes.size());
        assertEquals(Long.valueOf(7L), deviceTypes.get("PLATFORM"));
        assertEquals(Long.valueOf(3L), deviceTypes.get("CROSS_PLATFORM"));
    }

    @Test
    void testGetDeviceTypesReturnsEmptyMapWhenAbsent() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();

        Map<String, Long> deviceTypes = agg.getDeviceTypes();
        assertNotNull(deviceTypes, "must return empty map, not null, when key is missing");
        assertTrue(deviceTypes.isEmpty());

        Map<String, Long> errorCounts = agg.getErrorCounts();
        assertNotNull(errorCounts);
        assertTrue(errorCounts.isEmpty());
    }

    @Test
    void testIncrementMetricIsNullSafeOnBothSides() {
        Fido2MetricsAggregation agg = new Fido2MetricsAggregation();

        // Starting from absent key + non-null increment.
        agg.incrementMetric("foo", 5L);
        assertEquals(5L, agg.getLongMetric("foo"));

        // Subsequent increment accumulates.
        agg.incrementMetric("foo", 3L);
        assertEquals(8L, agg.getLongMetric("foo"));

        // Null increment leaves the current value untouched.
        agg.incrementMetric("foo", null);
        assertEquals(8L, agg.getLongMetric("foo"));

        // Both null on a fresh instance — current treated as 0, increment as 0.
        Fido2MetricsAggregation fresh = new Fido2MetricsAggregation();
        fresh.incrementMetric("bar", null);
        assertEquals(0L, fresh.getLongMetric("bar"));
    }

    @Test
    void testEqualsAndHashCodeContract() {
        Date start = new Date(1_700_000_000_000L);
        Date end = new Date(1_700_086_400_000L);

        Fido2MetricsAggregation a = new Fido2MetricsAggregation("DAILY", "2026-05-22", start, end);
        Fido2MetricsAggregation b = new Fido2MetricsAggregation("DAILY", "2026-05-22", start, end);

        // Identical (id, aggregationType, startTime, endTime) — equal and same hash.
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // Unrelated fields don't affect equality.
        a.setUniqueUsers(100L);
        b.setUniqueUsers(999L);
        a.setRegistrationAttempts(10L);
        b.setRegistrationAttempts(20L);
        assertEquals(a, b, "equality must ignore uniqueUsers and metricsData");
        assertEquals(a.hashCode(), b.hashCode());

        // Reflexive.
        assertEquals(a, a);

        // Null and other-type comparisons.
        assertNotEquals(null, a);
        assertNotEquals("string", a);
        assertFalse(a.equals(null));
        assertFalse(a.equals("string"));

        // Differ in id (via setId, since the ctor composes it).
        Fido2MetricsAggregation differentId = new Fido2MetricsAggregation("DAILY", "2026-05-22", start, end);
        differentId.setId("DAILY_2026-05-23");
        assertNotEquals(a, differentId);

        // Differ in aggregationType.
        Fido2MetricsAggregation differentType = new Fido2MetricsAggregation("HOURLY", "2026-05-22", start, end);
        assertNotEquals(a, differentType);

        // Differ in startTime.
        Fido2MetricsAggregation differentStart = new Fido2MetricsAggregation("DAILY", "2026-05-22",
                new Date(start.getTime() + 1), end);
        differentStart.setId(a.getId()); // hold id steady to isolate startTime
        assertNotEquals(a, differentStart);

        // Differ in endTime.
        Fido2MetricsAggregation differentEnd = new Fido2MetricsAggregation("DAILY", "2026-05-22",
                start, new Date(end.getTime() + 1));
        differentEnd.setId(a.getId());
        assertNotEquals(a, differentEnd);
    }
}
