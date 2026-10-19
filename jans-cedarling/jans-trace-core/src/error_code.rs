// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The stable error codes (core design §10, plus the codes this crate adds).

use std::fmt;

use crate::assertion::{
    ParseBatchError, ParseSignedAssertionError, ParseUnsignedAssertionError, SignError, VerifyError,
};
use crate::json::{CanonicalizeError, JsonError};
use crate::key::{JwkError, PublicKeyError};
use crate::receipt::{
    ChainHeadError, ParseReceivedAtError, ReceiptLinkError, ReceiptPositionError,
    ReceivedAtRangeError, StoredReceiptError,
};
use crate::self_test::SelfTestError;
use crate::signature::{ParseSignatureError, PrivateKeyError, SignerError};
use crate::{ParseDigestError, SizeLimitError};

/// Every failure's stable code. Codes are part of the contract: a release never renames or
/// reuses one, so hosts may return them verbatim (Lock's per-record bulk results). Each
/// error type's `code()` returns only the codes its operation can produce.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum ErrorCode {
    /// Not valid JSON.
    InvalidJson,
    /// An object repeats a member name.
    DuplicateMember,
    /// Valid JSON outside the profile, such as a number outside +/-(2^53-1).
    NonCanonicalizableJson,
    /// Input breaks one of the `Limits`.
    SizeLimitExceeded,
    /// Not an object, or `producer` / `kid` / `record_id` missing or empty.
    InvalidAssertionShape,
    /// No top-level `signature`.
    MissingSignature,
    /// A record to sign already has a top-level `signature`.
    SignatureAlreadyPresent,
    /// `signature` is not unpadded base64url of 64 bytes.
    InvalidSignatureEncoding,
    /// Ed25519 verification failed.
    SignatureInvalid,
    /// Signed `producer` differs from the expected one.
    ProducerMismatch,
    /// Signed `kid` differs from the selected key, or the record's `kid` from the signer's.
    KidMismatch,
    /// JWK or key bytes do not describe a usable Ed25519 public key.
    InvalidPublicKey,
    /// Not a usable Ed25519 private key. Not in the core design.
    InvalidPrivateKey,
    /// Text is not `sha256:` plus 64 lowercase hex digits.
    InvalidDigest,
    /// A receipt value breaks the v1 profile.
    InvalidReceipt,
    /// A receipt does not continue the chain it is checked against.
    ReceiptLinkMismatch,
    /// The signer failed; carries no detail.
    SignerFailure,
    /// A known answer differed. Not in the core design.
    SelfTestFailed,
    /// A bulk body is valid JSON but not an array. Not in the core design.
    InvalidBatch,
}

impl ErrorCode {
    /// The stable text form, such as `INVALID_JSON`.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::InvalidJson => "INVALID_JSON",
            Self::DuplicateMember => "DUPLICATE_MEMBER",
            Self::NonCanonicalizableJson => "NON_CANONICALIZABLE_JSON",
            Self::SizeLimitExceeded => "SIZE_LIMIT_EXCEEDED",
            Self::InvalidAssertionShape => "INVALID_ASSERTION_SHAPE",
            Self::MissingSignature => "MISSING_SIGNATURE",
            Self::SignatureAlreadyPresent => "SIGNATURE_ALREADY_PRESENT",
            Self::InvalidSignatureEncoding => "INVALID_SIGNATURE_ENCODING",
            Self::SignatureInvalid => "SIGNATURE_INVALID",
            Self::ProducerMismatch => "PRODUCER_MISMATCH",
            Self::KidMismatch => "KID_MISMATCH",
            Self::InvalidPublicKey => "INVALID_PUBLIC_KEY",
            Self::InvalidPrivateKey => "INVALID_PRIVATE_KEY",
            Self::InvalidDigest => "INVALID_DIGEST",
            Self::InvalidReceipt => "INVALID_RECEIPT",
            Self::ReceiptLinkMismatch => "RECEIPT_LINK_MISMATCH",
            Self::SignerFailure => "SIGNER_FAILURE",
            Self::SelfTestFailed => "SELF_TEST_FAILED",
            Self::InvalidBatch => "INVALID_BATCH",
        }
    }
}

impl fmt::Display for ErrorCode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}

/// Gives an error its stable code. Every implementation lives in this file, so every code the
/// crate can produce, and which error produces it, is in one place; each error's public
/// `code()` calls its implementation. Crate-private: callers use `code()` and need no import.
pub(crate) trait HasErrorCode {
    fn error_code(&self) -> ErrorCode;
}

impl HasErrorCode for ParseDigestError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidDigest
    }
}

impl HasErrorCode for SizeLimitError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::SizeLimitExceeded
    }
}

impl HasErrorCode for JsonError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::Invalid { .. } => ErrorCode::InvalidJson,
            Self::DuplicateMember => ErrorCode::DuplicateMember,
            Self::NonCanonicalizable(_) => ErrorCode::NonCanonicalizableJson,
            Self::LimitExceeded(e) => e.code(),
        }
    }
}

impl HasErrorCode for CanonicalizeError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
        }
    }
}

impl HasErrorCode for PublicKeyError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidPublicKey
    }
}

