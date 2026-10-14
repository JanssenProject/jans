// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use crate::digest::Digest;
use crate::error_code::{JwkError, PublicKeyError};
use crate::json::JsonBody;

/// An Ed25519 key that can verify producer signatures.
#[derive(Debug, PartialEq, Eq, uniffi::Object)]
#[uniffi::export(Eq)]
pub struct PublicKey(pub(crate) jans_trace_core::key::PublicKey);

#[uniffi::export]
impl PublicKey {
    /// Parses an OKP Ed25519 JWK (RFC 8037). Rejects other key types, a padded or
    /// wrong-length `x`, a JWK with `d` and small-order keys; other members are ignored.
    #[uniffi::constructor]
    pub fn from_jwk(jwk: &JsonBody) -> Result<Arc<Self>, Arc<JwkError>> {
        jans_trace_core::key::PublicKey::from_jwk(&jwk.value)
            .map(|key| Arc::new(Self(key)))
            .map_err(JwkError::wrap)
    }

    /// Raw 32-byte key; refuses small-order keys.
    #[uniffi::constructor]
    #[allow(
        clippy::needless_pass_by_value,
        reason = "UniFFI maps `Vec<u8>` to `byte[]`; `&[u8]` would become a `java.nio.ByteBuffer`"
    )]
    pub fn from_bytes(bytes: Vec<u8>) -> Result<Arc<Self>, Arc<PublicKeyError>> {
        jans_trace_core::key::PublicKey::from_bytes(&bytes)
            .map(|key| Arc::new(Self(key)))
            .map_err(PublicKeyError::wrap)
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        self.0.to_bytes().to_vec()
    }

    /// RFC 7638 thumbprint as `sha256:<hex>`.
    pub fn thumbprint(&self) -> Arc<Digest> {
        Arc::new(Digest(self.0.thumbprint()))
    }
}
