// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/** Thrown by {@link Digest#parse}; the code is always {@link ErrorCode#INVALID_DIGEST}. */
public final class ParseDigestException extends TraceException {

    private static final long serialVersionUID = 1L;

    ParseDigestException(ErrorCode code, String message) {
        super(code, message);
    }
}
