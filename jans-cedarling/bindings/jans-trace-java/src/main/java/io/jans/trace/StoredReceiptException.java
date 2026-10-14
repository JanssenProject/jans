// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** Thrown by {@link ReceiptCommitment#fromStored}; the code is always {@link ErrorCode#INVALID_RECEIPT}. */
public final class StoredReceiptException extends TraceException {

    private static final long serialVersionUID = 1L;

    StoredReceiptException(ErrorCode code, String message) {
        super(code, message);
    }
}
