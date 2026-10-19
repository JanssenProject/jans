// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * The members Lock selects a key by, read from strictly parsed bytes. Duplicate members were
 * rejected, so any other JSON reader sees the same values.
 */
public final class AssertionIdentity {

    private final String producer;
    private final String kid;
    private final String recordId;

    AssertionIdentity(io.jans.trace.uniffi.AssertionIdentity inner) {
        this.producer = inner.getProducer();
        this.kid = inner.getKid();
        this.recordId = inner.getRecordId();
    }

    public String producer() {
        return producer;
    }

    public String kid() {
        return kid;
    }

    public String recordId() {
        return recordId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AssertionIdentity)) {
            return false;
        }
        AssertionIdentity that = (AssertionIdentity) other;
        return producer.equals(that.producer) && kid.equals(that.kid) && recordId.equals(that.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(producer, kid, recordId);
    }

    /** Public identifiers only. */
    @Override
    public String toString() {
        return "AssertionIdentity[producer=" + producer + ", kid=" + kid + ", recordId=" + recordId + "]";
    }
}
