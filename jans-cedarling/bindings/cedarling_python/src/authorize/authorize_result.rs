/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use super::authorize_result_response::AuthorizeResultResponse;
use pyo3::prelude::*;

/// A Python wrapper for the Rust `cedarling::AuthorizeResult` struct.
/// Represents the result of an authorization request.
#[pyclass(module = "cedarling_python._cedarling_python")]
pub struct AuthorizeResult {
    inner: cedarling::AuthorizeResult,
}

#[pymethods]
impl AuthorizeResult {
    /// Returns true if request is allowed
    fn is_allowed(&self) -> bool {
        self.inner.decision
    }

    /// Get the decision value for workload
    fn workload(&self) -> Option<AuthorizeResultResponse> {
        self.inner.workload.clone().map(|v| v.into())
    }

    /// Get the decision value for person/user
    fn person(&self) -> Option<AuthorizeResultResponse> {
        self.inner.person.clone().map(|v| v.into())
    }

    fn request_id(&self) -> String {
        self.inner.request_id.clone()
    }
}

impl From<cedarling::AuthorizeResult> for AuthorizeResult {
    fn from(value: cedarling::AuthorizeResult) -> Self {
        Self { inner: value }
    }
}
