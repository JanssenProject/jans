/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

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
///     str: stable reason slug (e.g. ``"jwt_validation_failed"``).
/// .. attribute:: detail
///     str: claim-free detail for ``invalid_input``; empty otherwise.
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
    reason: String,
    #[pyo3(get)]
    detail: String,
    #[pyo3(get)]
    message: String,
}

#[pymethods]
impl DroppedToken {
    fn __repr__(&self) -> String {
        format!(
            "DroppedToken(mapping='{}', index={}, reason='{}')",
            self.mapping, self.index, self.reason
        )
    }
}

impl From<cedarling::DroppedToken> for DroppedToken {
    fn from(d: cedarling::DroppedToken) -> Self {
        let detail = match &d.reason {
            cedarling::DropReason::InvalidInput(e) => e.to_string(),
            _ => String::new(),
        };
        let message = d.reason.message();
        Self {
            mapping: d.mapping,
            index: d.index,
            reason: d.reason.slug().to_string(),
            detail,
            message,
        }
    }
}
