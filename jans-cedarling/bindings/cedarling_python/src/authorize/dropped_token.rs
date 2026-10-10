/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// DropReason
/// ==========
///
/// Why a token was dropped from a multi-issuer authorization.
/// Mirrors ``cedarling::DropReason``; exhaustive so a new core variant
/// becomes a compile error here instead of a silent gap.
///
/// Values
/// ------
///
/// - InvalidInput
/// - JwtValidationFailed
/// - DuplicateToken
/// - DuplicateMapping
/// - NoProcessorRegistered
/// - CustomProcessingFailed
/// - CustomProcessingTimedOut
/// - EntityBuildFailed
///
/// Methods
/// -------
///
/// .. method:: slug(self) -> str
///     Stable snake-case slug (e.g. ``"jwt_validation_failed"``); same
///     values as the core JSON ``kind``.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[pyclass(eq, eq_int, frozen, skip_from_py_object)]
#[pyo3(name = "DropReason")]
pub enum DropReason {
    InvalidInput,
    JwtValidationFailed,
    DuplicateToken,
    DuplicateMapping,
    NoProcessorRegistered,
    CustomProcessingFailed,
    CustomProcessingTimedOut,
    EntityBuildFailed,
}

impl From<&cedarling::DropReason> for DropReason {
    fn from(value: &cedarling::DropReason) -> Self {
        match value {
            cedarling::DropReason::InvalidInput(_) => DropReason::InvalidInput,
            cedarling::DropReason::JwtValidationFailed => DropReason::JwtValidationFailed,
            cedarling::DropReason::DuplicateToken => DropReason::DuplicateToken,
            cedarling::DropReason::DuplicateMapping => DropReason::DuplicateMapping,
            cedarling::DropReason::NoProcessorRegistered => DropReason::NoProcessorRegistered,
            cedarling::DropReason::CustomProcessingFailed => DropReason::CustomProcessingFailed,
            cedarling::DropReason::CustomProcessingTimedOut => DropReason::CustomProcessingTimedOut,
            cedarling::DropReason::EntityBuildFailed => DropReason::EntityBuildFailed,
        }
    }
}

#[pymethods]
impl DropReason {
    /// Stable snake-case slug for this reason (e.g. ``"jwt_validation_failed"``).
    fn slug(&self) -> &'static str {
        match self {
            DropReason::InvalidInput => "invalid_input",
            DropReason::JwtValidationFailed => "jwt_validation_failed",
            DropReason::DuplicateToken => "duplicate_token",
            DropReason::DuplicateMapping => "duplicate_mapping",
            DropReason::NoProcessorRegistered => "no_processor_registered",
            DropReason::CustomProcessingFailed => "custom_processing_failed",
            DropReason::CustomProcessingTimedOut => "custom_processing_timed_out",
            DropReason::EntityBuildFailed => "entity_build_failed",
        }
    }
}

/// DroppedToken
/// ============
///
/// A token the caller supplied that multi-issuer authorization did not use.
/// Identifies the entry by its input `mapping` and zero-based `index`, with a
/// claim-free `reason`.
///
/// Attributes
/// ----------
/// .. attribute:: mapping
///     str: the Cedar entity-type mapping of the dropped input.
/// .. attribute:: index
///     int: zero-based position in the request's ``tokens`` list.
/// .. attribute:: reason
///     DropReason: why the token was dropped.
/// .. attribute:: detail
///     str: stable detail slug for ``InvalidInput`` (``"empty_mapping"`` /
///     ``"empty_payload"``); empty otherwise. Same value as the core JSON.
/// .. attribute:: message
///     str: claim-free reason message from ``DropReason``'s display text;
///     non-empty for every reason.
#[pyclass]
pub struct DroppedToken {
    #[pyo3(get)]
    mapping: String,
    #[pyo3(get)]
    index: usize,
    #[pyo3(get)]
    reason: DropReason,
    #[pyo3(get)]
    detail: String,
    #[pyo3(get)]
    message: String,
}

#[pymethods]
impl DroppedToken {
    fn __repr__(&self) -> String {
        format!(
            "DroppedToken(mapping='{}', index={}, reason={:?})",
            self.mapping, self.index, self.reason
        )
    }
}

impl From<cedarling::DroppedToken> for DroppedToken {
    fn from(d: cedarling::DroppedToken) -> Self {
        let detail = d.reason.detail().unwrap_or_default().to_string();
        let message = d.reason.message();
        let reason = DropReason::from(&d.reason);
        Self {
            mapping: d.mapping,
            index: d.index,
            reason,
            detail,
            message,
        }
    }
}
