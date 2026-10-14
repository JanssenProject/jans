// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

import io.jans.trace.uniffi.Jans_trace_uniffiKt;

/** RFC 8785 canonicalization under the strict JSON rules every operation of this library applies. */
public final class Json {

    private Json() {
    }

    /** Returns the RFC 8785 canonical text of {@code input} after strict validation. */
    public static String canonicalize(JsonBody input, Limits limits) throws CanonicalizeException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(limits, "limits");
        try {
            return Jans_trace_uniffiKt.canonicalizeJson(input.inner(), limits.inner());
        } catch (io.jans.trace.uniffi.CanonicalizeException e) {
            throw new CanonicalizeException(Errors.code(e), e.getMessage());
        }
    }
}
