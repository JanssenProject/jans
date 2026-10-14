// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * In-memory Ed25519 signer for a producer key from a mounted secret. The native copy of the
 * secret is zeroized when the signer is freed; the caller owns its own copies, such as the PEM
 * string or the secret array.
 */
public final class Ed25519Signer {

    private final io.jans.trace.uniffi.Ed25519Signer inner;

    private Ed25519Signer(io.jans.trace.uniffi.Ed25519Signer inner) {
        this.inner = inner;
    }

    /**
     * Loads the unencrypted PKCS#8 PEM that {@code openssl genpkey -algorithm ed25519} writes.
     * Takes the text, not a path: reading the file is the host's job.
     */
    public static Ed25519Signer fromPkcs8Pem(String kid, String pem) throws PrivateKeyException {
        Objects.requireNonNull(kid, "kid");
        Objects.requireNonNull(pem, "pem");
        try {
            return new Ed25519Signer(io.jans.trace.uniffi.Ed25519Signer.Companion.fromPkcs8Pem(kid, pem));
        } catch (io.jans.trace.uniffi.PrivateKeyException e) {
            throw new PrivateKeyException(e.code(), e.toString());
        }
    }

    /** Raw 32-byte secret; prefer {@link #fromPkcs8Pem} for keys on disk. */
    public static Ed25519Signer fromSecretBytes(String kid, byte[] secret) throws PrivateKeyException {
        Objects.requireNonNull(kid, "kid");
        Objects.requireNonNull(secret, "secret");
        try {
            return new Ed25519Signer(io.jans.trace.uniffi.Ed25519Signer.Companion.fromSecretBytes(kid, secret));
        } catch (io.jans.trace.uniffi.PrivateKeyException e) {
            throw new PrivateKeyException(e.code(), e.toString());
        }
    }

    /** The {@code kid} Lock selects the verification key by; must equal the record's {@code kid}. */
    public String kid() {
        return inner.kid();
    }

    /** Register these bytes (as a JWK) in Lock; see {@link PublicKey#fromBytes}. */
    public byte[] publicKeyBytes() {
        return inner.publicKeyBytes();
    }

    io.jans.trace.uniffi.Ed25519Signer inner() {
        return inner;
    }

    /** Only the {@code kid}: never key material. */
    @Override
    public String toString() {
        return "Ed25519Signer[kid=" + kid() + "]";
    }
}
