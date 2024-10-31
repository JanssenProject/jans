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
/// .. method:: workload(self) -> AuthorizeResultResponse
///     Returns the detailed response as an `AuthorizeResultResponse` object.
///
#[pyclass]
pub struct AuthorizeResult {
    inner: cedarling::AuthorizeResult,
}

#[pymethods]
impl AuthorizeResult {
    /// Returns true if request is allowed
    fn is_allowed(&self) -> bool {
        self.inner.is_allowed()
    }

    /// Get the decision value for workload
    fn workload(&self) -> AuthorizeResultResponse {
        self.inner.workload.clone().into()
    }
}

impl From<cedarling::AuthorizeResult> for AuthorizeResult {
    fn from(value: cedarling::AuthorizeResult) -> Self {
        Self { inner: value }
    }
}
