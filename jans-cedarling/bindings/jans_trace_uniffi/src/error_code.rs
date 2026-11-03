// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Every error of the binding, in one place like the core's codes. Each operation keeps its
//! own error type; every type carries the core's `ErrorCode`, so the code list exists once.

use std::sync::Arc;

pub(crate) use jans_trace_core::ErrorCode;
use thiserror::Error;

/// The core's codes. Listed again only because UniFFI needs the variants of a remote type;
/// the compiler rejects this list if it misses a core variant.
#[uniffi::remote(Enum)]
pub enum ErrorCode {
    InvalidJson,
    DuplicateMember,
    NonCanonicalizableJson,
    SizeLimitExceeded,
    InvalidAssertionShape,
    MissingSignature,
    SignatureAlreadyPresent,
    InvalidSignatureEncoding,
    SignatureInvalid,
    ProducerMismatch,
    KidMismatch,
    InvalidPublicKey,
    InvalidPrivateKey,
    InvalidDigest,
    InvalidReceipt,
    ReceiptLinkMismatch,
    SignerFailure,
    SelfTestFailed,
    InvalidBatch,
}

/// Declares one error type per operation. Each carries the core's stable code and its
/// `Display` text, which never contains keys, signatures or record content.
macro_rules! errors {
    ($($name:ident => $core:ty;)*) => {$(
        #[derive(Debug, Error, uniffi::Object)]
        #[uniffi::export(Debug, Display)]
        #[error("{detail}")]
        pub struct $name {
            code: ErrorCode,
            detail: String,
        }

        #[uniffi::export]
        impl $name {
            pub fn code(&self) -> ErrorCode {
                self.code
            }
        }

        impl $name {
            #[allow(clippy::needless_pass_by_value, reason = "used as `map_err(Self::wrap)`")]
            pub(crate) fn wrap(error: $core) -> Arc<Self> {
                Arc::new(Self {
                    code: error.code(),
                    detail: error.to_string(),
                })
            }
        }
    )*};
}

errors! {
    ParseDigestError => jans_trace_core::ParseDigestError;
    SizeLimitError => jans_trace_core::SizeLimitError;
    CanonicalizeError => jans_trace_core::json::CanonicalizeError;
    PublicKeyError => jans_trace_core::key::PublicKeyError;
    JwkError => jans_trace_core::key::JwkError;
    ParseSignatureError => jans_trace_core::signature::ParseSignatureError;
    PrivateKeyError => jans_trace_core::signature::PrivateKeyError;
    ParseUnsignedAssertionError => jans_trace_core::assertion::ParseUnsignedAssertionError;
    SignError => jans_trace_core::assertion::SignError;
    ParseSignedAssertionError => jans_trace_core::assertion::ParseSignedAssertionError;
    VerifyError => jans_trace_core::assertion::VerifyError;
    ParseBatchError => jans_trace_core::assertion::ParseBatchError;
    ReceivedAtRangeError => jans_trace_core::receipt::ReceivedAtRangeError;
    ParseReceivedAtError => jans_trace_core::receipt::ParseReceivedAtError;
    ReceiptPositionError => jans_trace_core::receipt::ReceiptPositionError;
    StoredReceiptError => jans_trace_core::receipt::StoredReceiptError;
    ChainHeadError => jans_trace_core::receipt::ChainHeadError;
    ReceiptLinkError => jans_trace_core::receipt::ReceiptLinkError;
    SelfTestError => jans_trace_core::self_test::SelfTestError;
}
