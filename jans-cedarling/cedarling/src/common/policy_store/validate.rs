// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::Serialize;

/// Result of validating a policy store, one field per level.
#[derive(Debug, Clone, Serialize)]
pub struct ValidationReport {
    /// The result of parsing the policy store.
    pub parse: LevelResult,
    /// The result of schema validation.
    pub schema: LevelResult,
    /// The result of metadata validation.
    pub metadata: LevelResult,
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
        self.parse.error_count()
            + self.schema.error_count()
            + self.metadata.error_count()
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
