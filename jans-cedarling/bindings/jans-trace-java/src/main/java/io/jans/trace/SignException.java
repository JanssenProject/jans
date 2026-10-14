// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/** Thrown by {@link UnsignedAssertion#sign}: {@link ErrorCode#KID_MISMATCH} or {@link ErrorCode#SIGNER_FAILURE}. */
public final class SignException extends TraceException {

    private static final long serialVersionUID = 1L;

    SignException(ErrorCode code, String message) {
        super(code, message);
    }
}
