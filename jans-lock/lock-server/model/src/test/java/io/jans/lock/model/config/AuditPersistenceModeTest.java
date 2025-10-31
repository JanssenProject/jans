/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.model.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * Unit tests for AuditPersistenceMode enum
 * 
 * @author Janssen Project
 */
public class AuditPersistenceModeTest {

    @Test
    public void testEnumValues_shouldContainExpectedModes() {
        AuditPersistenceMode[] modes = AuditPersistenceMode.values();
        
        assertEquals(modes.length, 2);
        assertEquals(modes[0], AuditPersistenceMode.INTERNAL);
        assertEquals(modes[1], AuditPersistenceMode.CONFIG_API);
    }

    @Test
    public void testGetMode_internal_shouldReturnLowercaseInternal() {
        String mode = AuditPersistenceMode.INTERNAL.getmode();
        
        assertEquals(mode, "internal");
    }

    @Test
    public void testGetMode_configApi_shouldReturnConfigApi() {
        String mode = AuditPersistenceMode.CONFIG_API.getmode();
        
        assertEquals(mode, "config-api");
    }

    @Test
    public void testValueOf_withValidName_shouldReturnCorrectEnum() {
        AuditPersistenceMode internal = AuditPersistenceMode.valueOf("INTERNAL");
        AuditPersistenceMode configApi = AuditPersistenceMode.valueOf("CONFIG_API");
        
        assertEquals(internal, AuditPersistenceMode.INTERNAL);
        assertEquals(configApi, AuditPersistenceMode.CONFIG_API);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueOf_withInvalidName_shouldThrowException() {
        AuditPersistenceMode.valueOf("EXTERNAL");
    }

    @Test
    public void testJsonSerialization_shouldUseLowercaseValue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        String internalJson = mapper.writeValueAsString(AuditPersistenceMode.INTERNAL);
        String configApiJson = mapper.writeValueAsString(AuditPersistenceMode.CONFIG_API);
        
        assertEquals(internalJson, "\"internal\"");
        assertEquals(configApiJson, "\"config-api\"");
    }

    @Test
    public void testJsonDeserialization_shouldParseCorrectly() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        AuditPersistenceMode internal = mapper.readValue("\"internal\"", AuditPersistenceMode.class);
        AuditPersistenceMode configApi = mapper.readValue("\"config-api\"", AuditPersistenceMode.class);
        
        assertEquals(internal, AuditPersistenceMode.INTERNAL);
        assertEquals(configApi, AuditPersistenceMode.CONFIG_API);
    }

    @Test
    public void testEquality_sameEnum_shouldBeEqual() {
        AuditPersistenceMode mode1 = AuditPersistenceMode.INTERNAL;
        AuditPersistenceMode mode2 = AuditPersistenceMode.INTERNAL;
        
        assertSame(mode1, mode2);
        assertEquals(mode1, mode2);
    }

    @Test
    public void testEquality_differentEnum_shouldNotBeEqual() {
        AuditPersistenceMode internal = AuditPersistenceMode.INTERNAL;
        AuditPersistenceMode configApi = AuditPersistenceMode.CONFIG_API;
        
        assertNotEquals(internal, configApi);
    }
}