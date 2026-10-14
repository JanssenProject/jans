// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * Thrown by {@link Json#canonicalize}: {@link ErrorCode#INVALID_JSON},
 * {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON} or
 * {@link ErrorCode#SIZE_LIMIT_EXCEEDED}.
 */
public final class CanonicalizeException extends TraceException {

    private static final long serialVersionUID = 1L;

    CanonicalizeException(ErrorCode code, String message) {
        super(code, message);
    }
}
