/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Per-node TRACE hash chain: producer_chain_id, sequence_number and prev_record_hash.
 *
 * <p>{@link #signAndAdvance(ObjectNode)} is the only way to move the chain. Under one lock it builds the
 * producer_chain member, signs the record, persists the next state, and only then returns the signed record.
 * Consequences:
 * <ul>
 * <li>A signer failure leaves the state unchanged.</li>
 * <li>A persistence failure releases no record, so the same sequence number is reissued after restart. A crash
 * after persistence but before the record is sent leaves a gap, never a duplicate.</li>
 * </ul>
 *
 * <p>The first record of a chain has sequence_number 1 and the all-zero prev_record_hash. Lock rejects any other
 * genesis position. Restart resumes the saved chain when its state is usable. Otherwise it starts a new chain with
 * a new producer_chain_id and, if the previous chain id for this node is still known, a chain_link on its genesis.
 */
public final class TraceChainState {

    static final String ZERO_HASH = "sha256:" + "0".repeat(64);

    private static final String PRODUCER_CHAIN = "producer_chain";
    private static final String PRODUCER_CHAIN_ID = "producer_chain_id";
    private static final String SEQUENCE_NUMBER = "sequence_number";
    private static final String PREV_RECORD_HASH = "prev_record_hash";
    private static final String CHAIN_LINK = "chain_link";
    private static final String PREV_PRODUCER_CHAIN_ID = "prev_producer_chain_id";
    private static final String SIGNATURE = "signature";
    private static final String NODE_ID = "node_id";
    private static final String NEXT_SEQUENCE = "next_sequence";
    private static final String CHAIN_LINK_PREVIOUS = "chain_link_previous";

    private static final Pattern SHA256_PREFIXED = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Writes the state file. Replaceable in tests to simulate a failed or interrupted write. */
    interface StateWriter {
        void write(Path target, byte[] bytes) throws IOException;
    }

    private final TraceSigner signer;
    private final Path stateFile;
    private final String nodeId;
    private final StateWriter writer;

    // Guarded by "this".
    private String chainId;
    private long nextSequence;
    private String prevRecordHash;
    private String chainLinkPrevious;

    /**
     * @param stateFile file that holds the chain state for this node
     * @param nodeId node identifier, for example MetricService.getNodeIdentifier()
     * @param signer Ed25519 TRACE signer
     */
    public TraceChainState(Path stateFile, String nodeId, TraceSigner signer) {
        this(stateFile, nodeId, signer, TraceChainState::writeAtomically);
    }

    TraceChainState(Path stateFile, String nodeId, TraceSigner signer, StateWriter writer) {
        if (stateFile == null || nodeId == null || nodeId.isEmpty() || signer == null || writer == null) {
            throw new IllegalArgumentException("stateFile, nodeId, signer and writer are required");
        }
        this.stateFile = stateFile;
        this.nodeId = nodeId;
        this.signer = signer;
        this.writer = writer;
        load();
    }

    /**
     * Signs the record and advances the chain. Adds or replaces producer_chain (producer_chain_id, sequence_number,
     * prev_record_hash, and chain_link on a genesis record when known) and signature. The caller's producer_id and
     * producer_instance_id are kept.
     *
     * @param unsigned unsigned record; it is not modified
     * @return a signed copy of the record
     * @throws IllegalStateException if signing or persisting fails; the chain does not advance
     */
    public synchronized ObjectNode signAndAdvance(ObjectNode unsigned) {
        ObjectNode signed = unsigned.deepCopy();
        signed.remove(SIGNATURE);
        ObjectNode chain = chainOf(signed);
        chain.put(PRODUCER_CHAIN_ID, chainId);
        chain.put(SEQUENCE_NUMBER, nextSequence);
        chain.put(PREV_RECORD_HASH, prevRecordHash);
        if (nextSequence == 1 && chainLinkPrevious != null) {
            chain.putObject(CHAIN_LINK).put(PREV_PRODUCER_CHAIN_ID, chainLinkPrevious);
        } else {
            chain.remove(CHAIN_LINK);
        }

        signed.put(SIGNATURE, signer.sign(signed));
        String digest = TraceHashes.sha256Prefixed(TraceCanonicalizer.canonicalBytes(signed));

        // Persist first. Memory advances only after the file is written.
        persist(chainId, nextSequence + 1, digest, null);
        nextSequence++;
        prevRecordHash = digest;
        chainLinkPrevious = null;
        return signed;
    }

    /** @return the current producer_chain_id (for tests and diagnostics) */
    public synchronized String getChainId() {
        return chainId;
    }

    /** @return the sequence number the next record will receive */
    public synchronized long getNextSequence() {
        return nextSequence;
    }

    /** Returns the producer_chain object of the record, creating it when absent or not an object. */
    private static ObjectNode chainOf(ObjectNode record) {
        JsonNode existing = record.get(PRODUCER_CHAIN);
        return existing instanceof ObjectNode ? (ObjectNode) existing : record.putObject(PRODUCER_CHAIN);
    }

    private void load() {
        JsonNode saved = readSaved();
        boolean sameNode = saved != null && nodeId.equals(text(saved, NODE_ID));
        if (sameNode && isUsable(saved)) {
            chainId = text(saved, PRODUCER_CHAIN_ID);
            nextSequence = saved.get(NEXT_SEQUENCE).asLong();
            prevRecordHash = text(saved, PREV_RECORD_HASH);
            chainLinkPrevious = text(saved, CHAIN_LINK_PREVIOUS);
            return;
        }
        // Unusable state: start a new chain. Link to the previous one only when it is still known for this node.
        String previous = sameNode ? text(saved, PRODUCER_CHAIN_ID) : null;
        chainId = "chain-" + UUID.randomUUID();
        nextSequence = 1;
        prevRecordHash = ZERO_HASH;
        chainLinkPrevious = previous;
        persist(chainId, nextSequence, prevRecordHash, chainLinkPrevious);
    }

    private JsonNode readSaved() {
        try {
            if (!Files.exists(stateFile)) {
                return null;
            }
            return MAPPER.readTree(Files.readAllBytes(stateFile));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean isUsable(JsonNode saved) {
        String id = text(saved, PRODUCER_CHAIN_ID);
        JsonNode next = saved.get(NEXT_SEQUENCE);
        String prev = text(saved, PREV_RECORD_HASH);
        if (id == null || id.isEmpty() || next == null || !next.canConvertToLong() || next.asLong() < 1) {
            return false;
        }
        if (prev == null || !SHA256_PREFIXED.matcher(prev).matches()) {
            return false;
        }
        return next.asLong() != 1 || ZERO_HASH.equals(prev);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private void persist(String chain, long next, String prev, String link) {
        ObjectNode state = MAPPER.createObjectNode();
        state.put(NODE_ID, nodeId);
        state.put(PRODUCER_CHAIN_ID, chain);
        state.put(NEXT_SEQUENCE, next);
        state.put(PREV_RECORD_HASH, prev);
        if (link != null) {
            state.put(CHAIN_LINK_PREVIOUS, link);
        }
        try {
            writer.write(stateFile, MAPPER.writeValueAsBytes(state));
        } catch (IOException e) {
            throw new IllegalStateException("TRACE chain state could not be persisted", e);
        }
    }

    static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, target.getFileName().toString(), ".tmp");
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.write(ByteBuffer.wrap(bytes));
            channel.force(true);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
