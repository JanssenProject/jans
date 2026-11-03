// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * A record whose signature verified under the expected key. Only
 * {@link SignedAssertion#verify} creates one, so holding it proves the check happened; Lock
 * builds receipts from it.
 */
public final class VerifiedAssertion {

    private final io.jans.trace.uniffi.VerifiedAssertion inner;

    VerifiedAssertion(io.jans.trace.uniffi.VerifiedAssertion inner) {
        this.inner = inner;
    }

    public AssertionIdentity identity() {
        return new AssertionIdentity(inner.identity());
    }

    public Digest contentDigest() {
        return new Digest(inner.contentDigest());
    }

    /** SHA-256 of the exact bytes the signature covers; lets tooling compare signing inputs. */
    public Digest signatureInputSha256() {
        return new Digest(inner.signatureInputSha256());
    }

    io.jans.trace.uniffi.VerifiedAssertion inner() {
        return inner;
    }

    @Override
    public String toString() {
        return "VerifiedAssertion[" + identity() + "]";
    }
}
