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
 * Unit tests for LockProtectionMode enum
 * 
 * @author Janssen Project
 */
public class LockProtectionModeTest {

    @Test
    public void testEnumValues_shouldContainExpectedModes() {
        LockProtectionMode[] modes = LockProtectionMode.values();
        
        assertEquals(modes.length, 2);
        assertEquals(modes[0], LockProtectionMode.OAUTH);
        assertEquals(modes[1], LockProtectionMode.CEDARLING);
    }

    @Test
    public void testGetMode_oauth_shouldReturnLowercaseOauth() {
        String mode = LockProtectionMode.OAUTH.getmode();
        
        assertEquals(mode, "oauth");
    }

    @Test
    public void testGetMode_cedarling_shouldReturnLowercaseCedarling() {
        String mode = LockProtectionMode.CEDARLING.getmode();
        
        assertEquals(mode, "cedarling");
    }

    @Test
    public void testValueOf_withValidName_shouldReturnCorrectEnum() {
        LockProtectionMode oauth = LockProtectionMode.valueOf("OAUTH");
        LockProtectionMode cedarling = LockProtectionMode.valueOf("CEDARLING");
        
        assertEquals(oauth, LockProtectionMode.OAUTH);
        assertEquals(cedarling, LockProtectionMode.CEDARLING);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueOf_withInvalidName_shouldThrowException() {
        LockProtectionMode.valueOf("INVALID");
    }

    @Test
    public void testJsonSerialization_shouldUseLowercaseValue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        String oauthJson = mapper.writeValueAsString(LockProtectionMode.OAUTH);
        String cedarlingJson = mapper.writeValueAsString(LockProtectionMode.CEDARLING);
        
        assertEquals(oauthJson, "\"oauth\"");
        assertEquals(cedarlingJson, "\"cedarling\"");
    }

    @Test
    public void testJsonDeserialization_shouldParseCorrectly() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        LockProtectionMode oauth = mapper.readValue("\"oauth\"", LockProtectionMode.class);
        LockProtectionMode cedarling = mapper.readValue("\"cedarling\"", LockProtectionMode.class);
        
        assertEquals(oauth, LockProtectionMode.OAUTH);
        assertEquals(cedarling, LockProtectionMode.CEDARLING);
    }

    @Test
    public void testEquality_sameEnum_shouldBeEqual() {
        LockProtectionMode mode1 = LockProtectionMode.OAUTH;
        LockProtectionMode mode2 = LockProtectionMode.OAUTH;
        
        assertSame(mode1, mode2);
        assertEquals(mode1, mode2);
    }

    @Test
    public void testEquality_differentEnum_shouldNotBeEqual() {
        LockProtectionMode oauth = LockProtectionMode.OAUTH;
        LockProtectionMode cedarling = LockProtectionMode.CEDARLING;
        
        assertNotEquals(oauth, cedarling);
    }

    @Test
    public void testToString_shouldReturnEnumName() {
        assertEquals(LockProtectionMode.OAUTH.toString(), "OAUTH");
        assertEquals(LockProtectionMode.CEDARLING.toString(), "CEDARLING");
    }
}