// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/**
 * Stable error codes of jans-trace-core. A release never renames or reuses a code, so hosts may
 * return them verbatim, for example in Lock's per-record bulk results.
 */
public enum ErrorCode {
    INVALID_JSON,
    DUPLICATE_MEMBER,
    NON_CANONICALIZABLE_JSON,
    SIZE_LIMIT_EXCEEDED,
    INVALID_ASSERTION_SHAPE,
    MISSING_SIGNATURE,
    SIGNATURE_ALREADY_PRESENT,
    INVALID_SIGNATURE_ENCODING,
    SIGNATURE_INVALID,
    PRODUCER_MISMATCH,
    KID_MISMATCH,
    INVALID_PUBLIC_KEY,
    INVALID_PRIVATE_KEY,
    INVALID_DIGEST,
    INVALID_RECEIPT,
    RECEIPT_LINK_MISMATCH,
    SIGNER_FAILURE,
    SELF_TEST_FAILED,
    INVALID_BATCH,
}
