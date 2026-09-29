/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.config;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.config.AppConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TraceConfiguration deserialization and default fallback.
 *
 * <p>Written against JUnit 5 rather than the repo's TestNG convention: this module's Surefire
 * setup (see {@code lock-server/pom.xml} surefire pluginManagement) pins the JUnit-Platform
 * provider and does not execute TestNG classes, so a TestNG version of this test would never run.
 *
 * @author Janssen Project
 */
public class TraceConfigurationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testDeserialize_fullBlock_allValuesSet() throws Exception {
        String json = "{"
                + "\"traceConfiguration\": {"
                + "  \"enabled\": false,"
                + "  \"defaultEvidenceDomainId\": \"default\","
                + "  \"clientDomainBindings\": [ { \"clientId\": \"2200.abc\", \"evidenceDomainId\": \"default\", \"allowedProducerIds\": [\"*\"] } ],"
                + "  \"latenessThresholdSeconds\": 120,"
                + "  \"maxRequestBytes\": 1024,"
                + "  \"maxJsonDepth\": 4,"
                + "  \"maxStringLength\": 16,"
                + "  \"maxArrayLength\": 8,"
                + "  \"maxObjectMembers\": 8,"
                + "  \"receiptAllocationRetryLimit\": 3,"
                + "  \"receiptRepairIntervalSeconds\": 60,"
                + "  \"pendingReceiptTimeoutSeconds\": 30"
                + "}"
                + "}";

        AppConfiguration appConfiguration = mapper.readValue(json, AppConfiguration.class);
        TraceConfiguration traceConfiguration = appConfiguration.getTraceConfiguration();

        assertNotNull(traceConfiguration);
        assertFalse(traceConfiguration.isEnabled());
        assertEquals("default", traceConfiguration.getDefaultEvidenceDomainId());
        assertEquals(1, traceConfiguration.getClientDomainBindings().size());

        TraceClientDomainBinding binding = traceConfiguration.getClientDomainBindings().get(0);
        assertEquals("2200.abc", binding.getClientId());
        assertEquals("default", binding.getEvidenceDomainId());
        assertEquals(Collections.singletonList("*"), binding.getAllowedProducerIds());

        assertEquals(120, traceConfiguration.getLatenessThresholdSeconds());
        assertEquals(1024, traceConfiguration.getMaxRequestBytes());
        assertEquals(4, traceConfiguration.getMaxJsonDepth());
        assertEquals(16, traceConfiguration.getMaxStringLength());
        assertEquals(8, traceConfiguration.getMaxArrayLength());
        assertEquals(8, traceConfiguration.getMaxObjectMembers());
        assertEquals(3, traceConfiguration.getReceiptAllocationRetryLimit());
        assertEquals(60, traceConfiguration.getReceiptRepairIntervalSeconds());
        assertEquals(30, traceConfiguration.getPendingReceiptTimeoutSeconds());
    }

    @Test
    public void testDeserialize_missingBlock_defaultsApplied() throws Exception {
        AppConfiguration appConfiguration = mapper.readValue("{}", AppConfiguration.class);

        TraceConfiguration traceConfiguration = appConfiguration.getTraceConfiguration();

        assertNotNull(traceConfiguration);
        assertTrue(traceConfiguration.isEnabled());
        assertNull(traceConfiguration.getDefaultEvidenceDomainId());
        assertTrue(traceConfiguration.getClientDomainBindings().isEmpty());
        assertEquals(TraceConfiguration.DEFAULT_LATENESS_THRESHOLD_SECONDS, traceConfiguration.getLatenessThresholdSeconds());
        assertEquals(TraceConfiguration.DEFAULT_MAX_REQUEST_BYTES, traceConfiguration.getMaxRequestBytes());
        assertEquals(TraceConfiguration.DEFAULT_MAX_JSON_DEPTH, traceConfiguration.getMaxJsonDepth());
        assertEquals(TraceConfiguration.DEFAULT_MAX_STRING_LENGTH, traceConfiguration.getMaxStringLength());
        assertEquals(TraceConfiguration.DEFAULT_MAX_ARRAY_LENGTH, traceConfiguration.getMaxArrayLength());
        assertEquals(TraceConfiguration.DEFAULT_MAX_OBJECT_MEMBERS, traceConfiguration.getMaxObjectMembers());
        assertEquals(TraceConfiguration.DEFAULT_RECEIPT_ALLOCATION_RETRY_LIMIT, traceConfiguration.getReceiptAllocationRetryLimit());
        assertEquals(TraceConfiguration.DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS, traceConfiguration.getReceiptRepairIntervalSeconds());
        assertEquals(TraceConfiguration.DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS, traceConfiguration.getPendingReceiptTimeoutSeconds());
    }

    @Test
    public void testGetters_nonPositiveLimits_fallBackToDefaults() {
        TraceConfiguration traceConfiguration = new TraceConfiguration();

        traceConfiguration.setLatenessThresholdSeconds(0);
        traceConfiguration.setMaxRequestBytes(-1);
        traceConfiguration.setMaxJsonDepth(0);
        traceConfiguration.setMaxStringLength(-100);
        traceConfiguration.setMaxArrayLength(0);
        traceConfiguration.setMaxObjectMembers(-1);
        traceConfiguration.setReceiptAllocationRetryLimit(0);
        traceConfiguration.setReceiptRepairIntervalSeconds(-1);
        traceConfiguration.setPendingReceiptTimeoutSeconds(0);

        assertEquals(TraceConfiguration.DEFAULT_LATENESS_THRESHOLD_SECONDS, traceConfiguration.getLatenessThresholdSeconds());
        assertEquals(TraceConfiguration.DEFAULT_MAX_REQUEST_BYTES, traceConfiguration.getMaxRequestBytes());
        assertEquals(TraceConfiguration.DEFAULT_MAX_JSON_DEPTH, traceConfiguration.getMaxJsonDepth());
        assertEquals(TraceConfiguration.DEFAULT_MAX_STRING_LENGTH, traceConfiguration.getMaxStringLength());
        assertEquals(TraceConfiguration.DEFAULT_MAX_ARRAY_LENGTH, traceConfiguration.getMaxArrayLength());
        assertEquals(TraceConfiguration.DEFAULT_MAX_OBJECT_MEMBERS, traceConfiguration.getMaxObjectMembers());
        assertEquals(TraceConfiguration.DEFAULT_RECEIPT_ALLOCATION_RETRY_LIMIT, traceConfiguration.getReceiptAllocationRetryLimit());
        assertEquals(TraceConfiguration.DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS, traceConfiguration.getReceiptRepairIntervalSeconds());
        assertEquals(TraceConfiguration.DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS, traceConfiguration.getPendingReceiptTimeoutSeconds());
    }

}
