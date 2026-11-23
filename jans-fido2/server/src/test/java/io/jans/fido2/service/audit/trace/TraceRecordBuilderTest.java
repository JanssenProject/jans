/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.service.audit.trace.TraceRecordBuilder.CeremonyType;
import io.jans.fido2.service.audit.trace.TraceRecordBuilder.Outcome;
import io.jans.util.security.SecurityProviderUtility;

class TraceRecordBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant FIXED = Instant.parse("2026-11-20T10:00:00Z");
    private static final String SESSION_ID = "sess-1";
    private static final String SESSION_ISSUER = "https://op.example.org";

    private static KeyPair keys;

    @TempDir
    Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        SecurityProviderUtility.installBCProvider(true);
        keys = KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider()).generateKeyPair();
    }

    private static TraceRecordBuilder builder() {
        return new TraceRecordBuilder("fido2-test", "node-a", Clock.fixed(FIXED, ZoneOffset.UTC), () -> "rec-1");
    }

    private static ObjectNode expected(CeremonyType type, Outcome outcome) {
        String json = "{"
                + "\"record_id\":\"rec-1\","
                + "\"producer\":\"fido2-test\","
                + "\"producer_chain\":{\"producer_id\":\"fido2-test\",\"producer_instance_id\":\"node-a\"},"
                + "\"trace\":{"
                + "\"eat_profile\":\"tag:jans.io,2026:trace-v1\","
                + "\"event_kind\":\"FIDO_CEREMONY\","
                + "\"signed_at\":" + FIXED.getEpochSecond() + ","
                + "\"session_id\":\"" + SESSION_ID + "\","
                + "\"session_issuer\":\"" + SESSION_ISSUER + "\","
                + "\"event\":{\"ceremony_type\":\"" + type.name() + "\",\"ceremony_outcome\":\"" + outcome.name() + "\"}"
                + "}}";
        try {
            return (ObjectNode) MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void goldenRecordForEveryTypeAndOutcome() {
        for (CeremonyType type : CeremonyType.values()) {
            for (Outcome outcome : Outcome.values()) {
                ObjectNode record = builder().build(new TraceRecordBuilder.Ceremony(type, outcome, SESSION_ID, SESSION_ISSUER));
                assertEquals(TraceCanonicalizer.canonicalString(expected(type, outcome)),
                        TraceCanonicalizer.canonicalString(record), type + "/" + outcome);
            }
        }
    }

    @Test
    void recordHoldsNoDecisionPolicyOrIdentityFields() {
        ObjectNode record = builder().build(new TraceRecordBuilder.Ceremony(CeremonyType.ASSERTION, Outcome.FAILURE,
                SESSION_ID, SESSION_ISSUER));

        assertEquals(keys("record_id", "producer", "producer_chain", "trace"), keys(record));
        assertEquals(keys("eat_profile", "event_kind", "signed_at", "session_id", "session_issuer", "event"), keys(record.get("trace")));
        assertEquals(keys("ceremony_type", "ceremony_outcome"), keys(record.get("trace").get("event")));
        String text = record.toString().toLowerCase();
        for (String forbidden : new String[] {"decision", "invocation", "policy", "username", "credential", "attestation", "subject"}) {
            assertFalse(text.contains(forbidden), "record must not contain " + forbidden);
        }
    }

    @Test
    void rejectsMissingSessionId() {
        assertThrows(IllegalArgumentException.class,
                () -> new TraceRecordBuilder.Ceremony(CeremonyType.REGISTRATION, Outcome.SUCCESS, null, SESSION_ISSUER));
        assertThrows(IllegalArgumentException.class,
                () -> new TraceRecordBuilder.Ceremony(CeremonyType.REGISTRATION, Outcome.SUCCESS, "  ", SESSION_ISSUER));
    }

    @Test
    void rejectsMissingSessionIssuer() {
        assertThrows(IllegalArgumentException.class,
                () -> new TraceRecordBuilder.Ceremony(CeremonyType.REGISTRATION, Outcome.SUCCESS, SESSION_ID, null));
    }

    @Test
    void rejectsSessionIssuerThatLockWouldRefuse() {
        for (String bad : new String[] {"http://op.example.org", "not a uri", "urn:x:y", "ftp://op.example.org"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new TraceRecordBuilder.Ceremony(CeremonyType.ASSERTION, Outcome.SUCCESS, SESSION_ID, bad), bad);
        }
        // http is allowed only for localhost
        new TraceRecordBuilder.Ceremony(CeremonyType.ASSERTION, Outcome.SUCCESS, SESSION_ID, "http://localhost:8080");
    }

    @Test
    void twoSignedRecordsFormAVerifiableLinkAndTamperingIsDetected() throws Exception {
        TraceChainState chain = new TraceChainState(dir.resolve("chain2.json"), "node-a",
                new TraceSigner(keys.getPrivate()));
        ObjectNode first = chain.signAndAdvance(builder().build(new TraceRecordBuilder.Ceremony(
                CeremonyType.REGISTRATION, Outcome.SUCCESS, SESSION_ID, SESSION_ISSUER)));
        ObjectNode second = chain.signAndAdvance(new TraceRecordBuilder("fido2-test", "node-a",
                Clock.fixed(FIXED, ZoneOffset.UTC), () -> "rec-2").build(new TraceRecordBuilder.Ceremony(
                CeremonyType.ASSERTION, Outcome.FAILURE, "sess-2", SESSION_ISSUER)));

        assertEquals(TraceChainState.ZERO_HASH, first.get("producer_chain").get("prev_record_hash").asText());
        String firstHash = TraceHashes.sha256Prefixed(TraceCanonicalizer.canonicalBytes(first));
        assertEquals(firstHash, second.get("producer_chain").get("prev_record_hash").asText());
        assertTrue(verifies(first, keys.getPublic()));
        assertTrue(verifies(second, keys.getPublic()));

        ObjectNode tampered = first.deepCopy();
        ((ObjectNode) tampered.get("trace").get("event")).put("ceremony_outcome", "FAILURE");
        assertFalse(verifies(tampered, keys.getPublic()));
        ObjectNode tamperedSession = first.deepCopy();
        ((ObjectNode) tamperedSession.get("trace")).put("session_id", "other");
        assertFalse(verifies(tamperedSession, keys.getPublic()));
    }

    @Test
    void signedRecordVerifiesAndCarriesChainFields() throws Exception {
        ObjectNode unsigned = builder().build(new TraceRecordBuilder.Ceremony(CeremonyType.REGISTRATION, Outcome.SUCCESS,
                SESSION_ID, SESSION_ISSUER));
        TraceChainState chain = new TraceChainState(dir.resolve("chain.json"), "node-a",
                new TraceSigner(keys.getPrivate()));

        ObjectNode signed = chain.signAndAdvance(unsigned);

        ObjectNode producerChain = (ObjectNode) signed.get("producer_chain");
        assertEquals(1, producerChain.get("sequence_number").asLong());
        assertEquals("fido2-test", producerChain.get("producer_id").asText());
        assertEquals("node-a", producerChain.get("producer_instance_id").asText());
        assertTrue(verifies(signed, keys.getPublic()));
    }

    private static boolean verifies(ObjectNode signed, PublicKey publicKey) throws Exception {
        ObjectNode unsigned = signed.deepCopy();
        String sig = unsigned.remove("signature").asText();
        Signature verifier = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        verifier.initVerify(publicKey);
        verifier.update(TraceCanonicalizer.canonicalBytes(unsigned));
        return verifier.verify(Base64.getUrlDecoder().decode(sig));
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> names = new TreeSet<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    private static Set<String> keys(String... names) {
        return new TreeSet<>(java.util.Arrays.asList(names));
    }
}
