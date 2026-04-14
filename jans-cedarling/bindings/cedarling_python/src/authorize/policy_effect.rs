/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;

/// PolicyEffect
/// ============
///
/// Represents the effect of a Cedar policy.
///
/// Values
/// ------
/// - Permit: The policy permits the request.
/// - Forbid: The policy forbids the request.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[pyclass]
#[pyo3(name = "PolicyEffect")]
pub enum PolicyEffect {
    /// The policy permits the request.
    Permit,
    /// The policy forbids the request.
    Forbid,
}

#[pymethods]
impl PolicyEffect {
    /// Get the string representation of the effect
    fn __str__(&self) -> &'static str {
        match self {
            PolicyEffect::Permit => "permit",
            PolicyEffect::Forbid => "forbid",
        }
    }

    /// Compare two PolicyEffect values for equality
    fn __eq__(&self, other: &Self) -> bool {
        self == other
    }

    /// Get the detailed type representation
    fn __repr__(&self) -> String {
        let variant_name = match self {
            PolicyEffect::Permit => "Permit",
            PolicyEffect::Forbid => "Forbid",
        };
        format!("PolicyEffect.{}", variant_name)
    }
}

impl From<cedarling::PolicyEffect> for PolicyEffect {
    fn from(value: cedarling::PolicyEffect) -> Self {
        match value {
            cedarling::PolicyEffect::Permit => PolicyEffect::Permit,
            cedarling::PolicyEffect::Forbid => PolicyEffect::Forbid,
        }
    }
}
