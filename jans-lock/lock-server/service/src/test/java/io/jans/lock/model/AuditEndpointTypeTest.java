/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.model;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for AuditEndpointType enum
 * 
 * @author Janssen Project
 */
public class AuditEndpointTypeTest {

    @Test
    public void testEnumValues_shouldContainAllExpectedTypes() {
        AuditEndpointType[] types = AuditEndpointType.values();
        
        assertEquals(types.length, 6);
    }

    @Test
    public void testTelemetry_shouldHaveCorrectProperties() {
        AuditEndpointType type = AuditEndpointType.TELEMETRY;
        
        assertEquals(type.getType(), "telemetry");
        assertEquals(type.getPath(), "telemetry");
        assertEquals(type.getConfigPath(), "jans-config-api/lock/audit/telemetry");
        assertEquals(type.getScopes().length, 1);
        assertEquals(type.getScopes()[0], "https://jans.io/oauth/lock/telemetry.write");
    }

    @Test
    public void testTelemetryBulk_shouldHaveCorrectProperties() {
        AuditEndpointType type = AuditEndpointType.TELEMETRY_BULK;
        
        assertEquals(type.getType(), "telemetry_bulk");
        assertEquals(type.getPath(), "telemetry/bulk");
        assertEquals(type.getConfigPath(), "jans-config-api/lock/audit/telemetry/bulk");
        assertEquals(type.getScopes().length, 1);
        assertEquals(type.getScopes()[0], "https://jans.io/oauth/lock/telemetry.write");
    }

    @Test
    public void testLog_shouldHaveCorrectProperties() {
        AuditEndpointType type = AuditEndpointType.LOG;
        
        assertEquals(type.getType(), "log");
        assertEquals(type.getPath(), "log");
        assertEquals(type.getConfigPath(), "jans-config-api/lock/audit/log");
        assertEquals(type.getScopes().length, 1);
        assertEquals(type.getScopes()[0], "https://jans.io/oauth/lock/log.write");
    }

    @Test
    public void testLogBulk_shouldHaveCorrectProperties() {
        AuditEndpointType type = AuditEndpointType.LOG_BULK;
        
        assertEquals(type.getType(), "log_bulk");
        assertEquals(type.getPath(), "log/bulk");
        assertEquals(type.getConfigPath(), "jans-config-api/lock/audit/log/bulk");
        assertEquals(type.getScopes().length, 1);
        assertEquals(type.getScopes()[0], "https://jans.io/oauth/lock/log.write");
    }

    @Test
    public void testHealth_shouldHaveCorrectProperties() {
        AuditEndpointType type = AuditEndpointType.HEALTH;
        
        assertEquals(type.getType(), "health");
        assertEquals(type.getPath(), "health");
        assertEquals(type.getConfigPath(), "jans-config-api/lock/audit/health");
        assertEquals(type.getScopes().length, 1);
        assertEquals(type.getScopes()[0], "https://jans.io/oauth/lock/health.write");
    }

    @Test
    public void testHealthBulk_shouldHaveCorrectProperties() {
        AuditEndpointType type = AuditEndpointType.HEALTH_BULK;
        
        assertEquals(type.getType(), "health_bulk");
        assertEquals(type.getPath(), "health/bulk");
        assertEquals(type.getConfigPath(), "jans-config-api/lock/audit/health/bulk");
        assertEquals(type.getScopes().length, 1);
        assertEquals(type.getScopes()[0], "https://jans.io/oauth/lock/health.write");
    }

    @Test
    public void testGetScopes_shouldReturnNonNullArray() {
        for (AuditEndpointType type : AuditEndpointType.values()) {
            assertNotNull(type.getScopes());
            assertTrue(type.getScopes().length > 0);
        }
    }

    @Test
    public void testGetPath_shouldMatchExpectedPattern() {
        assertEquals(AuditEndpointType.TELEMETRY.getPath(), "telemetry");
        assertEquals(AuditEndpointType.TELEMETRY_BULK.getPath(), "telemetry/bulk");
        assertEquals(AuditEndpointType.LOG.getPath(), "log");
        assertEquals(AuditEndpointType.LOG_BULK.getPath(), "log/bulk");
        assertEquals(AuditEndpointType.HEALTH.getPath(), "health");
        assertEquals(AuditEndpointType.HEALTH_BULK.getPath(), "health/bulk");
    }

    @Test
    public void testGetConfigPath_shouldStartWithConfigApiPrefix() {
        for (AuditEndpointType type : AuditEndpointType.values()) {
            assertTrue(type.getConfigPath().startsWith("jans-config-api/lock/audit/"));
        }
    }

    @Test
    public void testValueOf_withValidName_shouldReturnCorrectEnum() {
        AuditEndpointType telemetry = AuditEndpointType.valueOf("TELEMETRY");
        AuditEndpointType log = AuditEndpointType.valueOf("LOG");
        AuditEndpointType health = AuditEndpointType.valueOf("HEALTH");
        
        assertEquals(telemetry, AuditEndpointType.TELEMETRY);
        assertEquals(log, AuditEndpointType.LOG);
        assertEquals(health, AuditEndpointType.HEALTH);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueOf_withInvalidName_shouldThrowException() {
        AuditEndpointType.valueOf("INVALID_TYPE");
    }

    @Test
    public void testBulkVariants_shouldHaveBulkInPath() {
        assertTrue(AuditEndpointType.TELEMETRY_BULK.getPath().contains("bulk"));
        assertTrue(AuditEndpointType.LOG_BULK.getPath().contains("bulk"));
        assertTrue(AuditEndpointType.HEALTH_BULK.getPath().contains("bulk"));
    }

    @Test
    public void testNonBulkVariants_shouldNotHaveBulkInPath() {
        assertFalse(AuditEndpointType.TELEMETRY.getPath().contains("bulk"));
        assertFalse(AuditEndpointType.LOG.getPath().contains("bulk"));
        assertFalse(AuditEndpointType.HEALTH.getPath().contains("bulk"));
    }

    @Test
    public void testScopes_shouldBeValidUrls() {
        for (AuditEndpointType type : AuditEndpointType.values()) {
            for (String scope : type.getScopes()) {
                assertTrue(scope.startsWith("https://jans.io/oauth/lock/"));
                assertTrue(scope.endsWith(".write") || scope.endsWith(".readonly"));
            }
        }
    }
}
