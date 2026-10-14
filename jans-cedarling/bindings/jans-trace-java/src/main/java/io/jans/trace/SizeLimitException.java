// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** Thrown by {@link Batch#body} when the batch is above the limits; the code is always {@link ErrorCode#SIZE_LIMIT_EXCEEDED}. */
public final class SizeLimitException extends TraceException {

    private static final long serialVersionUID = 1L;

    SizeLimitException(ErrorCode code, String message) {
        super(code, message);
    }
}
