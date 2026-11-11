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
    void serializedConfiguration_roundTrips() throws Exception {
        Fido2Configuration conf = new Fido2Configuration();
        conf.setFido2TraceEnabled(true);
        conf.setFido2TraceProducerId("fido-node-1");
        conf.setFido2TraceSigningKey("fido-trace-key");
        conf.setFido2TraceChainGenesis("pre_registered");
        conf.setFido2TraceIncludeCredentialIdHash(false);
        conf.setFido2TraceIncludeAttestationDetail(true);
        conf.setFido2TraceEndpoint("https://lock.example.com/api/v1/audit/trace");

        ObjectMapper mapper = new ObjectMapper();
        Fido2Configuration copy = mapper.readValue(mapper.writeValueAsString(conf), Fido2Configuration.class);
        assertTrue(copy.isFido2TraceEnabled());
        assertEquals("fido-node-1", copy.getFido2TraceProducerId());
        assertEquals("fido-trace-key", copy.getFido2TraceSigningKey());
        assertEquals("pre_registered", copy.getFido2TraceChainGenesis());
        assertFalse(copy.isFido2TraceIncludeCredentialIdHash());
        assertTrue(copy.isFido2TraceIncludeAttestationDetail());
        assertEquals("https://lock.example.com/api/v1/audit/trace", copy.getFido2TraceEndpoint());
    }

    @Test
    void signingKey_rejectsKeyMaterial() {
        Fido2Configuration conf = new Fido2Configuration();
        assertThrows(IllegalArgumentException.class,
                () -> conf.setFido2TraceSigningKey("-----BEGIN PRIVATE KEY-----\nMC4CAQAw\n-----END PRIVATE KEY-----"));
        assertThrows(IllegalArgumentException.class, () -> conf.setFido2TraceSigningKey("key\nmore"));
        assertThrows(IllegalArgumentException.class, () -> conf.setFido2TraceSigningKey("MC4CAQAwBQYDK2VwBCIEIPz+abc/def=="));
        assertEquals(null, conf.getFido2TraceSigningKey());
    }

    @Test
    void signingKey_acceptsKeyStoreAlias() {
        Fido2Configuration conf = new Fido2Configuration();
        conf.setFido2TraceSigningKey("fido.trace-key_1");
        assertEquals("fido.trace-key_1", conf.getFido2TraceSigningKey());
        conf.setFido2TraceSigningKey("");
        assertEquals("", conf.getFido2TraceSigningKey());
    }
}
