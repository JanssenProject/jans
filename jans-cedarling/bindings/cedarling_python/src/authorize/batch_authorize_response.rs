/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::authorize_result::AuthorizeResult;
use super::multi_issuer_authorize_result::MultiIssuerAuthorizeResult;
use pyo3::prelude::*;

/// BatchAuthorizeUnsignedResponse
/// ==============================
///
/// A Python wrapper for `cedarling::BatchAuthorizeResponse<AuthorizeResult>`.
/// Carries a shared `batch_id` (UUIDv7) alongside per-item results.
/// `results[i]` corresponds to the `items[i]` supplied to the request.
///
/// Attributes
/// ----------
/// :param batch_id: Shared UUIDv7 correlation id stamped on every per-item decision log entry.
/// :param results: Per-item `AuthorizeResult` list in input order (`results[i]` maps to `items[i]`).
#[pyclass]
pub(crate) struct BatchAuthorizeUnsignedResponse {
    batch_id: String,
    results: Vec<Py<AuthorizeResult>>,
}

#[pymethods]
impl BatchAuthorizeUnsignedResponse {
    /// Shared correlation id stamped on every per-item decision log entry.
    #[getter]
    fn batch_id(&self) -> String {
        self.batch_id.clone()
    }

    /// Per-item results in input order.
    #[getter]
    fn results(&self, py: Python) -> Vec<Py<AuthorizeResult>> {
        self.results.iter().map(|r| r.clone_ref(py)).collect()
    }
}

impl BatchAuthorizeUnsignedResponse {
    pub(crate) fn from_cedarling(
        py: Python,
        value: cedarling::BatchAuthorizeResponse<cedarling::AuthorizeResult>,
    ) -> PyResult<Self> {
        let results = value
            .results
            .into_iter()
            .map(|r| Py::new(py, AuthorizeResult::from(r)))
            .collect::<PyResult<Vec<_>>>()?;
        Ok(Self {
            batch_id: value.batch_id.to_string(),
            results,
        })
    }
}

/// BatchAuthorizeMultiIssuerResponse
/// =================================
///
/// A Python wrapper for `cedarling::BatchAuthorizeResponse<MultiIssuerAuthorizeResult>`.
/// Carries a shared `batch_id` (UUIDv7) alongside per-item results.
/// `results[i]` corresponds to the `items[i]` supplied to the request.
///
/// Attributes
/// ----------
/// :param batch_id: Shared UUIDv7 correlation id stamped on every per-item decision log entry.
/// :param results: Per-item `MultiIssuerAuthorizeResult` list in input order (`results[i]` maps to `items[i]`).
#[pyclass]
pub(crate) struct BatchAuthorizeMultiIssuerResponse {
    batch_id: String,
    results: Vec<Py<MultiIssuerAuthorizeResult>>,
}

#[pymethods]
impl BatchAuthorizeMultiIssuerResponse {
    /// Shared correlation id stamped on every per-item decision log entry.
    #[getter]
    fn batch_id(&self) -> String {
        self.batch_id.clone()
    }

    /// Per-item results in input order.
    #[getter]
    fn results(&self, py: Python) -> Vec<Py<MultiIssuerAuthorizeResult>> {
        self.results.iter().map(|r| r.clone_ref(py)).collect()
    }
}

impl BatchAuthorizeMultiIssuerResponse {
    pub(crate) fn from_cedarling(
        py: Python,
        value: cedarling::BatchAuthorizeResponse<cedarling::MultiIssuerAuthorizeResult>,
    ) -> PyResult<Self> {
        let results = value
            .results
            .into_iter()
            .map(|r| Py::new(py, MultiIssuerAuthorizeResult::from(r)))
            .collect::<PyResult<Vec<_>>>()?;
        Ok(Self {
            batch_id: value.batch_id.to_string(),
            results,
        })
    }
}
