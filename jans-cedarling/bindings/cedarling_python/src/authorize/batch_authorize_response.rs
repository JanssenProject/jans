/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::batch_item_result::{BatchItemMultiIssuerResult, BatchItemUnsignedResult};
use pyo3::prelude::*;

/// A Python wrapper for `cedarling::BatchAuthorizeResponse<Result<AuthorizeResult, BatchItemError>>`.
/// 
/// Carries a shared `batch_id` (UUIDv7) alongside per-item results. Each entry
/// in `results` is a `BatchItemUnsignedResult` — an `AuthorizeResult` when
/// Cedar reached a decision, or a `BatchItemError` when the item failed to
/// build. `results[i]` corresponds to the `items[i]` supplied to the request.
///
/// Attributes:
///     batch_id (str): The shared UUIDv7 identifier for this batch evaluation.
///     results (List[BatchItemUnsignedResult]): The evaluation results, in input order.
#[pyclass]
pub(crate) struct BatchAuthorizeUnsignedResponse {
    batch_id: String,
    results: Vec<Py<BatchItemUnsignedResult>>,
}

#[pymethods]
impl BatchAuthorizeUnsignedResponse {
    #[getter]
    fn batch_id(&self) -> String {
        self.batch_id.clone()
    }

    #[getter]
    fn results(&self, py: Python) -> Vec<Py<BatchItemUnsignedResult>> {
        self.results.iter().map(|r| r.clone_ref(py)).collect()
    }
}

impl BatchAuthorizeUnsignedResponse {
    pub(crate) fn from_cedarling(
        py: Python,
        value: cedarling::BatchAuthorizeResponse<
            Result<cedarling::AuthorizeResult, cedarling::BatchItemError>,
        >,
    ) -> PyResult<Self> {
        let results = value
            .results
            .into_iter()
            .map(|r| Py::new(py, BatchItemUnsignedResult::from_cedarling(py, r)?))
            .collect::<PyResult<Vec<_>>>()?;
        Ok(Self {
            batch_id: value.batch_id.to_string(),
            results,
        })
    }
}

/// Multi-issuer analog of `BatchAuthorizeUnsignedResponse`.
///
/// Attributes:
///     batch_id (str): The shared UUIDv7 identifier for this batch evaluation.
///     results (List[BatchItemMultiIssuerResult]): The evaluation results, in input order.
#[pyclass]
pub(crate) struct BatchAuthorizeMultiIssuerResponse {
    batch_id: String,
    results: Vec<Py<BatchItemMultiIssuerResult>>,
}

#[pymethods]
impl BatchAuthorizeMultiIssuerResponse {
    #[getter]
    fn batch_id(&self) -> String {
        self.batch_id.clone()
    }

    #[getter]
    fn results(&self, py: Python) -> Vec<Py<BatchItemMultiIssuerResult>> {
        self.results.iter().map(|r| r.clone_ref(py)).collect()
    }
}

impl BatchAuthorizeMultiIssuerResponse {
    pub(crate) fn from_cedarling(
        py: Python,
        value: cedarling::BatchAuthorizeResponse<
            Result<cedarling::MultiIssuerAuthorizeResult, cedarling::BatchItemError>,
        >,
    ) -> PyResult<Self> {
        let results = value
            .results
            .into_iter()
            .map(|r| Py::new(py, BatchItemMultiIssuerResult::from_cedarling(py, r)?))
            .collect::<PyResult<Vec<_>>>()?;
        Ok(Self {
            batch_id: value.batch_id.to_string(),
            results,
        })
    }
}
