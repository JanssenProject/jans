/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.model.config;

import io.jans.lock.model.config.cedarling.CedarlingConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for AppConfiguration focusing on new methods
 * 
 * @author Janssen Project
 */
public class AppConfigurationTest {

    private AppConfiguration appConfiguration;

    @BeforeMethod
    public void setUp() {
        appConfiguration = new AppConfiguration();
    }

    @Test
    public void testGetProtectionMode_defaultValue_shouldBeOAuth() {
        assertEquals(appConfiguration.getProtectionMode(), LockProtectionMode.OAUTH);
    }

    @Test
    public void testSetProtectionMode_shouldStoreValue() {
        appConfiguration.setProtectionMode(LockProtectionMode.CEDARLING);
        
        assertEquals(appConfiguration.getProtectionMode(), LockProtectionMode.CEDARLING);
    }

    @Test
    public void testSetProtectionMode_withNull_shouldAcceptNull() {
        appConfiguration.setProtectionMode(null);
        
        assertNull(appConfiguration.getProtectionMode());
    }

    @Test
    public void testGetAuditPersistenceMode_defaultValue_shouldBeInternal() {
        assertEquals(appConfiguration.getAuditPersistenceMode(), AuditPersistenceMode.INTERNAL);
    }

    @Test
    public void testSetAuditPersistenceMode_shouldStoreValue() {
        appConfiguration.setAuditPersistenceMode(AuditPersistenceMode.CONFIG_API);
        
        assertEquals(appConfiguration.getAuditPersistenceMode(), AuditPersistenceMode.CONFIG_API);
    }

    @Test
    public void testSetAuditPersistenceMode_withNull_shouldAcceptNull() {
        appConfiguration.setAuditPersistenceMode(null);
        
        assertNull(appConfiguration.getAuditPersistenceMode());
    }

    @Test
    public void testGetCedarlingConfiguration_initiallyNull() {
        assertNull(appConfiguration.getCedarlingConfiguration());
    }

    @Test
    public void testSetCedarlingConfiguration_shouldStoreValue() {
        CedarlingConfiguration config = new CedarlingConfiguration();
        appConfiguration.setCedarlingConfiguration(config);
        
        assertSame(appConfiguration.getCedarlingConfiguration(), config);
    }

    @Test
    public void testGetDisableJdkLogger_defaultValue_shouldBeTrue() {
        assertTrue(appConfiguration.getDisableJdkLogger());
    }

    @Test
    public void testSetClientPassword_shouldStoreValue() {
        String password = "test-password";
        appConfiguration.setClientPassword(password);
        
        assertEquals(appConfiguration.getClientPassword(), password);
    }

    @Test
    public void testSetClientPassword_withNull_shouldAcceptNull() {
        appConfiguration.setClientPassword("initial");
        appConfiguration.setClientPassword(null);
        
        assertNull(appConfiguration.getClientPassword());
    }

    @Test
    public void testSetCleanServiceBatchChunkSize_shouldStoreValue() {
        int chunkSize = 5000;
        appConfiguration.setCleanServiceBatchChunkSize(chunkSize);
        
        assertEquals(appConfiguration.getCleanServiceBatchChunkSize(), chunkSize);
    }

    @Test
    public void testSetCleanServiceBatchChunkSize_withZero_shouldAcceptZero() {
        appConfiguration.setCleanServiceBatchChunkSize(0);
        
        assertEquals(appConfiguration.getCleanServiceBatchChunkSize(), 0);
    }

    @Test
    public void testSetCleanServiceBatchChunkSize_withNegative_shouldAcceptNegative() {
        appConfiguration.setCleanServiceBatchChunkSize(-1);
        
        assertEquals(appConfiguration.getCleanServiceBatchChunkSize(), -1);
    }

    @Test
    public void testToString_shouldIncludeProtectionMode() {
        appConfiguration.setProtectionMode(LockProtectionMode.CEDARLING);
        
        String result = appConfiguration.toString();
        
        assertTrue(result.contains("protectionMode="));
        assertTrue(result.contains("CEDARLING"));
    }

    @Test
    public void testToString_shouldIncludeCedarlingConfiguration() {
        CedarlingConfiguration config = new CedarlingConfiguration();
        appConfiguration.setCedarlingConfiguration(config);
        
        String result = appConfiguration.toString();
        
        assertTrue(result.contains("cedarlingConfiguration="));
    }

    @Test
    public void testToString_shouldNotIncludeRemovedFields() {
        String result = appConfiguration.toString();
        
        assertFalse(result.contains("tokenUrl"));
        assertFalse(result.contains("endpointGroups"));
        assertFalse(result.contains("endpointDetails"));
    }

    @Test
    public void testMultiplePropertyChanges_shouldBeIndependent() {
        appConfiguration.setProtectionMode(LockProtectionMode.CEDARLING);
        appConfiguration.setAuditPersistenceMode(AuditPersistenceMode.CONFIG_API);
        appConfiguration.setClientPassword("test-pass");
        appConfiguration.setCleanServiceBatchChunkSize(1000);
        
        assertEquals(appConfiguration.getProtectionMode(), LockProtectionMode.CEDARLING);
        assertEquals(appConfiguration.getAuditPersistenceMode(), AuditPersistenceMode.CONFIG_API);
        assertEquals(appConfiguration.getClientPassword(), "test-pass");
        assertEquals(appConfiguration.getCleanServiceBatchChunkSize(), 1000);
    }

    @Test
    public void testSetProtectionMode_switchingBetweenModes() {
        appConfiguration.setProtectionMode(LockProtectionMode.OAUTH);
        assertEquals(appConfiguration.getProtectionMode(), LockProtectionMode.OAUTH);
        
        appConfiguration.setProtectionMode(LockProtectionMode.CEDARLING);
        assertEquals(appConfiguration.getProtectionMode(), LockProtectionMode.CEDARLING);
        
        appConfiguration.setProtectionMode(LockProtectionMode.OAUTH);
        assertEquals(appConfiguration.getProtectionMode(), LockProtectionMode.OAUTH);
    }

    @Test
    public void testSetAuditPersistenceMode_switchingBetweenModes() {
        appConfiguration.setAuditPersistenceMode(AuditPersistenceMode.INTERNAL);
        assertEquals(appConfiguration.getAuditPersistenceMode(), AuditPersistenceMode.INTERNAL);
        
        appConfiguration.setAuditPersistenceMode(AuditPersistenceMode.CONFIG_API);
        assertEquals(appConfiguration.getAuditPersistenceMode(), AuditPersistenceMode.CONFIG_API);
        
        appConfiguration.setAuditPersistenceMode(AuditPersistenceMode.INTERNAL);
        assertEquals(appConfiguration.getAuditPersistenceMode(), AuditPersistenceMode.INTERNAL);
    }
}