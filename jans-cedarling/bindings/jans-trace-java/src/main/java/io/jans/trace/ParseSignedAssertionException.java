// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** Thrown by {@link SignedAssertion#parse} and {@link BatchRecord#get}: {@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED}, {@link ErrorCode#INVALID_ASSERTION_SHAPE}, {@link ErrorCode#MISSING_SIGNATURE} or {@link ErrorCode#INVALID_SIGNATURE_ENCODING}. */
public final class ParseSignedAssertionException extends TraceException {

    private static final long serialVersionUID = 1L;

    ParseSignedAssertionException(ErrorCode code, String message) {
        super(code, message);
    }
}
