/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds the unsigned FIDO_CEREMONY TRACE record for one completed ceremony. No I/O.
 *
 * <p>The record carries the ceremony type, outcome, and session correlation. It carries no policy decision, no
 * username, no credential material, and no raw attestation. The producer_chain member holds only producer_id and
 * producer_instance_id. {@link TraceChainState#signAndAdvance(ObjectNode)} adds the chain fields and the signature.
 *
 * <p>Lock does not accept FIDO_CEREMONY yet. The event kind and member names are set in one place here so a rename
 * is a one-line change. The field placement follows docs/janssen-server/lock/trace.md; the exact profile is still
 * pending.
 */
public final class TraceRecordBuilder {

    /** Event kind. Lock does not accept this value yet. */
    public static final String EVENT_KIND = "FIDO_CEREMONY";

    static final String FIELD_RECORD_ID = "record_id";
    static final String FIELD_PRODUCER = "producer";
    static final String FIELD_PRODUCER_CHAIN = "producer_chain";
    static final String FIELD_PRODUCER_ID = "producer_id";
    static final String FIELD_PRODUCER_INSTANCE_ID = "producer_instance_id";
    static final String FIELD_TRACE = "trace";
    static final String FIELD_EVENT_KIND = "event_kind";
    static final String FIELD_SIGNED_AT = "signed_at";
    static final String FIELD_SESSION_ID = "session_id";
    static final String FIELD_SESSION_ISSUER = "session_issuer";
    static final String FIELD_EVENT = "event";
    static final String FIELD_CEREMONY_TYPE = "ceremony_type";
    static final String FIELD_CEREMONY_OUTCOME = "ceremony_outcome";

    /** Ceremony type: registration or assertion. */
    public enum CeremonyType {
        REGISTRATION, ASSERTION
    }

    /** Ceremony outcome. */
    public enum Outcome {
        SUCCESS, FAILURE, ABANDONED
    }

    /** One completed ceremony, validated on construction. */
    public static final class Ceremony {

        private final CeremonyType type;
        private final Outcome outcome;
        private final String sessionId;
        private final String sessionIssuer;

        /**
         * @param type registration or assertion
         * @param outcome success, failure or abandoned
         * @param sessionId session the ceremony belongs to; required, non-blank
         * @param sessionIssuer issuer of that session; required, non-blank
         * @throws IllegalArgumentException if the session id or issuer is missing or blank
         */
        public Ceremony(CeremonyType type, Outcome outcome, String sessionId, String sessionIssuer) {
            this.type = Objects.requireNonNull(type, "type");
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.sessionId = requireNonBlank(sessionId, "session_id");
            this.sessionIssuer = requireNonBlank(sessionIssuer, "session_issuer");
        }

        CeremonyType getType() {
            return type;
        }

        Outcome getOutcome() {
            return outcome;
        }

        String getSessionId() {
            return sessionId;
        }

        String getSessionIssuer() {
            return sessionIssuer;
        }

        private static String requireNonBlank(String value, String name) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " is required for a TRACE record");
            }
            return value;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String producerId;
    private final String producerInstanceId;
    private final Clock clock;
    private final Supplier<String> recordIds;

    /**
     * @param producerId stable producer identifier
     * @param producerInstanceId identifier of this node within the producer
     * @param clock source of signed_at; a fixed clock makes output deterministic
     * @param recordIds source of record_id values; a UUID is generated when none is given
     */
    public TraceRecordBuilder(String producerId, String producerInstanceId, Clock clock, Supplier<String> recordIds) {
        this.producerId = requireNonBlankArg(producerId, "producerId");
        this.producerInstanceId = requireNonBlankArg(producerInstanceId, "producerInstanceId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.recordIds = Objects.requireNonNull(recordIds, "recordIds");
    }

    /**
     * @param producerId stable producer identifier
     * @param producerInstanceId identifier of this node within the producer
     * @param clock source of signed_at
     */
    public TraceRecordBuilder(String producerId, String producerInstanceId, Clock clock) {
        this(producerId, producerInstanceId, clock, () -> UUID.randomUUID().toString());
    }

    /**
     * @param ceremony the completed ceremony
     * @return an unsigned record; sign it with TraceChainState.signAndAdvance
     */
    public ObjectNode build(Ceremony ceremony) {
        Objects.requireNonNull(ceremony, "ceremony");
        ObjectNode record = MAPPER.createObjectNode();
        record.put(FIELD_RECORD_ID, recordIds.get());
        record.put(FIELD_PRODUCER, producerId);

        ObjectNode chain = record.putObject(FIELD_PRODUCER_CHAIN);
        chain.put(FIELD_PRODUCER_ID, producerId);
        chain.put(FIELD_PRODUCER_INSTANCE_ID, producerInstanceId);

        ObjectNode trace = record.putObject(FIELD_TRACE);
        trace.put(FIELD_EVENT_KIND, EVENT_KIND);
        trace.put(FIELD_SIGNED_AT, clock.instant().getEpochSecond());
        trace.put(FIELD_SESSION_ID, ceremony.getSessionId());
        trace.put(FIELD_SESSION_ISSUER, ceremony.getSessionIssuer());

        ObjectNode event = trace.putObject(FIELD_EVENT);
        event.put(FIELD_CEREMONY_TYPE, ceremony.getType().name());
        event.put(FIELD_CEREMONY_OUTCOME, ceremony.getOutcome().name());
        return record;
    }

    private static String requireNonBlankArg(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
