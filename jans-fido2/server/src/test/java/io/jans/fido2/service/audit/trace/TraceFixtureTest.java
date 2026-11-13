/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.util.security.SecurityProviderUtility;

/**
 * Pins the cross-check fixture shared with Lock (jans-lock trace-crypto-java, TraceFixtureTest).
 * Ed25519 signatures are deterministic, so the signature must match byte for byte.
 */
class TraceFixtureTest {

    private static final String PKCS8_PREFIX = "302e020100300506032b657004220420";

    @BeforeAll
    static void installProvider() {
        SecurityProviderUtility.installBCProvider(true);
    }

    @Test
    void canonicalFormAndSignatureMatchFixture() throws Exception {
        JsonNode fixture;
        try (InputStream in = getClass().getResourceAsStream("trace-ed25519-fixture.json")) {
            fixture = new ObjectMapper().readTree(in);
        }
        JsonNode recordNode = fixture.get("record");

        assertEquals(fixture.get("canonical").asText(), TraceCanonicalizer.canonicalString(recordNode));

        String seed = fixture.get("seed").asText();
        PrivateKey key = KeyFactory.getInstance("Ed25519", SecurityProviderUtility.getBCProvider())
                .generatePrivate(new PKCS8EncodedKeySpec(hex(PKCS8_PREFIX + seed)));
        assertEquals(fixture.get("signature").asText(), new TraceSigner(key).sign(recordNode));
        assertEquals(32, Base64.getUrlDecoder().decode(fixture.get("jwk").get("x").asText()).length);
    }

    private static byte[] hex(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
