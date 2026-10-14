// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jans_trace_core::json::JsonError;
use thiserror::Error;

use crate::limits::Limits;

/// JSON text as received. A record rather than a bare string, so a JSON argument cannot be
/// confused with an id or a key; `UniFFI` converts it to UTF-8, so no charset mistake is possible.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct JsonBody {
    pub value: String,
}

/// Returns the RFC 8785 canonical text of `input` after strict validation.
#[uniffi::export]
pub fn canonicalize_json(input: &JsonBody, limits: &Limits) -> Result<String, CanonicalizeError> {
    jans_trace_core::json::canonicalize_json(&input.value, &limits.0).map_err(Into::into)
}

/// Variants are named after the core error codes.
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum CanonicalizeError {
    #[error(transparent)]
    InvalidJson(jans_trace_core::json::CanonicalizeError),
    #[error(transparent)]
    DuplicateMember(jans_trace_core::json::CanonicalizeError),
    #[error(transparent)]
    NonCanonicalizableJson(jans_trace_core::json::CanonicalizeError),
    #[error(transparent)]
    SizeLimitExceeded(jans_trace_core::json::CanonicalizeError),
}

impl From<jans_trace_core::json::CanonicalizeError> for CanonicalizeError {
    fn from(error: jans_trace_core::json::CanonicalizeError) -> Self {
        use jans_trace_core::json::CanonicalizeError as Core;
        match &error {
            Core::SizeLimitExceeded(_) | Core::Json(JsonError::LimitExceeded(_)) => {
                Self::SizeLimitExceeded(error)
            },
            Core::Json(JsonError::Invalid { .. }) => Self::InvalidJson(error),
            Core::Json(JsonError::DuplicateMember) => Self::DuplicateMember(error),
            Core::Json(JsonError::NonCanonicalizable(_)) => Self::NonCanonicalizableJson(error),
        }
    }
}
