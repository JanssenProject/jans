// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![cfg(feature = "tools")]

use serde::Serialize;
use strsim::levenshtein;

use super::formats::v2::TRUSTED_ISSUERS_FILE;
use super::formats::v2::trusted_issuers::UnknownField;

/// Farther names are more likely vendor extensions than typos.
const MAX_SUGGESTION_DISTANCE: usize = 2;

/// Result of validating a policy store, one field per level.
#[derive(Debug, Clone, Serialize)]
pub struct ValidationReport {
    /// The result of parsing the policy store.
    pub parse: LevelResult,
    /// The result of schema validation.
    pub schema: LevelResult,
    /// The result of metadata validation.
    pub metadata: LevelResult,
    /// Non-fatal findings, such as an outdated `policy_store_spec_version`.
    /// They do not affect [`is_ok`](Self::is_ok).
    pub warnings: Vec<Diagnostic>,
    /// Fields of `trusted-issuers.json` that were ignored: vendor extensions
    /// or typos. They do not affect [`is_ok`](Self::is_ok).
    pub unknown_fields: Vec<Diagnostic>,
}

impl ValidationReport {
    /// True when every level either passed or was skipped.
    #[must_use]
    pub fn is_ok(&self) -> bool {
        self.parse.is_ok_or_skipped()
            && self.schema.is_ok_or_skipped()
            && self.metadata.is_ok_or_skipped()
    }

    /// Total error count across all levels.
    #[must_use]
    pub fn error_count(&self) -> usize {
        self.parse.error_count() + self.schema.error_count() + self.metadata.error_count()
    }
}

/// The result of a specific validation level.
#[derive(Debug, Clone, Serialize)]
pub enum LevelResult {
    /// Validation passed successfully.
    Ok,
    /// Level was skipped (e.g., no schema present).
    Skipped {
        /// The reason the level was skipped.
        reason: String,
    },
    /// Validation failed with one or more diagnostics.
    Failed {
        /// The validation errors encountered.
        errors: Vec<Diagnostic>,
    },
}

impl LevelResult {
    /// Returns true if the validation passed or was skipped.
    #[must_use]
    pub fn is_ok_or_skipped(&self) -> bool {
        matches!(self, LevelResult::Ok | LevelResult::Skipped { .. })
    }

    /// Returns the number of errors if the validation failed.
    #[must_use]
    pub fn error_count(&self) -> usize {
        match self {
            LevelResult::Failed { errors } => errors.len(),
            _ => 0,
        }
    }
}

/// A diagnostic message indicating a validation failure.
#[derive(Debug, Clone, Serialize)]
pub struct Diagnostic {
    /// File name (e.g., `policies/allow_read.cedar`), or `<inline>` for YAML/JSON stores.
    pub file: String,
    /// 1-based line if the underlying error carried one; `None` otherwise.
    pub line: Option<usize>,
    /// 1-based column if available.
    pub column: Option<usize>,
    /// Human-readable message.
    pub message: String,
}

impl From<UnknownField> for Diagnostic {
    fn from(unknown: UnknownField) -> Self {
        let UnknownField {
            entity_type,
            field,
            known_fields,
        } = unknown;
        let suggestion = known_fields
            .iter()
            .min_by_key(|known| levenshtein(known, &field))
            .filter(|known| levenshtein(known, &field) <= MAX_SUGGESTION_DISTANCE);
        let hint = suggestion.map_or(String::new(), |known| format!("; did you mean `{known}`?"));
        Diagnostic {
            file: TRUSTED_ISSUERS_FILE.to_string(),
            line: None,
            column: None,
            message: format!("entry '{entity_type}': unknown field `{field}` is ignored{hint}"),
        }
    }
}

/// Errors that occur due to infrastructure failures (network, IO) during validation.
#[derive(Debug, thiserror::Error)]
pub enum ValidateInfraError {
    /// HTTP request failed.
    #[error("HTTP error: {0}")]
    Http(#[from] reqwest::Error),
    /// IO error occurred.
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    /// Failed to build the HTTP client.
    #[error("failed to build HTTP client: {0}")]
    HttpClientBuild(#[from] crate::http::InitializeHttpClientError),
}

#[cfg(test)]
mod tests {
    use super::*;

    fn message(field: &str) -> String {
        Diagnostic::from(UnknownField {
            entity_type: "Acme::Access_token".to_string(),
            field: field.to_string(),
            known_fields: &["issuer", "required_claims"],
        })
        .message
    }

    #[test]
    fn suggests_near_known_field() {
        assert_eq!(
            message("required_claim"),
            "entry 'Acme::Access_token': unknown field `required_claim` is ignored; did you mean \
             `required_claims`?",
            "a typo should get a suggestion"
        );
    }

    #[test]
    fn does_not_suggest_for_distant_field() {
        assert_eq!(
            message("x_vendor_feature"),
            "entry 'Acme::Access_token': unknown field `x_vendor_feature` is ignored",
            "a vendor extension should not get a suggestion"
        );
    }
}
