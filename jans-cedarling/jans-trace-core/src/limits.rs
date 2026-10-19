// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Caller-chosen bounds for untrusted input (core design §11, decision R16).

use crate::ErrorCode;
use crate::error_code::HasErrorCode;
use thiserror::Error;

/// Bounds applied before and during parsing, so hostile input is rejected without unbounded
/// allocation. Lock's own API limits must not exceed them.
///
/// Fields are private: a new bound arrives as a new field without breaking callers.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Limits {
    input_bytes: usize,
    depth: usize,
    object_members: usize,
    array_elements: usize,
    string_bytes: usize,
    canonical_bytes: usize,
    batch_bytes: usize,
    batch_records: usize,
}

impl Limits {
    /// For the producer's own records and for values the crate builds itself: nothing there
    /// is attacker-controlled.
    pub(crate) const UNBOUNDED: Self = Self {
        input_bytes: usize::MAX,
        depth: usize::MAX,
        object_members: usize::MAX,
        array_elements: usize::MAX,
        string_bytes: usize::MAX,
        canonical_bytes: usize::MAX,
        batch_bytes: usize::MAX,
        batch_records: usize::MAX,
    };

    #[must_use]
    pub const fn with_max_input_bytes(mut self, max_input_bytes: usize) -> Self {
        self.input_bytes = max_input_bytes;
        self
    }

    /// Nesting of arrays and objects; the outermost container is depth 1.
    #[must_use]
    pub const fn with_max_depth(mut self, max_depth: usize) -> Self {
        self.depth = max_depth;
        self
    }

    #[must_use]
    pub const fn with_max_object_members(mut self, max_object_members: usize) -> Self {
        self.object_members = max_object_members;
        self
    }

    #[must_use]
    pub const fn with_max_array_elements(mut self, max_array_elements: usize) -> Self {
        self.array_elements = max_array_elements;
        self
    }

    /// UTF-8 bytes of one string value or member name after unescaping.
    #[must_use]
    pub const fn with_max_string_bytes(mut self, max_string_bytes: usize) -> Self {
        self.string_bytes = max_string_bytes;
        self
    }

    #[must_use]
    pub const fn with_max_canonical_bytes(mut self, max_canonical_bytes: usize) -> Self {
        self.canonical_bytes = max_canonical_bytes;
        self
    }

    /// Bound on a whole bulk request body, checked before it is parsed.
    #[must_use]
    pub const fn with_max_batch_bytes(mut self, max_batch_bytes: usize) -> Self {
        self.batch_bytes = max_batch_bytes;
        self
    }

    #[must_use]
    pub const fn with_max_batch_records(mut self, max_batch_records: usize) -> Self {
        self.batch_records = max_batch_records;
        self
    }

    #[must_use]
    pub const fn max_input_bytes(&self) -> usize {
        self.input_bytes
    }

    #[must_use]
    pub const fn max_depth(&self) -> usize {
        self.depth
    }

    #[must_use]
    pub const fn max_object_members(&self) -> usize {
        self.object_members
    }

    #[must_use]
    pub const fn max_array_elements(&self) -> usize {
        self.array_elements
    }

    #[must_use]
    pub const fn max_string_bytes(&self) -> usize {
        self.string_bytes
    }

    #[must_use]
    pub const fn max_canonical_bytes(&self) -> usize {
        self.canonical_bytes
    }

    #[must_use]
    pub const fn max_batch_bytes(&self) -> usize {
        self.batch_bytes
    }

    #[must_use]
    pub const fn max_batch_records(&self) -> usize {
        self.batch_records
    }

    pub(crate) fn check_input(self, input: &str) -> Result<(), SizeLimitError> {
        if input.len() > self.input_bytes {
            return Err(SizeLimitError::new(LimitKind::InputBytes, self.input_bytes));
        }
        Ok(())
    }
}

impl Default for Limits {
    /// Decision R16: generous MVP values; Lock's record limit for the input size. Batch bounds
    /// are the values proposed to Lock (500 records, 8 MiB) until Lock confirms its own.
    fn default() -> Self {
        Self {
            input_bytes: 256 * 1024,
            depth: 32,
            object_members: 1024,
            array_elements: 1024,
            string_bytes: 64 * 1024,
            canonical_bytes: 512 * 1024,
            batch_bytes: 8 * 1024 * 1024,
            batch_records: 500,
        }
    }
}

