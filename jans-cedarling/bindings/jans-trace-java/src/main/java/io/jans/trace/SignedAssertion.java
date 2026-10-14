// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * A record with a well-formed top-level {@code signature}, not yet verified. Parsed once:
 * identity, signature, signing input and {@code content_digest} are known afterwards. Immutable.
 */
public final class SignedAssertion {

    private final io.jans.trace.uniffi.SignedAssertion inner;

    SignedAssertion(io.jans.trace.uniffi.SignedAssertion inner) {
        this.inner = inner;
    }

    /**
     * Parses one untrusted record. For a bulk request body use {@link Batch#parse}, never a JSON
     * library: it would merge duplicate members and lose the record's original text.
     */
    public static SignedAssertion parse(JsonBody assertion, Limits limits) throws ParseSignedAssertionException {
        Objects.requireNonNull(assertion, "assertion");
        Objects.requireNonNull(limits, "limits");
        try {
            return new SignedAssertion(
                    io.jans.trace.uniffi.SignedAssertion.Companion.parse(assertion.inner(), limits.inner()));
        } catch (io.jans.trace.uniffi.ParseSignedAssertionException e) {
            throw new ParseSignedAssertionException(Errors.code(e), e.getMessage());
        }
    }

    /**
     * The record exactly as received, or as {@link UnsignedAssertion#sign} produced it. Store
     * this text; never serialize the record again from a parsed tree.
     */
    public String text() {
        return inner.text();
    }

    public AssertionIdentity identity() {
        return new AssertionIdentity(inner.identity());
    }

    public SignatureValue signature() {
        return new SignatureValue(inner.signature());
    }

    /** JCS of the record without its top-level {@code signature}: what the producer signed. */
    public String signingInput() {
        return inner.signingInput();
    }

    /** SHA-256 of JCS of the complete record, signature included. */
    public Digest contentDigest() {
        return new Digest(inner.contentDigest());
    }

    /** Checks producer and {@code kid} first, then the Ed25519 signature. */
    public VerifiedAssertion verify(VerificationContext context) throws VerifyException {
        Objects.requireNonNull(context, "context");
        try {
            return new VerifiedAssertion(inner.verify(context.inner()));
        } catch (io.jans.trace.uniffi.VerifyException e) {
            throw new VerifyException(Errors.code(e), e.getMessage());
        }
    }

    io.jans.trace.uniffi.SignedAssertion inner() {
        return inner;
    }

    /** Public identifiers only, never the record. */
    @Override
    public String toString() {
        return "SignedAssertion[" + identity() + "]";
    }
}
