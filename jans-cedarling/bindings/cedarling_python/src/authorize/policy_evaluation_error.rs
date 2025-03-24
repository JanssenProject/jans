/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// Represents an error that occurred when evaluating a Cedar policy.
#[derive(Debug, Clone)]
#[pyclass(get_all, module = "cedarling_python._cedarling_python")]
pub struct PolicyEvaluationError {
    /// Id of the policy with an error
    id: String,
    /// Underlying evaluation error string representation
    error: String,
}

impl From<cedarling::bindings::PolicyEvaluationError> for PolicyEvaluationError {
    fn from(value: cedarling::bindings::PolicyEvaluationError) -> Self {
        Self {
            id: value.id,
            error: value.error,
        }
    }
}
