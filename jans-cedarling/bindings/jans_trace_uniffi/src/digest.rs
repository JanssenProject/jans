// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;
use std::sync::Arc;

use thiserror::Error;

/// SHA-256 digest in the TRACE text form `sha256:<64 lowercase hex>`.
#[derive(Debug, PartialEq, Eq, Hash, uniffi::Object)]
#[uniffi::export(Display, Eq, Hash)]
pub struct Digest(pub(crate) jans_trace_core::Digest);

#[uniffi::export]
impl Digest {
    /// Strict parse: anything but `sha256:` and 64 lowercase hex digits is rejected.
    #[uniffi::constructor]
    pub fn parse(text: &str) -> Result<Arc<Self>, ParseDigestError> {
        text.parse()
            .map(|digest| Arc::new(Self(digest)))
            .map_err(ParseDigestError::InvalidDigest)
    }

    /// The all-zero sentinel that precedes the first link of a producer or receipt chain.
    #[uniffi::constructor]
    pub fn genesis() -> Arc<Self> {
        Arc::new(Self(jans_trace_core::Digest::GENESIS))
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        self.0.as_bytes().to_vec()
    }
}

impl fmt::Display for Digest {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.0.fmt(f)
    }
}

#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum ParseDigestError {
    #[error(transparent)]
    InvalidDigest(jans_trace_core::ParseDigestError),
}
