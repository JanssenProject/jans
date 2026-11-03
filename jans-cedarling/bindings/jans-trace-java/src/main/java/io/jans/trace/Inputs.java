// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

import io.jans.trace.uniffi.Jans_trace_uniffiKt;

/** Hashes of decision inputs that are not the record itself: the policy store and tokens. */
public final class Inputs {

    private Inputs() {
    }

    /** {@code bundle_hash} of a policy store: SHA-256 of the {@code .cjar} exactly as received. */
    public static Digest computeBundleHash(byte[] archive) {
        Objects.requireNonNull(archive, "archive");
        return new Digest(Jans_trace_uniffiKt.computeBundleHash(archive));
    }

    /**
     * Identity of a token without {@code jti}: SHA-256 of the UTF-8 text of the token exactly as
     * received. Every producer uses this function, so {@code (issuer, fingerprint)} correlates.
     */
    public static Digest tokenFingerprint(String token) {
        Objects.requireNonNull(token, "token");
        return new Digest(Jans_trace_uniffiKt.tokenFingerprint(token));
    }
}
