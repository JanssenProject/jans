// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * A receipt as Lock stored it, read back for an audit; nothing here is verified yet. Built
 * with named fields, so the three ids cannot be swapped by position.
 */
public final class StoredReceipt {

    private final String evidenceDomainId;
    private final ReceiptPosition position;
    private final ReceivedAt receivedAt;
    private final String producerId;
    private final String recordId;
    private final Digest contentDigest;

    private StoredReceipt(Builder builder) {
        this.evidenceDomainId = builder.evidenceDomainId;
        this.position = builder.position;
        this.receivedAt = builder.receivedAt;
        this.producerId = builder.producerId;
        this.recordId = builder.recordId;
        this.contentDigest = builder.contentDigest;
    }

    public static Builder builder() {
        return new Builder();
    }

    io.jans.trace.uniffi.StoredReceipt inner() {
        return new io.jans.trace.uniffi.StoredReceipt(evidenceDomainId, position.inner(), receivedAt.inner(),
                producerId, recordId, contentDigest.inner());
    }

    /** Every field is required; {@link #build} names the first one missing. */
    public static final class Builder {

        private String evidenceDomainId;
        private ReceiptPosition position;
        private ReceivedAt receivedAt;
        private String producerId;
        private String recordId;
        private Digest contentDigest;

        private Builder() {
        }

        public Builder evidenceDomainId(String value) {
            this.evidenceDomainId = Objects.requireNonNull(value, "evidenceDomainId");
            return this;
        }

        public Builder position(ReceiptPosition value) {
            this.position = Objects.requireNonNull(value, "position");
            return this;
        }

        public Builder receivedAt(ReceivedAt value) {
            this.receivedAt = Objects.requireNonNull(value, "receivedAt");
            return this;
        }

        public Builder producerId(String value) {
            this.producerId = Objects.requireNonNull(value, "producerId");
            return this;
        }

        public Builder recordId(String value) {
            this.recordId = Objects.requireNonNull(value, "recordId");
            return this;
        }

        public Builder contentDigest(Digest value) {
            this.contentDigest = Objects.requireNonNull(value, "contentDigest");
            return this;
        }

        public StoredReceipt build() {
            require(evidenceDomainId, "evidenceDomainId");
            require(position, "position");
            require(receivedAt, "receivedAt");
            require(producerId, "producerId");
            require(recordId, "recordId");
            require(contentDigest, "contentDigest");
            return new StoredReceipt(this);
        }

        private static void require(Object value, String name) {
            if (value == null) {
                throw new IllegalStateException("StoredReceipt is missing " + name);
            }
        }
    }
}
