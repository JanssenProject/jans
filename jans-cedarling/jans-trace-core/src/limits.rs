// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Caller-chosen bounds for untrusted input (core design §11).

use thiserror::Error;

/// Lock's per-record limit (design, Operational Limits). Lock's own API limit must not exceed
/// the library limit, so equal is the largest safe default.
const DEFAULT_MAX_INPUT_BYTES: usize = 256 * 1024;

/// Bounds applied before parsing, so oversized input is rejected without allocation.
///
/// Fields are private: later bounds (depth, member count, ...) arrive as new fields
/// without breaking callers.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Limits {
    max_input_bytes: usize,
}

impl Limits {
    #[must_use]
    pub const fn with_max_input_bytes(mut self, max_input_bytes: usize) -> Self {
        self.max_input_bytes = max_input_bytes;
        self
    }

    #[must_use]
    pub const fn max_input_bytes(&self) -> usize {
        self.max_input_bytes
    }

    pub(crate) fn check_input(self, input: &[u8]) -> Result<(), SizeLimitError> {
        if input.len() > self.max_input_bytes {
            return Err(SizeLimitError {
                limit: self.max_input_bytes,
            });
        }
        Ok(())
    }
}

impl Default for Limits {
    fn default() -> Self {
        Self {
            max_input_bytes: DEFAULT_MAX_INPUT_BYTES,
        }
    }
}

/// Input is larger than the caller's limit.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("input exceeds the {limit}-byte limit")]
pub struct SizeLimitError {
    limit: usize,
}

impl SizeLimitError {
    #[must_use]
    pub const fn limit(&self) -> usize {
        self.limit
    }

    #[must_use]
    pub const fn code(&self) -> &'static str {
        "SIZE_LIMIT_EXCEEDED"
    }
}

#[cfg(test)]
mod tests {
    use super::{Limits, SizeLimitError};

    #[test]
    fn default_matches_lock_record_limit() {
        assert_eq!(
            Limits::default().max_input_bytes(),
            256 * 1024,
            "Lock rejects records above 256 KiB, so the library default must not be smaller"
        );
    }

    #[test]
    fn accepts_input_at_the_limit_and_rejects_above_it() {
        let limits = Limits::default().with_max_input_bytes(3);
        limits
            .check_input(b"[1]")
            .expect("input of exactly the limit is accepted");
        let err = limits
            .check_input(b"[10]")
            .expect_err("input above the limit is rejected");
        assert_eq!(
            err,
            SizeLimitError { limit: 3 },
            "error reports the configured limit"
        );
    }

    #[test]
    fn code_is_stable() {
        assert_eq!(
            SizeLimitError { limit: 1 }.code(),
            "SIZE_LIMIT_EXCEEDED",
            "stable code from core design §10"
        );
    }
}
