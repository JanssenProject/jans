// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Arrays;
import java.util.Objects;
import io.jans.trace.TraceException.JwkException;
import io.jans.trace.TraceException.PublicKeyException;

/** An Ed25519 key that verifies producer signatures. Immutable; parse once at registration. */
public final class PublicKey {

    private final io.jans.trace.uniffi.PublicKey inner;
    private final byte[] bytes;

    private PublicKey(io.jans.trace.uniffi.PublicKey inner) {
        this.inner = inner;
        this.bytes = inner.toBytes();
    }

    /**
     * Parses an OKP Ed25519 JWK (RFC 8037), as Lock's registry stores it. Rejects other key types,
     * a padded or wrong-length {@code x}, a JWK with {@code d} (a private key) and small-order
     * keys; {@code kid}, {@code use} and {@code alg} are ignored.
     */
    public static PublicKey fromJwk(JsonBody jwk) throws JwkException {
        Objects.requireNonNull(jwk, "jwk");
        try {
            return new PublicKey(io.jans.trace.uniffi.PublicKey.Companion.fromJwk(jwk.inner()));
        } catch (io.jans.trace.uniffi.JwkException e) {
            throw new JwkException(e.code(), e.toString());
        }
    }

    /** Raw 32-byte key; small-order keys are rejected. */
    public static PublicKey fromBytes(byte[] bytes) throws PublicKeyException {
        Objects.requireNonNull(bytes, "bytes");
        try {
            return new PublicKey(io.jans.trace.uniffi.PublicKey.Companion.fromBytes(bytes));
        } catch (io.jans.trace.uniffi.PublicKeyException e) {
            throw new PublicKeyException(e.code(), e.toString());
        }
    }

    public byte[] toBytes() {
        return bytes.clone();
    }

    /** RFC 7638 thumbprint as {@code sha256:<hex>}, the form Lock stores as {@code key_thumbprint}. */
    public Digest thumbprint() {
        return new Digest(inner.thumbprint());
    }

    io.jans.trace.uniffi.PublicKey inner() {
        return inner;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PublicKey && Arrays.equals(bytes, ((PublicKey) other).bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "PublicKey[" + thumbprint() + "]";
    }
}
