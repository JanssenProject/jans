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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.util.security.SecurityProviderUtility;

class TraceChainStateTest {

    private static final String NODE = "node-a";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String HASH_A = "sha256:" + "a".repeat(64);
    private static final String MAX_SEQUENCE = "9007199254740991";

    private static PrivateKey ed25519Key;
    private static PublicKey ed25519Public;

    @TempDir
    Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        SecurityProviderUtility.installBCProvider(true);
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider()).generateKeyPair();
        ed25519Key = pair.getPrivate();
        ed25519Public = pair.getPublic();
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

    @Test
    void retryAfterFailedWriteOnSameInstanceReissuesTheSequence() {
        Path file = dir.resolve("chain.json");
        AtomicBoolean failWrites = new AtomicBoolean(false);
        TraceChainState.StateWriter flaky = (target, bytes) -> writeOrFail(failWrites, target, bytes);
        TraceChainState state = new TraceChainState(file, NODE, new TraceSigner(ed25519Key), flaky);
        signNew(state);
        ObjectNode second = signNew(state);

        failWrites.set(true);
        assertThrows(IllegalStateException.class, () -> signNew(state));

        failWrites.set(false);
        ObjectNode retried = signNew(state);
        assertEquals(3, seq(retried));
        assertEquals(hashOf(second), retried.get("producer_chain").get("prev_record_hash").asText());
        assertEquals(4, state.getNextSequence());
    }

    @Test
    void returnedRecordsVerifyAndTamperingBreaksTheLink() throws Exception {
        TraceChainState state = open(dir.resolve("chain.json"));
        ObjectNode first = signNew(state);
        ObjectNode second = signNew(state);

        assertTrue(verifies(first));
        assertTrue(verifies(second));
        assertEquals(hashOf(first), second.get("producer_chain").get("prev_record_hash").asText());

        // Change record_id and keep the old signature: verification fails and the link no longer matches.
        ObjectNode tampered = first.deepCopy();
        tampered.put("record_id", UUID.randomUUID().toString());
        assertFalse(verifies(tampered));
        assertNotEquals(hashOf(tampered), second.get("producer_chain").get("prev_record_hash").asText());
    }

    @Test
    void concatenatedSnapshotsAreTreatedAsMalformed() throws IOException {
        Path file = dir.resolve("chain.json");
        TraceChainState original = open(file);
        signNew(original);
        signNew(original);
        byte[] snapshot = Files.readAllBytes(file);
        byte[] twice = new byte[snapshot.length * 2];
        System.arraycopy(snapshot, 0, twice, 0, snapshot.length);
        System.arraycopy(snapshot, 0, twice, snapshot.length, snapshot.length);
        Files.write(file, twice);

        TraceChainState restarted = open(file);
        ObjectNode genesis = signNew(restarted);

        assertNotEquals(original.getChainId(), restarted.getChainId());
        assertEquals(1, seq(genesis));
        assertNull(genesis.get("producer_chain").get("chain_link"));
    }

    @Test
    void unreadableStateFileIsReportedAndLeftInPlace() throws IOException {
        // A directory where the state file should be: it exists but cannot be read as a file.
        Path file = dir.resolve("chain.json");
        Files.createDirectories(file);
        AtomicInteger writes = new AtomicInteger();
        TraceChainState.StateWriter counting = (target, bytes) -> {
            writes.incrementAndGet();
            TraceChainState.writeAtomically(target, bytes);
        };

        assertThrows(IllegalStateException.class,
                () -> new TraceChainState(file, NODE, new TraceSigner(ed25519Key), counting));
        assertEquals(0, writes.get());
        assertTrue(Files.isDirectory(file));
    }

    @Test
    void malformedSnapshotsStartANewChainLinkedToTheSavedId() throws IOException {
        String[][] cases = {
                {"\"chain-old\"", "2.5", "\"" + HASH_A + "\""},
                {"\"chain-old\"", "9007199254740993", "\"" + HASH_A + "\""},
                {"\"chain-old\"", "9007199254740992", "\"" + HASH_A + "\""},
                {"\"chain-old\"", "2", "\"" + TraceChainState.ZERO_HASH + "\""},
                {"\"chain-old\"", "1", "\"" + HASH_A + "\""},
                {"\"chain-old\"", "0", "\"" + HASH_A + "\""},
        };
        for (int i = 0; i < cases.length; i++) {
            Path file = dir.resolve("case-" + i + ".json");
            writeRaw(file, savedJson(cases[i][0], cases[i][1], cases[i][2]));

            TraceChainState state = open(file);
            ObjectNode genesis = signNew(state);

            assertNotEquals("chain-old", state.getChainId(), "case " + i);
            assertEquals(1, seq(genesis), "case " + i);
            assertEquals(TraceChainState.ZERO_HASH, genesis.get("producer_chain").get("prev_record_hash").asText());
            assertEquals("chain-old", genesis.get("producer_chain").get("chain_link")
                    .get("prev_producer_chain_id").asText(), "case " + i);
        }
    }

    @Test
    void invalidPriorChainIdIsNeverWrittenAsALink() throws IOException {
        // Malformed state with an empty saved id.
        Path emptyId = dir.resolve("empty-id.json");
        writeRaw(emptyId, savedJson("\"\"", "5", "\"" + HASH_A + "\""));
        assertNull(signNew(open(emptyId)).get("producer_chain").get("chain_link"));

        // Malformed state with an unpaired surrogate in the saved id.
        Path surrogateId = dir.resolve("surrogate-id.json");
        writeRaw(surrogateId, savedJson("\"\\uD800\"", "5", "\"" + HASH_A + "\""));
        assertNull(signNew(open(surrogateId)).get("producer_chain").get("chain_link"));

        // Usable state whose chain_link_previous is empty: the genesis of the resumed chain carries no link.
        Path emptyLink = dir.resolve("empty-link.json");
        writeRaw(emptyLink, "{\"node_id\":\"" + NODE + "\",\"producer_chain_id\":\"chain-x\",\"next_sequence\":1,"
                + "\"prev_record_hash\":\"" + TraceChainState.ZERO_HASH + "\",\"chain_link_previous\":\"\"}");
        ObjectNode genesis = signNew(open(emptyLink));
        assertEquals("chain-x", genesis.get("producer_chain").get("producer_chain_id").asText());
        assertNull(genesis.get("producer_chain").get("chain_link"));
    }

    @Test
    void nearLimitSequenceIsResumedAndExhaustedChainStartsALinkedChain() throws IOException {
        Path file = dir.resolve("chain.json");
        writeRaw(file, savedJson("\"chain-old\"", MAX_SEQUENCE, "\"" + HASH_A + "\""));

        TraceChainState state = open(file);
        ObjectNode last = signNew(state);
        ObjectNode genesis = signNew(state);
        ObjectNode second = signNew(state);

        assertEquals(9007199254740991L, seq(last));
        assertEquals("chain-old", last.get("producer_chain").get("producer_chain_id").asText());
        assertEquals(1, seq(genesis));
        assertNotEquals("chain-old", genesis.get("producer_chain").get("producer_chain_id").asText());
        assertEquals("chain-old",
                genesis.get("producer_chain").get("chain_link").get("prev_producer_chain_id").asText());
        assertEquals(2, seq(second));
        assertEquals(hashOf(genesis), second.get("producer_chain").get("prev_record_hash").asText());
    }

    @Test
    void failedMoveRemovesTempFileAndKeepsTheTarget() throws IOException {
        // A non-empty directory at the target makes the move fail.
        Path target = dir.resolve("chain.json");
        Files.createDirectories(target);
        Files.write(target.resolve("keep.txt"), "x".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> TraceChainState.writeAtomically(target, new byte[] {1, 2, 3}));

        try (Stream<Path> entries = Files.list(dir)) {
            assertEquals(0, entries.filter(p -> p.getFileName().toString().endsWith(".tmp")).count());
        }
        assertTrue(Files.exists(target.resolve("keep.txt")));
    }

    private static void writeRaw(Path file, String json) throws IOException {
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String savedJson(String chainIdJson, String nextJson, String prevJson) {
        return "{\"node_id\":\"" + NODE + "\",\"producer_chain_id\":" + chainIdJson + ",\"next_sequence\":" + nextJson
                + ",\"prev_record_hash\":" + prevJson + "}";
    }

    private boolean verifies(ObjectNode signed) throws Exception {
        ObjectNode unsigned = signed.deepCopy();
        unsigned.remove("signature");
        Signature verifier = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        verifier.initVerify(ed25519Public);
        verifier.update(TraceCanonicalizer.canonicalBytes(unsigned));
        return verifier.verify(Base64.getUrlDecoder().decode(signed.get("signature").asText()));
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
