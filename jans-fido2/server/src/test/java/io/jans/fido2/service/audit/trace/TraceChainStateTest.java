/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.util.security.SecurityProviderUtility;

class TraceChainStateTest {

    private static final String NODE = "node-a";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static PrivateKey ed25519Key;

    @TempDir
    Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        SecurityProviderUtility.installBCProvider(true);
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider()).generateKeyPair();
        ed25519Key = pair.getPrivate();
    }

    private static ObjectNode unsignedRecord() {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("record_id", UUID.randomUUID().toString());
        payload.put("ceremony_outcome", "SUCCESS");
        payload.putObject("producer_chain").put("producer_id", "fido2-test");
        return payload;
    }

    private static ObjectNode signNew(TraceChainState state) {
        return state.signAndAdvance(unsignedRecord());
    }

    private static String hashOf(ObjectNode signed) {
        return TraceHashes.sha256Prefixed(TraceCanonicalizer.canonicalBytes(signed));
    }

    private static void writeOrFail(AtomicBoolean failWrites, Path target, byte[] bytes) throws IOException {
        if (failWrites.get()) {
            throw new IOException("simulated crash during write");
        }
        TraceChainState.writeAtomically(target, bytes);
    }

    private TraceChainState open(Path file) {
        return new TraceChainState(file, NODE, new TraceSigner(ed25519Key));
    }

    @Test
    void firstRecordIsGenesisWithZeroPrevHash() {
        TraceChainState state = open(dir.resolve("chain.json"));

        ObjectNode signed = signNew(state);

        ObjectNode chain = (ObjectNode) signed.get("producer_chain");
        assertEquals(1, chain.get("sequence_number").asLong());
        assertEquals(TraceChainState.ZERO_HASH, chain.get("prev_record_hash").asText());
        assertEquals(state.getChainId(), chain.get("producer_chain_id").asText());
        assertEquals("fido2-test", chain.get("producer_id").asText());
        assertNull(chain.get("chain_link"));
        assertTrue(signed.hasNonNull("signature"));
    }

    @Test
    void signAndAdvanceDoesNotModifyTheInputRecord() {
        TraceChainState state = open(dir.resolve("chain.json"));
        ObjectNode input = unsignedRecord();

        state.signAndAdvance(input);

        assertFalse(input.has("signature"));
        assertFalse(input.get("producer_chain").has("sequence_number"));
    }

    @Test
    void concurrentCallsGiveConsecutiveSequencesAndValidLinks() throws Exception {
        TraceChainState state = open(dir.resolve("chain.json"));
        int total = 400;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<ObjectNode>> futures = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                futures.add(pool.submit(() -> signNew(state)));
            }
            List<ObjectNode> bySequence = new ArrayList<>();
            for (Future<ObjectNode> f : futures) {
                bySequence.add(f.get());
            }
            bySequence.sort((a, b) -> Long.compare(seq(a), seq(b)));

            String expectedPrev = TraceChainState.ZERO_HASH;
            for (int i = 0; i < total; i++) {
                ObjectNode signed = bySequence.get(i);
                assertEquals(i + 1, seq(signed));
                assertEquals(expectedPrev, signed.get("producer_chain").get("prev_record_hash").asText());
                expectedPrev = hashOf(signed);
            }
            assertEquals(total + 1, state.getNextSequence());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void restartResumesTheSavedChain() {
        Path file = dir.resolve("chain.json");
        TraceChainState first = open(file);
        ObjectNode last = null;
        for (int i = 0; i < 3; i++) {
            last = signNew(first);
        }

        TraceChainState restarted = open(file);
        ObjectNode next = signNew(restarted);

        assertEquals(first.getChainId(), restarted.getChainId());
        assertEquals(4, seq(next));
        assertEquals(hashOf(last), next.get("producer_chain").get("prev_record_hash").asText());
        assertNull(next.get("producer_chain").get("chain_link"));
    }

    @Test
    void corruptStateStartsNewChainWithoutLinkWhenPreviousIsUnknown() throws IOException {
        Path file = dir.resolve("chain.json");
        Files.write(file, "not json".getBytes("UTF-8"));

        TraceChainState state = open(file);
        ObjectNode signed = signNew(state);

        assertEquals(1, seq(signed));
        assertEquals(TraceChainState.ZERO_HASH, signed.get("producer_chain").get("prev_record_hash").asText());
        assertNull(signed.get("producer_chain").get("chain_link"));
    }

    @Test
    void unusableStateStartsNewChainAndLinksToPreviousChain() throws IOException {
        Path file = dir.resolve("chain.json");
        TraceChainState original = open(file);
        signNew(original);
        String oldChainId = original.getChainId();

        // Same node, but the stored hash is malformed, so the state cannot be resumed.
        ObjectNode broken = (ObjectNode) MAPPER.readTree(Files.readAllBytes(file));
        broken.put("prev_record_hash", "bogus");
        Files.write(file, MAPPER.writeValueAsBytes(broken));

        TraceChainState restarted = open(file);
        ObjectNode genesis = signNew(restarted);
        ObjectNode second = signNew(restarted);

        assertNotEquals(oldChainId, restarted.getChainId());
        assertEquals(1, seq(genesis));
        assertEquals(oldChainId, genesis.get("producer_chain").get("chain_link").get("prev_producer_chain_id").asText());
        assertEquals(2, seq(second));
        assertNull(second.get("producer_chain").get("chain_link"));
    }

    @Test
    void signerFailureLeavesStateUnchanged() {
        Path file = dir.resolve("chain.json");
        TraceChainState good = open(file);
        ObjectNode genesis = signNew(good);
        String chainId = good.getChainId();

        // An RSA key cannot sign Ed25519, so the signer throws.
        PrivateKey rsa = rsaKey();
        TraceChainState failing = new TraceChainState(file, NODE, new TraceSigner(rsa));
        assertThrows(IllegalStateException.class, () -> signNew(failing));
        assertEquals(2, failing.getNextSequence());

        // Restart from disk: the chain is still at sequence 2 and links to the genesis record.
        TraceChainState restarted = open(file);
        ObjectNode next = signNew(restarted);
        assertEquals(chainId, restarted.getChainId());
        assertEquals(2, seq(next));
        assertEquals(hashOf(genesis), next.get("producer_chain").get("prev_record_hash").asText());
    }

    @Test
    void persistFailureReleasesNoRecordAndRestartReissuesTheSequence() {
        Path file = dir.resolve("chain.json");
        AtomicBoolean failWrites = new AtomicBoolean(false);
        TraceChainState.StateWriter flaky = (target, bytes) -> writeOrFail(failWrites, target, bytes);
        TraceChainState state = new TraceChainState(file, NODE, new TraceSigner(ed25519Key), flaky);
        signNew(state);
        ObjectNode second = signNew(state);

        failWrites.set(true);
        assertThrows(IllegalStateException.class, () -> signNew(state));
        assertEquals(3, state.getNextSequence());

        // After restart the same sequence number is produced again, and it links to record 2.
        TraceChainState restarted = open(file);
        ObjectNode reissued = signNew(restarted);
        assertEquals(3, seq(reissued));
        assertEquals(hashOf(second), reissued.get("producer_chain").get("prev_record_hash").asText());
    }

    private static long seq(ObjectNode signed) {
        return signed.get("producer_chain").get("sequence_number").asLong();
    }

    private static PrivateKey rsaKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair().getPrivate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
