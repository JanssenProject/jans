// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use crate::error_code::CanonicalizeError;
use crate::limits::Limits;

/// JSON text as received. A record rather than a bare string, so a JSON argument cannot be
/// confused with an id or a key; `UniFFI` converts it to UTF-8, so no charset mistake is possible.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct JsonBody {
    pub value: String,
}

/// Returns the RFC 8785 canonical text of `input` after strict validation.
#[uniffi::export]
pub fn canonicalize_json(
    input: &JsonBody,
    limits: &Limits,
) -> Result<String, Arc<CanonicalizeError>> {
    jans_trace_core::json::canonicalize_json(&input.value, &limits.0)
        .map_err(CanonicalizeError::wrap)
}
