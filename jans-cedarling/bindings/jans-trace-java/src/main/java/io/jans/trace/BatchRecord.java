// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * One element of a bulk body: the parsed record, or why it was rejected. Lock answers every
 * element on its own, so a bad record never fails the others.
 */
public final class BatchRecord {

    private final SignedAssertion assertion;
    private final ErrorCode code;
    private final String message;

    private BatchRecord(SignedAssertion assertion, ErrorCode code, String message) {
        this.assertion = assertion;
        this.code = code;
        this.message = message;
    }

    static BatchRecord of(io.jans.trace.uniffi.BatchRecord inner) {
        try {
            return new BatchRecord(new SignedAssertion(inner.assertion()), null, null);
        } catch (io.jans.trace.uniffi.ParseSignedAssertionException e) {
            return new BatchRecord(null, Errors.code(e), e.getMessage());
        }
    }

    public boolean isParsed() {
        return assertion != null;
    }

    /** The parsed record; throws this record's rejection, a new exception on every call. */
    public SignedAssertion get() throws ParseSignedAssertionException {
        if (assertion == null) {
            throw new ParseSignedAssertionException(code, message);
        }
        return assertion;
    }
}
