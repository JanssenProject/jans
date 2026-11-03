// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * The registered key Lock selected by the record's identity, with the {@code producer} and
 * {@code kid} it is registered under. The signed members must equal them.
 */
public final class VerificationContext {

    private final String expectedProducer;
    private final String expectedKid;
    private final PublicKey publicKey;

    public VerificationContext(String expectedProducer, String expectedKid, PublicKey publicKey) {
        this.expectedProducer = Objects.requireNonNull(expectedProducer, "expectedProducer");
        this.expectedKid = Objects.requireNonNull(expectedKid, "expectedKid");
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
    }

    public String expectedProducer() {
        return expectedProducer;
    }

    public String expectedKid() {
        return expectedKid;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    io.jans.trace.uniffi.VerificationContext inner() {
        return new io.jans.trace.uniffi.VerificationContext(expectedProducer, expectedKid, publicKey.inner());
    }
}
