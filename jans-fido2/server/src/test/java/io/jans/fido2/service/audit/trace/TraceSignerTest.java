/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.util.security.SecurityProviderUtility;

class TraceSignerTest {

    private static final String PKCS8_ED25519_PREFIX = "302e020100300506032b657004220420";

    @BeforeAll
    static void installProvider() {
        SecurityProviderUtility.installBCProvider(true);
    }

    private static byte[] hex(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static ObjectNode sampleRecord() {
        ObjectNode sample = new ObjectMapper().createObjectNode();
        sample.put("sequence_number", 7);
        sample.put("ceremony_outcome", "SUCCESS");
        return sample;
    }

    @Test
    void rfc8032TestVector1() throws Exception {
        // RFC 8032 section 7.1, TEST 1: empty message
        String seed = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";
        KeyFactory factory = KeyFactory.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(hex(PKCS8_ED25519_PREFIX + seed)));

        byte[] signature = new TraceSigner(privateKey).signBytes(new byte[0]);

        assertEquals("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
                toHex(signature));
    }

    @Test
    void sha256OfEmptyInput() {
        assertEquals("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                TraceHashes.sha256Prefixed(new byte[0]));
    }

    @Test
    void signatureVerifiesOverCanonicalForm() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        KeyPair keys = generator.generateKeyPair();
        ObjectNode sample = sampleRecord();

        String signature = new TraceSigner(keys.getPrivate()).sign(sample);

        // Ed25519 signatures are 64 bytes, which is 86 base64url characters without padding
        assertEquals(86, signature.length());
        assertTrue(verify(keys.getPublic(), TraceCanonicalizer.canonicalBytes(sample),
                Base64.getUrlDecoder().decode(signature)));
    }

    @Test
    void tamperedRecordFailsVerification() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        KeyPair keys = generator.generateKeyPair();
        String signature = new TraceSigner(keys.getPrivate()).sign(sampleRecord());

        ObjectNode tampered = sampleRecord();
        tampered.put("ceremony_outcome", "FAILURE");
        assertFalse(verify(keys.getPublic(), TraceCanonicalizer.canonicalBytes(tampered),
                Base64.getUrlDecoder().decode(signature)));
    }

    private static boolean verify(PublicKey publicKey, byte[] message, byte[] signature) throws Exception {
        Signature verifier = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        verifier.initVerify(publicKey);
        verifier.update(message);
        return verifier.verify(signature);
    }
}
