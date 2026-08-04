/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for FIDO2 Fido2MetricsConstants utility class
 *
 * @author Janssen Project
 * @version 1.0
 */
class Fido2MetricsConstantsTest {

    @Test
    void testPrivateConstructorThrowsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<Fido2MetricsConstants> ctor = Fido2MetricsConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor must be private");
        ctor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, ctor::newInstance);
        Throwable cause = ex.getCause();
        assertNotNull(cause, "InvocationTargetException must have a cause");
        assertTrue(cause instanceof UnsupportedOperationException,
                "Cause must be UnsupportedOperationException, was " + cause.getClass().getName());
        assertEquals("Utility class", cause.getMessage());
    }

    @Test
    void testAggregationTypeConstants() {
        assertEquals("HOURLY", Fido2MetricsConstants.HOURLY);
        assertEquals("DAILY", Fido2MetricsConstants.DAILY);
        assertEquals("WEEKLY", Fido2MetricsConstants.WEEKLY);
        assertEquals("MONTHLY", Fido2MetricsConstants.MONTHLY);
    }

    @Test
    void testOperationTypeConstants() {
        assertEquals("REGISTRATION", Fido2MetricsConstants.REGISTRATION);
        assertEquals("AUTHENTICATION", Fido2MetricsConstants.AUTHENTICATION);
        assertEquals("FALLBACK", Fido2MetricsConstants.FALLBACK);
    }

    @Test
    void testOperationStatusConstants() {
        assertEquals("SUCCESS", Fido2MetricsConstants.SUCCESS);
        assertEquals("FAILURE", Fido2MetricsConstants.FAILURE);
        assertEquals("ATTEMPT", Fido2MetricsConstants.ATTEMPT);
    }

    @Test
    void testTrendTypeConstants() {
        assertEquals("STABLE", Fido2MetricsConstants.STABLE);
        assertEquals("INCREASING", Fido2MetricsConstants.INCREASING);
        assertEquals("DECREASING", Fido2MetricsConstants.DECREASING);
    }

    @Test
    void testQualityEngagementAdoptionLevels() {
        assertEquals("HIGH", Fido2MetricsConstants.HIGH);
        assertEquals("MEDIUM", Fido2MetricsConstants.MEDIUM);
        assertEquals("LOW", Fido2MetricsConstants.LOW);
        assertEquals("EARLY", Fido2MetricsConstants.EARLY);
        assertEquals("GROWTH", Fido2MetricsConstants.GROWTH);
        assertEquals("MATURE", Fido2MetricsConstants.MATURE);
    }

    @Test
    void testBaseDnConstants() {
        assertEquals("ou=fido2-metrics,o=jans", Fido2MetricsConstants.FIDO2_METRICS_ENTRY_BASE_DN);
        assertEquals("ou=fido2-aggregations,o=jans", Fido2MetricsConstants.FIDO2_METRICS_AGGREGATION_BASE_DN);
        assertEquals("ou=fido2-user-metrics,o=jans", Fido2MetricsConstants.FIDO2_USER_METRICS_BASE_DN);
    }

    @Test
    void testDbAttributeName() {
        assertEquals("jansFido2MetricsTimestamp", Fido2MetricsConstants.JANS_TIMESTAMP);
    }

    @Test
    void testQueryParameterNames() {
        assertEquals("startTime", Fido2MetricsConstants.PARAM_START_TIME);
        assertEquals("endTime", Fido2MetricsConstants.PARAM_END_TIME);
    }

    @Test
    void testFallbackAndErrorConstants() {
        assertEquals("PASSWORD", Fido2MetricsConstants.FALLBACK_METHOD_PASSWORD);
        assertEquals("timeout", Fido2MetricsConstants.ERROR_KEYWORD_TIMEOUT);
        assertEquals("expired", Fido2MetricsConstants.ERROR_KEYWORD_EXPIRED);
        assertEquals("An unexpected error occurred while processing the request",
                Fido2MetricsConstants.ERROR_UNEXPECTED);
    }

    @Test
    void testMetricsDataKeys() {
        assertEquals("registrationAttempts", Fido2MetricsConstants.REGISTRATION_ATTEMPTS);
        assertEquals("registrationSuccessRate", Fido2MetricsConstants.REGISTRATION_SUCCESS_RATE);
        assertEquals("authenticationAvgDuration", Fido2MetricsConstants.AUTHENTICATION_AVG_DURATION);
        assertEquals("totalOperations", Fido2MetricsConstants.TOTAL_OPERATIONS);
        assertEquals("deviceTypes", Fido2MetricsConstants.DEVICE_TYPES);
        assertEquals("errorCounts", Fido2MetricsConstants.ERROR_COUNTS);
        assertEquals("performanceMetrics", Fido2MetricsConstants.PERFORMANCE_METRICS);
    }

    /**
     * These values mirror the column widths declared in
     * jans-linux-setup/jans_setup/static/rdbm/sql_data_types.json. That file lives
     * outside this Maven module, so it cannot be read from here; pinning the numbers
     * is what forces a reviewer to move both sides together.
     *
     * <p>If you are reading this because the test failed: a constant was changed
     * without widening the matching column, which is how FIDO2 telemetry silently
     * collected nothing until the columns were fixed. Update sql_data_types.json in
     * the same commit.
     */
    @Test
    void testMaxLengthConstantsMatchDeclaredColumnWidths() {
        assertEquals(512, Fido2MetricsConstants.MAX_LENGTH_USER_AGENT);
        assertEquals(1024, Fido2MetricsConstants.MAX_LENGTH_ERROR_REASON);
        assertEquals(512, Fido2MetricsConstants.MAX_LENGTH_FALLBACK_REASON);
        assertEquals(256, Fido2MetricsConstants.MAX_LENGTH_USERNAME);
        assertEquals(128, Fido2MetricsConstants.MAX_LENGTH_USER_ID);
        assertEquals(128, Fido2MetricsConstants.MAX_LENGTH_SESSION_ID);
        assertEquals(128, Fido2MetricsConstants.MAX_LENGTH_DEVICE_INFO_FIELD);
    }

    /**
     * Fields left at the schema default. These are bounded by construction -- enums,
     * a UUID, a MAC address, an IPv6 literal -- so widening their columns would be
     * noise. If one of these ever starts carrying free-form input it needs a column
     * override first.
     */
    @Test
    void testBoundedFieldsStayAtTheSchemaDefault() {
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_OPERATION_TYPE);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_STATUS);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_METRIC_TYPE);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_AUTHENTICATOR_TYPE);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_ERROR_CATEGORY);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_FALLBACK_METHOD);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_IP_ADDRESS);
        assertEquals(64, Fido2MetricsConstants.MAX_LENGTH_NODE_ID);
    }
}
