// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;
import io.jans.trace.TraceException.ParseUnsignedAssertionException;
import io.jans.trace.TraceException.SignException;

/**
 * A producer's record before signing: strict JSON, an object with non-empty {@code producer},
 * {@code kid} and {@code record_id}, and no top-level {@code signature}. Immutable.
 */
public final class UnsignedAssertion {

    private final io.jans.trace.uniffi.UnsignedAssertion inner;

    private UnsignedAssertion(io.jans.trace.uniffi.UnsignedAssertion inner) {
        this.inner = inner;
    }

    /**
     * Also checks that the signed record will fit {@code limits}: use the limits Lock enforces,
     * so a record Lock would refuse for its size fails here, before it is signed.
     */
    public static UnsignedAssertion parse(JsonBody assertion, Limits limits) throws ParseUnsignedAssertionException {
        Objects.requireNonNull(assertion, "assertion");
        Objects.requireNonNull(limits, "limits");
        try {
            return new UnsignedAssertion(
                    io.jans.trace.uniffi.UnsignedAssertion.Companion.parse(assertion.inner(), limits.inner()));
        } catch (io.jans.trace.uniffi.ParseUnsignedAssertionException e) {
            throw new ParseUnsignedAssertionException(e.code(), e.toString());
        }
    }

    public AssertionIdentity identity() {
        return new AssertionIdentity(inner.identity());
    }

    /** The text the signature covers: JCS of the record. */
    public String signingInput() {
        return inner.signingInput();
    }

    /**
     * Signs the record. The result's {@link SignedAssertion#text()} is the JCS form of the whole
     * record, signature included; there is nothing to insert by hand.
     */
    public SignedAssertion sign(Ed25519Signer signer) throws SignException {
        Objects.requireNonNull(signer, "signer");
        try {
            return new SignedAssertion(inner.sign(signer.inner()));
        } catch (io.jans.trace.uniffi.SignException e) {
            throw new SignException(e.code(), e.toString());
        }
    }
}
