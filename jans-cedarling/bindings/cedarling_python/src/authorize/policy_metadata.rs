/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use pyo3::prelude::*;

use super::policy_effect::PolicyEffect;

/// PolicyMetadata
/// ==============
///
/// Metadata about a Cedar policy, including its ID, effect, annotations, and source code.
///
/// Attributes
/// ----------
/// :param id: The policy ID string.
/// :param effect: The policy effect as a PolicyEffect enum value.
/// :param annotations: Dictionary of policy annotations.
/// :param source: The Cedar policy source code.
#[pyclass]
pub struct PolicyMetadata {
    inner: cedarling::PolicyMetadata,
}

#[pymethods]
impl PolicyMetadata {
    /// The policy ID.
    #[getter]
    fn id(&self) -> &str {
        &self.inner.id
    }

    /// The policy effect as a PolicyEffect enum.
    #[getter]
    fn effect(&self) -> PolicyEffect {
        self.inner.effect.into()
    }

    /// Key-value pairs from Cedar policy annotations.
    #[getter]
    fn annotations(&self) -> HashMap<String, String> {
        self.inner.annotations.clone()
    }

    /// The Cedar policy source code.
    #[getter]
    fn source(&self) -> &str {
        &self.inner.source
    }

    fn __repr__(&self) -> String {
        let effect_repr = match self.inner.effect {
            cedarling::PolicyEffect::Permit => "PolicyEffect.Permit",
            cedarling::PolicyEffect::Forbid => "PolicyEffect.Forbid",
        };
        format!(
            "PolicyMetadata(id='{}', effect={})",
            self.inner.id, effect_repr
        )
    }

    fn __str__(&self) -> String {
        self.__repr__()
    }
}

impl From<cedarling::PolicyMetadata> for PolicyMetadata {
    fn from(value: cedarling::PolicyMetadata) -> Self {
        Self { inner: value }
    }
}
