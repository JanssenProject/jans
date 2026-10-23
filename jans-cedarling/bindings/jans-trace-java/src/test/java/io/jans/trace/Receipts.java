// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * Receipt fixtures. Hashes were computed independently with Python: SHA-256 of
 * {@code json.dumps(sort_keys=True)} over the eight v1 fields, not with this library. Test-only.
 */
final class Receipts {

    static final String DOMAIN = "domain-1";
    static final long FIRST_MILLIS = 1_789_676_132_123L;
    static final String FIRST_RECEIVED_AT = "2026-09-17T20:15:32.123Z";
    static final String FIRST_HASH = "sha256:b9dc11581b85c4548d915b34b253a419d20cd55fa6afd68d48784969225ab568";

    static final long SECOND_MILLIS = 1_789_676_133_000L;
    static final String SECOND_RECORD_ID = "record-2";
    static final String SECOND_CONTENT_DIGEST = "sha256:" + "ab".repeat(32);
    static final String SECOND_HASH = "sha256:c0edb428c7a6800f2c6e57564fd2c3aee02a2a382716894f98cc81183118fec4";

    private Receipts() {
    }

    static VerifiedAssertion verified() throws TraceException {
        return Records.signed().verify(new VerificationContext(Records.PRODUCER, Records.KID, Records.publicKey()));
    }

    /** The first receipt of {@link #DOMAIN}, for the {@link Records} record. */
    static ReceiptCommitment first() throws TraceException {
        return ReceiptCommitment.forAssertion(DOMAIN, ReceiptPosition.genesis(),
                ReceivedAt.fromUnixMillis(FIRST_MILLIS), verified());
    }

    /** The second receipt, rebuilt from stored values as an audit does. */
    static ReceiptCommitment second() throws TraceException {
        return ReceiptCommitment.fromStored(StoredReceipt.builder()
                .evidenceDomainId(DOMAIN)
                .position(ReceiptPosition.of(2, Digest.parse(FIRST_HASH)))
                .receivedAt(ReceivedAt.fromUnixMillis(SECOND_MILLIS))
                .producerId(Records.PRODUCER)
                .recordId(SECOND_RECORD_ID)
                .contentDigest(Digest.parse(SECOND_CONTENT_DIGEST))
                .build());
    }
}
