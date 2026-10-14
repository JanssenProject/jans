// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/** Thrown by {@link ReceiptCommitment#verifyLink} and {@link ChainHead#append}; the code is always {@link ErrorCode#RECEIPT_LINK_MISMATCH}. */
public final class ReceiptLinkException extends TraceException {

    private static final long serialVersionUID = 1L;

    ReceiptLinkException(ErrorCode code, String message) {
        super(code, message);
    }
}
