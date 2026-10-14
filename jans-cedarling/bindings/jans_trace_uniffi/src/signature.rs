// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;
use std::sync::Arc;

use jans_trace_core::signature::TraceSigner as _;
use thiserror::Error;
use zeroize::Zeroizing;

/// In-memory Ed25519 signer for keys from a mounted secret. The Rust copy of the secret is
/// zeroized on drop; the host owns its own copies (Java strings and arrays).
#[derive(Debug, uniffi::Object)]
pub struct Ed25519Signer(pub(crate) jans_trace_core::signature::Ed25519Signer);

#[uniffi::export]
impl Ed25519Signer {
    /// Loads the unencrypted PKCS#8 PEM that `openssl genpkey -algorithm ed25519` writes.
    #[uniffi::constructor]
    pub fn from_pkcs8_pem(kid: String, pem: &str) -> Result<Arc<Self>, PrivateKeyError> {
        jans_trace_core::signature::Ed25519Signer::from_pkcs8_pem(kid, pem)
            .map(|signer| Arc::new(Self(signer)))
            .map_err(|e| PrivateKeyError::InvalidPrivateKey(PrivateKeyCause::Core(e)))
    }

    /// Raw 32-byte secret.
    #[uniffi::constructor]
    #[allow(
        clippy::needless_pass_by_value,
        reason = "taken by value so the secret is zeroized here, not left in a UniFFI buffer"
    )]
    pub fn from_secret_bytes(kid: String, secret: Vec<u8>) -> Result<Arc<Self>, PrivateKeyError> {
        let secret = Zeroizing::new(secret);
        let bytes: Zeroizing<[u8; 32]> =
            Zeroizing::new(secret.as_slice().try_into().map_err(|_| {
                PrivateKeyError::InvalidPrivateKey(PrivateKeyCause::WrongLength(secret.len()))
            })?);
        Ok(Arc::new(Self(
            jans_trace_core::signature::Ed25519Signer::from_secret_bytes(kid, &bytes),
        )))
    }

    /// The `kid` Lock selects the verification key by; must equal the assertion `kid`.
    pub fn kid(&self) -> String {
        self.0.kid().to_owned()
    }

    pub fn public_key_bytes(&self) -> Vec<u8> {
        self.0.public_key_bytes().to_vec()
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum PrivateKeyError {
    #[error(transparent)]
    InvalidPrivateKey(PrivateKeyCause),
}

/// The core reason, or a secret the core never sees because it has the wrong length.
#[derive(Debug, Error)]
pub enum PrivateKeyCause {
    #[error(transparent)]
    Core(jans_trace_core::signature::PrivateKeyError),
    #[error("private key is not a 32-byte Ed25519 secret: {0} bytes")]
    WrongLength(usize),
}

/// A 64-byte Ed25519 signature as carried in the top-level `signature` member.
#[derive(Debug, PartialEq, Eq, uniffi::Object)]
#[uniffi::export(Display, Eq)]
pub struct SignatureValue(pub(crate) jans_trace_core::signature::SignatureValue);

#[uniffi::export]
impl SignatureValue {
    /// Accepts only unpadded base64url of exactly 64 bytes, so one signature has one text form.
    #[uniffi::constructor]
    pub fn parse(text: &str) -> Result<Arc<Self>, ParseSignatureError> {
        text.parse()
            .map(|signature| Arc::new(Self(signature)))
            .map_err(ParseSignatureError::InvalidSignatureEncoding)
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        self.0.as_bytes().to_vec()
    }
}

impl fmt::Display for SignatureValue {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.0.fmt(f)
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ParseSignatureError {
    #[error(transparent)]
    InvalidSignatureEncoding(jans_trace_core::signature::ParseSignatureError),
}
