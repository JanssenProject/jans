// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.Jans_trace_uniffiKt;

/** Known-answer checks a host runs at startup. */
public final class SelfTest {

    private SelfTest() {
    }

    /**
     * Checks JCS, SHA-256, Ed25519 and the receipt hash on the machine and native library that
     * actually run. If it throws, or the native library does not load, disable TRACE (fail
     * closed).
     */
    public static void run() throws SelfTestException {
        try {
            Jans_trace_uniffiKt.selfTest();
        } catch (io.jans.trace.uniffi.SelfTestException e) {
            throw new SelfTestException(Errors.code(e), e.getMessage());
        }
    }
}
