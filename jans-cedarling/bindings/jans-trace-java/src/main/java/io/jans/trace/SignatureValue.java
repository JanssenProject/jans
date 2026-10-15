// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Arrays;
import java.util.Objects;
import io.jans.trace.TraceException.ParseSignatureException;

/** A 64-byte Ed25519 signature as carried in the top-level {@code signature} member. Immutable. */
public final class SignatureValue {

    private final io.jans.trace.uniffi.SignatureValue inner;

    SignatureValue(io.jans.trace.uniffi.SignatureValue inner) {
        this.inner = inner;
    }

    /** Accepts only unpadded base64url of exactly 64 bytes, so one signature has one text form. */
    public static SignatureValue parse(String text) throws ParseSignatureException {
        Objects.requireNonNull(text, "text");
        try {
            return new SignatureValue(io.jans.trace.uniffi.SignatureValue.Companion.parse(text));
        } catch (io.jans.trace.uniffi.ParseSignatureException e) {
            throw new ParseSignatureException(e.code(), e.toString());
        }
    }

    public byte[] toBytes() {
        return inner.toBytes();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SignatureValue && inner.equals(((SignatureValue) other).inner);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBytes());
    }

    /** The unpadded base64url form. */
    @Override
    public String toString() {
        return inner.toString();
    }
}
