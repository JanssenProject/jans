/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::authorize_result_response::AuthorizeResultResponse;
use pyo3::prelude::*;

/// MultiIssuerAuthorizeResult
/// ==========================
///
/// A Python wrapper for the Rust `cedarling::MultiIssuerAuthorizeResult` struct.
/// Represents the result of a multi-issuer authorization request.
///
/// Methods
/// -------
/// .. method:: is_allowed(self) -> bool
///     Returns whether the request is allowed.
///
/// .. method:: response(self) -> AuthorizeResultResponse
///     Returns the detailed Cedar policy response.
///
/// .. method:: request_id(self) -> str
///     Returns the unique request ID for this authorization.
///
#[pyclass]
pub struct MultiIssuerAuthorizeResult {
    inner: cedarling::MultiIssuerAuthorizeResult,
}

#[pymethods]
impl MultiIssuerAuthorizeResult {
    /// Returns true if request is allowed
    fn is_allowed(&self) -> bool {
        self.inner.decision
    }

    /// Get the Cedar policy response
    fn response(&self) -> AuthorizeResultResponse {
        self.inner.response.clone().into()
    }

    /// Get the request ID associated with this result
    fn request_id(&self) -> String {
        self.inner.request_id.clone()
    }
}

impl From<cedarling::MultiIssuerAuthorizeResult> for MultiIssuerAuthorizeResult {
    fn from(value: cedarling::MultiIssuerAuthorizeResult) -> Self {
        Self { inner: value }
    }
}

