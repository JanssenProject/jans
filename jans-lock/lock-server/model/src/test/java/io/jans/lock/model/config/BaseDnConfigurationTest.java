/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.model.config;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for BaseDnConfiguration
 * 
 * @author Janssen Project
 */
public class BaseDnConfigurationTest {

    private BaseDnConfiguration baseDnConfiguration;

    @BeforeMethod
    public void setUp() {
        baseDnConfiguration = new BaseDnConfiguration();
    }

    @Test
    public void testGetConfiguration_initiallyNull() {
        assertNull(baseDnConfiguration.getConfiguration());
    }

    @Test
    public void testSetConfiguration_shouldStoreValue() {
        String configDn = "ou=configuration,o=jans";
        baseDnConfiguration.setConfiguration(configDn);
        
        assertEquals(baseDnConfiguration.getConfiguration(), configDn);
    }

    @Test
    public void testGetAudit_initiallyNull() {
        assertNull(baseDnConfiguration.getAudit());
    }

    @Test
    public void testSetAudit_shouldStoreValue() {
        String auditDn = "ou=audit,o=jans";
        baseDnConfiguration.setAudit(auditDn);
        
        assertEquals(baseDnConfiguration.getAudit(), auditDn);
    }

    @Test
    public void testSetAudit_withNull_shouldAcceptNull() {
        baseDnConfiguration.setAudit("ou=audit,o=jans");
        baseDnConfiguration.setAudit(null);
        
        assertNull(baseDnConfiguration.getAudit());
    }

    @Test
    public void testGetStat_initiallyNull() {
        assertNull(baseDnConfiguration.getStat());
    }

    @Test
    public void testSetStat_shouldStoreValue() {
        String statDn = "ou=stat,o=jans";
        baseDnConfiguration.setStat(statDn);
        
        assertEquals(baseDnConfiguration.getStat(), statDn);
    }

    @Test
    public void testMultipleProperties_shouldBeIndependent() {
        baseDnConfiguration.setConfiguration("ou=configuration,o=jans");
        baseDnConfiguration.setAudit("ou=audit,o=jans");
        baseDnConfiguration.setStat("ou=stat,o=jans");
        
        assertEquals(baseDnConfiguration.getConfiguration(), "ou=configuration,o=jans");
        assertEquals(baseDnConfiguration.getAudit(), "ou=audit,o=jans");
        assertEquals(baseDnConfiguration.getStat(), "ou=stat,o=jans");
    }

    @Test
    public void testSetAudit_withEmptyString_shouldStoreEmptyString() {
        baseDnConfiguration.setAudit("");
        
        assertEquals(baseDnConfiguration.getAudit(), "");
    }

    @Test
    public void testSetAudit_withComplexDn_shouldStoreCorrectly() {
        String complexDn = "inum=12345,ou=audit,ou=lock,o=jans";
        baseDnConfiguration.setAudit(complexDn);
        
        assertEquals(baseDnConfiguration.getAudit(), complexDn);
    }
}