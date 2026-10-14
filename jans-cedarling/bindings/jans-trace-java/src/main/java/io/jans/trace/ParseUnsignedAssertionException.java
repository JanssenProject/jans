// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/** Thrown by {@link UnsignedAssertion#parse}: {@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED}, {@link ErrorCode#INVALID_ASSERTION_SHAPE} or {@link ErrorCode#SIGNATURE_ALREADY_PRESENT}. */
public final class ParseUnsignedAssertionException extends TraceException {

    private static final long serialVersionUID = 1L;

    ParseUnsignedAssertionException(ErrorCode code, String message) {
        super(code, message);
    }
}
