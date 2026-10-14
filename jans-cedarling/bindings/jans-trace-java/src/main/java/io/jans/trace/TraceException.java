// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/**
 * Common parent of every failure this library reports. Each operation throws its own subclass,
 * which carries only the codes that operation can produce; catch this type to handle them all.
 * Messages explain the cause and never contain keys, signatures or record content.
 */
public abstract class TraceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    TraceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * The stable code, for the wire and for logs (Lock returns it verbatim in per-record bulk
     * results). Handle a failure by its exception type, which says what the operation can
     * produce; the code namespace is shared by the whole library.
     */
    public ErrorCode code() {
        return code;
    }
}
