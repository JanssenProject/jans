// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * Thrown by {@link SelfTest#run}; the code is always {@link ErrorCode#SELF_TEST_FAILED} and the
 * message names the broken algorithms.
 */
public final class SelfTestException extends TraceException {

    private static final long serialVersionUID = 1L;

    SelfTestException(ErrorCode code, String message) {
        super(code, message);
    }
}
