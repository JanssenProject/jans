// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** Thrown by {@link SignedAssertion#verify}: {@link ErrorCode#PRODUCER_MISMATCH}, {@link ErrorCode#KID_MISMATCH} or {@link ErrorCode#SIGNATURE_INVALID}. */
public final class VerifyException extends TraceException {

    private static final long serialVersionUID = 1L;

    VerifyException(ErrorCode code, String message) {
        super(code, message);
    }
}
