/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class NativeReadinessConfigurationTest {

    @Test
    void defaults_areAllOff() {
        Fido2Configuration conf = new Fido2Configuration();
        assertFalse(conf.isNativeReadinessEnabled());
        assertFalse(conf.isNativeReadinessAllowExternalFetch());
        assertEquals(131072, conf.getNativeReadinessMaxResponseBytes());
        assertEquals(3000, conf.getNativeReadinessTimeoutMs());
        assertEquals(3600, conf.getNativeReadinessCacheTtlSeconds());
    }

    @Test
    void storedJsonWithoutTheProperties_keepsTheDefaults() throws Exception {
        Fido2Configuration conf = new ObjectMapper().readValue("{\"lockAuditEnabled\":true}", Fido2Configuration.class);
        assertFalse(conf.isNativeReadinessEnabled());
        assertFalse(conf.isNativeReadinessAllowExternalFetch());
        assertEquals(131072, conf.getNativeReadinessMaxResponseBytes());
        assertEquals(3000, conf.getNativeReadinessTimeoutMs());
        assertEquals(3600, conf.getNativeReadinessCacheTtlSeconds());
    }

    @Test
    void storedValues_areRead() throws Exception {
        Fido2Configuration conf = new ObjectMapper().readValue(
                "{\"nativeReadinessEnabled\":true,\"nativeReadinessAllowExternalFetch\":true,"
                        + "\"nativeReadinessMaxResponseBytes\":1024,\"nativeReadinessTimeoutMs\":500,"
                        + "\"nativeReadinessCacheTtlSeconds\":60}", Fido2Configuration.class);
        assertTrue(conf.isNativeReadinessEnabled());
        assertTrue(conf.isNativeReadinessAllowExternalFetch());
        assertEquals(1024, conf.getNativeReadinessMaxResponseBytes());
        assertEquals(500, conf.getNativeReadinessTimeoutMs());
        assertEquals(60, conf.getNativeReadinessCacheTtlSeconds());
    }
}
