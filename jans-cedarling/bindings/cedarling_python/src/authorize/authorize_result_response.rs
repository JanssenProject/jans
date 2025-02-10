/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use super::decision::Decision;
use super::diagnostics::Diagnostics;
use cedarling::bindings::cedar_policy;
use pyo3::prelude::*;

/// AuthorizeResultResponse
/// =======================
///
/// A Python wrapper for the Rust `cedar_policy::Response` struct.
/// Represents the result of an authorization request.
///
/// Attributes
/// ----------
/// :param decision: The authorization decision (wrapped `Decision` object).
/// :param diagnostics: Additional information on the decision (wrapped `Diagnostics` object).
#[pyclass]
pub struct AuthorizeResultResponse {
    inner: cedar_policy::Response,
}

#[pymethods]
impl AuthorizeResultResponse {
    /// Authorization decision
    #[getter]
    fn decision(&self) -> Decision {
        self.inner.decision().into()
    }

    /// Diagnostics providing more information on how this decision was reached
    #[getter]
    fn diagnostics(&self) -> Diagnostics {
        self.inner.diagnostics().into()
    }
}

impl From<cedar_policy::Response> for AuthorizeResultResponse {
    fn from(value: cedar_policy::Response) -> Self {
        Self { inner: value }
    }
}
