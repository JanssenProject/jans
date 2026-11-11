/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TraceConfigurationTest {

    @Test
    void defaults_areOffExceptCredentialIdHash() {
        Fido2Configuration conf = new Fido2Configuration();
        assertFalse(conf.isFido2TraceEnabled());
        assertEquals("first_observed", conf.getFido2TraceChainGenesis());
        assertTrue(conf.isFido2TraceIncludeCredentialIdHash());
        assertFalse(conf.isFido2TraceIncludeAttestationDetail());
    }

    @Test
    void storedJsonWithoutTraceKeys_keepsTheDefaults() throws Exception {
        Fido2Configuration conf = new ObjectMapper().readValue("{\"lockAuditEnabled\":true}", Fido2Configuration.class);
        assertFalse(conf.isFido2TraceEnabled());
        assertEquals("first_observed", conf.getFido2TraceChainGenesis());
        assertTrue(conf.isFido2TraceIncludeCredentialIdHash());
        assertFalse(conf.isFido2TraceIncludeAttestationDetail());
    }

    @Test
    void storedValues_areRead() throws Exception {
        String json = "{\"fido2TraceEnabled\":true,\"fido2TraceProducerId\":\"fido-node-1\","
                + "\"fido2TraceSigningKey\":\"fido-trace-key\",\"fido2TraceChainGenesis\":\"pre_registered\","
                + "\"fido2TraceIncludeCredentialIdHash\":false,\"fido2TraceIncludeAttestationDetail\":true,"
                + "\"fido2TraceEndpoint\":\"https://lock.example.com/api/v1/audit/trace\"}";
        Fido2Configuration conf = new ObjectMapper().readValue(json, Fido2Configuration.class);
        assertTrue(conf.isFido2TraceEnabled());
        assertEquals("fido-node-1", conf.getFido2TraceProducerId());
        assertEquals("fido-trace-key", conf.getFido2TraceSigningKey());
        assertEquals("pre_registered", conf.getFido2TraceChainGenesis());
        assertFalse(conf.isFido2TraceIncludeCredentialIdHash());
        assertTrue(conf.isFido2TraceIncludeAttestationDetail());
        assertEquals("https://lock.example.com/api/v1/audit/trace", conf.getFido2TraceEndpoint());
    }

    @Test
    void signingKey_rejectsInlinePemKeyMaterial() {
        Fido2Configuration conf = new Fido2Configuration();
        assertThrows(IllegalArgumentException.class,
                () -> conf.setFido2TraceSigningKey("-----BEGIN PRIVATE KEY-----\nMC4CAQAw\n-----END PRIVATE KEY-----"));
        assertThrows(IllegalArgumentException.class, () -> conf.setFido2TraceSigningKey("key\nmore"));
        assertEquals(null, conf.getFido2TraceSigningKey());
    }
}
