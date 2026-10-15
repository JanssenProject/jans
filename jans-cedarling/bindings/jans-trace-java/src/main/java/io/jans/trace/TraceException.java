// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import io.jans.trace.uniffi.ErrorCode;

/**
 * Common parent of every failure this library reports. Each operation throws its own subclass,
 * nested below, which carries only the codes that operation can produce; catch this type to
 * handle them all.
 * Messages explain the cause and never contain keys, signatures or record content.
 */
public abstract class TraceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    TraceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * The stable code, for the wire and for logs (Lock returns it verbatim in per-record bulk
     * results). Handle a failure by its exception type, which says what the operation can
     * produce; the code namespace is shared by the whole library.
     */
    public ErrorCode code() {
        return code;
    }

    /**
     * Thrown by {@link Json#canonicalize}: {@link ErrorCode#INVALID_JSON},
     * {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON} or
     * {@link ErrorCode#SIZE_LIMIT_EXCEEDED}.
     */
    public static final class CanonicalizeException extends TraceException {

        private static final long serialVersionUID = 1L;

        CanonicalizeException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link ChainHead#resume}; the code is always {@link ErrorCode#INVALID_RECEIPT}. */
    public static final class ChainHeadException extends TraceException {

        private static final long serialVersionUID = 1L;

        ChainHeadException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link PublicKey#fromJwk}: {@link ErrorCode#INVALID_PUBLIC_KEY} or one of the JSON codes ({@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED}). */
    public static final class JwkException extends TraceException {

        private static final long serialVersionUID = 1L;

        JwkException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link Batch#parse} when the body as a whole is rejected: {@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED} or {@link ErrorCode#INVALID_BATCH}. */
    public static final class ParseBatchException extends TraceException {

        private static final long serialVersionUID = 1L;

        ParseBatchException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link Digest#parse}; the code is always {@link ErrorCode#INVALID_DIGEST}. */
    public static final class ParseDigestException extends TraceException {

        private static final long serialVersionUID = 1L;

        ParseDigestException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link ReceivedAt#parse}; the code is always {@link ErrorCode#INVALID_RECEIPT}. */
    public static final class ParseReceivedAtException extends TraceException {

        private static final long serialVersionUID = 1L;

        ParseReceivedAtException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link SignatureValue#parse}; the code is always {@link ErrorCode#INVALID_SIGNATURE_ENCODING}. */
    public static final class ParseSignatureException extends TraceException {

        private static final long serialVersionUID = 1L;

        ParseSignatureException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link SignedAssertion#parse} and {@link BatchRecord#get}: {@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED}, {@link ErrorCode#INVALID_ASSERTION_SHAPE}, {@link ErrorCode#MISSING_SIGNATURE} or {@link ErrorCode#INVALID_SIGNATURE_ENCODING}. */
    public static final class ParseSignedAssertionException extends TraceException {

        private static final long serialVersionUID = 1L;

        ParseSignedAssertionException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link UnsignedAssertion#parse}: {@link ErrorCode#INVALID_JSON}, {@link ErrorCode#DUPLICATE_MEMBER}, {@link ErrorCode#NON_CANONICALIZABLE_JSON}, {@link ErrorCode#SIZE_LIMIT_EXCEEDED}, {@link ErrorCode#INVALID_ASSERTION_SHAPE} or {@link ErrorCode#SIGNATURE_ALREADY_PRESENT}. */
    public static final class ParseUnsignedAssertionException extends TraceException {

        private static final long serialVersionUID = 1L;

        ParseUnsignedAssertionException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown when an {@link Ed25519Signer} cannot be built; the code is always {@link ErrorCode#INVALID_PRIVATE_KEY}. The message never contains key bytes. */
    public static final class PrivateKeyException extends TraceException {

        private static final long serialVersionUID = 1L;

        PrivateKeyException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link PublicKey#fromBytes}; the code is always {@link ErrorCode#INVALID_PUBLIC_KEY}. */
    public static final class PublicKeyException extends TraceException {

        private static final long serialVersionUID = 1L;

        PublicKeyException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link ReceiptCommitment#verifyLink} and {@link ChainHead#append}; the code is always {@link ErrorCode#RECEIPT_LINK_MISMATCH}. */
    public static final class ReceiptLinkException extends TraceException {

        private static final long serialVersionUID = 1L;

        ReceiptLinkException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link ReceiptPosition#of}; the code is always {@link ErrorCode#INVALID_RECEIPT}. */
    public static final class ReceiptPositionException extends TraceException {

        private static final long serialVersionUID = 1L;

        ReceiptPositionException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link ReceivedAt#fromUnixMillis} for a reading before 1970 or after 9999; the code is always {@link ErrorCode#INVALID_RECEIPT}. */
    public static final class ReceivedAtRangeException extends TraceException {

        private static final long serialVersionUID = 1L;

        ReceivedAtRangeException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /**
     * Thrown by {@link SelfTest#run}; the code is always {@link ErrorCode#SELF_TEST_FAILED} and the
     * message names the broken algorithms.
     */
    public static final class SelfTestException extends TraceException {

        private static final long serialVersionUID = 1L;

        SelfTestException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link UnsignedAssertion#sign}: {@link ErrorCode#KID_MISMATCH} or {@link ErrorCode#SIGNER_FAILURE}. */
    public static final class SignException extends TraceException {

        private static final long serialVersionUID = 1L;

        SignException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link Batch#body} when the batch is above the limits; the code is always {@link ErrorCode#SIZE_LIMIT_EXCEEDED}. */
    public static final class SizeLimitException extends TraceException {

        private static final long serialVersionUID = 1L;

        SizeLimitException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link ReceiptCommitment#fromStored}; the code is always {@link ErrorCode#INVALID_RECEIPT}. */
    public static final class StoredReceiptException extends TraceException {

        private static final long serialVersionUID = 1L;

        StoredReceiptException(ErrorCode code, String message) {
            super(code, message);
        }
    }

    /** Thrown by {@link SignedAssertion#verify}: {@link ErrorCode#PRODUCER_MISMATCH}, {@link ErrorCode#KID_MISMATCH} or {@link ErrorCode#SIGNATURE_INVALID}. */
    public static final class VerifyException extends TraceException {

        private static final long serialVersionUID = 1L;

        VerifyException(ErrorCode code, String message) {
            super(code, message);
        }
    }
}
