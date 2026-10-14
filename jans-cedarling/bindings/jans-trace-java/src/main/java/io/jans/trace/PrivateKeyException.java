// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/** Thrown when an {@link Ed25519Signer} cannot be built; the code is always {@link ErrorCode#INVALID_PRIVATE_KEY}. The message never contains key bytes. */
public final class PrivateKeyException extends TraceException {

    private static final long serialVersionUID = 1L;

    PrivateKeyException(ErrorCode code, String message) {
        super(code, message);
    }
}
