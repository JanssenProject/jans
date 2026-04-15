/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use super::authorize_result_response::AuthorizeResultResponse;
use pyo3::prelude::*;

/// AuthorizeResult
/// ===============
///
/// A Python wrapper for the Rust `cedarling::AuthorizeResult` struct.
/// Represents the result of an authorization request.
///
/// Methods
/// -------
/// .. method:: is_allowed(self) -> bool
///     Returns whether the request is allowed.
///
#[pyclass]
pub struct AuthorizeResult {
    inner: cedarling::AuthorizeResult,
}

#[pymethods]
impl AuthorizeResult {
    /// Returns true if request is allowed
    fn is_allowed(&self) -> bool {
        self.inner.decision
    }

    /// Returns the underlying cedar_policy `Response` for this request.
    #[getter]
    fn response(&self) -> AuthorizeResultResponse {
        self.inner.response.clone().into()
    }

    /// Get the request ID associated with this result
    fn request_id(&self) -> String {
        self.inner.request_id.clone()
    }
}

impl From<cedarling::AuthorizeResult> for AuthorizeResult {
    fn from(value: cedarling::AuthorizeResult) -> Self {
        Self { inner: value }
    }
}