/// Input breaks one of the caller's limits. `Display` names which one; the code is the same
/// for all of them (core design §10).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("{kind} exceeds the configured limit of {limit}")]
pub struct SizeLimitError {
    kind: LimitKind,
    limit: usize,
}

/// Which bound of `Limits` was exceeded.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub(crate) enum LimitKind {
    #[error("input size in bytes")]
    InputBytes,
    #[error("JSON nesting depth")]
    Depth,
    #[error("members in one JSON object")]
    ObjectMembers,
    #[error("elements in one JSON array")]
    ArrayElements,
    #[error("bytes in one JSON string")]
    StringBytes,
    #[error("canonical output size in bytes")]
    CanonicalBytes,
    #[error("batch body size in bytes")]
    BatchBytes,
    #[error("records in one batch")]
    BatchRecords,
}

impl SizeLimitError {
    pub(crate) const fn new(kind: LimitKind, limit: usize) -> Self {
        Self { kind, limit }
    }

    #[must_use]
    pub const fn limit(&self) -> usize {
        self.limit
    }

    #[must_use]
    pub fn code(&self) -> ErrorCode {
        HasErrorCode::error_code(self)
    }
}

#[cfg(test)]
mod tests {
    use super::{LimitKind, Limits, SizeLimitError};
    use crate::ErrorCode;

    #[test]
    fn display_names_the_limit() {
        let cases = [
            (LimitKind::InputBytes, "input size in bytes"),
            (LimitKind::Depth, "JSON nesting depth"),
            (LimitKind::ObjectMembers, "members in one JSON object"),
            (LimitKind::ArrayElements, "elements in one JSON array"),
            (LimitKind::StringBytes, "bytes in one JSON string"),
            (LimitKind::CanonicalBytes, "canonical output size in bytes"),
            (LimitKind::BatchBytes, "batch body size in bytes"),
            (LimitKind::BatchRecords, "records in one batch"),
        ];
        for (kind, what) in cases {
            assert_eq!(
                SizeLimitError::new(kind, 7).to_string(),
                format!("{what} exceeds the configured limit of 7"),
                "`{{err}}` names the limit for {kind:?}"
            );
        }
    }

    #[test]
    fn batch_defaults_are_the_values_proposed_to_lock() {
        let limits = Limits::default();
        assert_eq!(
            limits.max_batch_records(),
            500,
            "batch record count proposed in 13 §9"
        );
        assert_eq!(
            limits.max_batch_bytes(),
            8 * 1024 * 1024,
            "batch body size proposed in 13 §9"
        );
        let custom = limits.with_max_batch_records(3).with_max_batch_bytes(10);
        assert_eq!(
            (custom.max_batch_records(), custom.max_batch_bytes()),
            (3, 10),
            "both batch bounds are configurable"
        );
    }

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
            .check_input("[1]")
            .expect("input of exactly the limit is accepted");
        let err = limits
            .check_input("[10]")
            .expect_err("input above the limit is rejected");
        assert_eq!(
            err,
            SizeLimitError::new(LimitKind::InputBytes, 3),
            "error reports which limit and its configured value"
        );
    }

    #[test]
    fn code_is_stable() {
        assert_eq!(
            SizeLimitError::new(LimitKind::Depth, 1).code(),
            ErrorCode::SizeLimitExceeded,
            "stable code from core design §10"
        );
    }

    #[test]
    fn defaults_follow_decision_r16() {
        let limits = Limits::default();
        let actual = [
            limits.max_input_bytes(),
            limits.max_depth(),
            limits.max_object_members(),
            limits.max_array_elements(),
            limits.max_string_bytes(),
            limits.max_canonical_bytes(),
        ];
        assert_eq!(
            actual,
            [256 * 1024, 32, 1024, 1024, 64 * 1024, 512 * 1024],
            "defaults from decision R16 (notes 13 §5)"
        );
    }

    #[test]
    fn builders_change_only_their_own_bound() {
        let limits = Limits::default().with_max_depth(4).with_max_string_bytes(9);
        assert_eq!(limits.max_depth(), 4, "depth is set");
        assert_eq!(limits.max_string_bytes(), 9, "string bound is set");
        assert_eq!(
            limits.max_input_bytes(),
            Limits::default().max_input_bytes(),
            "other bounds keep their defaults"
        );
    }
}