impl HasErrorCode for JwkError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::Json(e) => e.code(),
            Self::InvalidPublicKey(e) => e.code(),
        }
    }
}

impl HasErrorCode for ParseSignatureError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidSignatureEncoding
    }
}

impl HasErrorCode for SignerError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::SignerFailure
    }
}

impl HasErrorCode for PrivateKeyError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidPrivateKey
    }
}

impl HasErrorCode for ParseUnsignedAssertionError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject
            | Self::InvalidProducer
            | Self::InvalidKid
            | Self::InvalidRecordId => ErrorCode::InvalidAssertionShape,
            Self::SignatureAlreadyPresent => ErrorCode::SignatureAlreadyPresent,
        }
    }
}

impl HasErrorCode for SignError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::KidMismatch { .. } => ErrorCode::KidMismatch,
            Self::Signer(e) => e.code(),
        }
    }
}

impl HasErrorCode for ParseSignedAssertionError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnObject
            | Self::InvalidProducer
            | Self::InvalidKid
            | Self::InvalidRecordId => ErrorCode::InvalidAssertionShape,
            Self::MissingSignature => ErrorCode::MissingSignature,
            Self::InvalidSignatureEncoding(e) => e.code(),
        }
    }
}

impl HasErrorCode for VerifyError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::ProducerMismatch { .. } => ErrorCode::ProducerMismatch,
            Self::KidMismatch { .. } => ErrorCode::KidMismatch,
            Self::SignatureInvalid => ErrorCode::SignatureInvalid,
        }
    }
}

impl HasErrorCode for ParseBatchError {
    fn error_code(&self) -> ErrorCode {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnArray => ErrorCode::InvalidBatch,
        }
    }
}

impl HasErrorCode for ParseReceivedAtError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidReceipt
    }
}

impl HasErrorCode for ReceivedAtRangeError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidReceipt
    }
}

impl HasErrorCode for ReceiptPositionError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidReceipt
    }
}

impl HasErrorCode for ChainHeadError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidReceipt
    }
}

impl HasErrorCode for StoredReceiptError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::InvalidReceipt
    }
}

impl HasErrorCode for ReceiptLinkError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::ReceiptLinkMismatch
    }
}

impl HasErrorCode for SelfTestError {
    fn error_code(&self) -> ErrorCode {
        ErrorCode::SelfTestFailed
    }
}

#[cfg(test)]
mod tests {
    use std::collections::BTreeSet;

    use super::ErrorCode;

    /// The contract: a release never renames or reuses one of these strings.
    const STABLE: [(ErrorCode, &str); 19] = [
        (ErrorCode::InvalidJson, "INVALID_JSON"),
        (ErrorCode::DuplicateMember, "DUPLICATE_MEMBER"),
        (
            ErrorCode::NonCanonicalizableJson,
            "NON_CANONICALIZABLE_JSON",
        ),
        (ErrorCode::SizeLimitExceeded, "SIZE_LIMIT_EXCEEDED"),
        (ErrorCode::InvalidAssertionShape, "INVALID_ASSERTION_SHAPE"),
        (ErrorCode::MissingSignature, "MISSING_SIGNATURE"),
        (
            ErrorCode::SignatureAlreadyPresent,
            "SIGNATURE_ALREADY_PRESENT",
        ),
        (
            ErrorCode::InvalidSignatureEncoding,
            "INVALID_SIGNATURE_ENCODING",
        ),
        (ErrorCode::SignatureInvalid, "SIGNATURE_INVALID"),
        (ErrorCode::ProducerMismatch, "PRODUCER_MISMATCH"),
        (ErrorCode::KidMismatch, "KID_MISMATCH"),
        (ErrorCode::InvalidPublicKey, "INVALID_PUBLIC_KEY"),
        (ErrorCode::InvalidPrivateKey, "INVALID_PRIVATE_KEY"),
        (ErrorCode::InvalidDigest, "INVALID_DIGEST"),
        (ErrorCode::InvalidReceipt, "INVALID_RECEIPT"),
        (ErrorCode::ReceiptLinkMismatch, "RECEIPT_LINK_MISMATCH"),
        (ErrorCode::SignerFailure, "SIGNER_FAILURE"),
        (ErrorCode::SelfTestFailed, "SELF_TEST_FAILED"),
        (ErrorCode::InvalidBatch, "INVALID_BATCH"),
    ];

    #[test]
    fn every_code_has_its_stable_string() {
        for (code, text) in STABLE {
            assert_eq!(code.as_str(), text, "{code:?} is written as {text}");
            assert_eq!(code.to_string(), text, "Display is the stable string");
        }
    }

    #[test]
    fn readme_documents_exactly_these_codes() {
        let documented: BTreeSet<&str> = include_str!("../README.md")
            .lines()
            .filter_map(|line| line.strip_prefix("| `")?.split_once("` |"))
            .map(|(code, _)| code)
            .filter(|code| code.bytes().all(|b| b.is_ascii_uppercase() || b == b'_'))
            .collect();
        let stable: BTreeSet<&str> = STABLE.iter().map(|(_, text)| *text).collect();
        assert_eq!(
            documented, stable,
            "the README error table lists every code once"
        );
    }
}
