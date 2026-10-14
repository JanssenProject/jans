// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use thiserror::Error;

use crate::digest::Digest;
use crate::json::{JsonBody, json_variant};

/// An Ed25519 key that can verify producer signatures.
#[derive(Debug, PartialEq, Eq, uniffi::Object)]
#[uniffi::export(Eq)]
pub struct PublicKey(pub(crate) jans_trace_core::key::PublicKey);

#[uniffi::export]
impl PublicKey {
    /// Parses an OKP Ed25519 JWK (RFC 8037). Rejects other key types, a padded or
    /// wrong-length `x`, a JWK with `d` and small-order keys; other members are ignored.
    #[uniffi::constructor]
    pub fn from_jwk(jwk: &JsonBody) -> Result<Arc<Self>, JwkError> {
        jans_trace_core::key::PublicKey::from_jwk(&jwk.value)
            .map(|key| Arc::new(Self(key)))
            .map_err(Into::into)
    }

    /// Raw 32-byte key; refuses small-order keys.
    #[uniffi::constructor]
    #[allow(
        clippy::needless_pass_by_value,
        reason = "UniFFI maps `Vec<u8>` to `byte[]`; `&[u8]` would become a `java.nio.ByteBuffer`"
    )]
    pub fn from_bytes(bytes: Vec<u8>) -> Result<Arc<Self>, PublicKeyError> {
        jans_trace_core::key::PublicKey::from_bytes(&bytes)
            .map(|key| Arc::new(Self(key)))
            .map_err(PublicKeyError::InvalidPublicKey)
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        self.0.to_bytes().to_vec()
    }

    /// RFC 7638 thumbprint as `sha256:<hex>`.
    pub fn thumbprint(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.thumbprint()))
    }
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum PublicKeyError {
    #[error(transparent)]
    InvalidPublicKey(jans_trace_core::key::PublicKeyError),
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum JwkError {
    #[error(transparent)]
    InvalidJson(jans_trace_core::key::JwkError),
    #[error(transparent)]
    DuplicateMember(jans_trace_core::key::JwkError),
    #[error(transparent)]
    NonCanonicalizableJson(jans_trace_core::key::JwkError),
    #[error(transparent)]
    SizeLimitExceeded(jans_trace_core::key::JwkError),
    #[error(transparent)]
    InvalidPublicKey(jans_trace_core::key::JwkError),
}

impl From<jans_trace_core::key::JwkError> for JwkError {
    fn from(error: jans_trace_core::key::JwkError) -> Self {
        use jans_trace_core::key::JwkError as Core;
        match &error {
            Core::InvalidPublicKey(_) => Self::InvalidPublicKey(error),
            Core::Json(json) => json_variant!(json, error),
        }
    }
}
