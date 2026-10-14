// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** Thrown by {@link Batch#parse} when the body as a whole is rejected: {@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED} or {@link ErrorCode#INVALID_BATCH}. */
public final class ParseBatchException extends TraceException {

    private static final long serialVersionUID = 1L;

    ParseBatchException(ErrorCode code, String message) {
        super(code, message);
    }
}
