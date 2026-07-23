/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

use super::authorize_result::AuthorizeResult;
use super::batch_item_error::BatchItemError;
use super::multi_issuer_authorize_result::MultiIssuerAuthorizeResult;

/// BatchItemUnsignedResult
/// =======================
///
/// One slot in a batch unsigned response's `results` list. Callers switch on
/// `is_ok()` — on `True`, call `unwrap()` for the `AuthorizeResult`; on `False`,
/// read `.error` for the `BatchItemError`.
#[pyclass]
pub(crate) struct BatchItemUnsignedResult {
    ok: Option<Py<AuthorizeResult>>,
    err: Option<Py<BatchItemError>>,
}

#[pymethods]
impl BatchItemUnsignedResult {
    /// `True` when Cedar evaluated this item (Allow or Deny); `False` when the
    /// item failed to build.
    fn is_ok(&self) -> bool {
        self.ok.is_some()
    }

    /// The `AuthorizeResult` if `is_ok()`; raises `RuntimeError` otherwise.
    fn unwrap(&self, py: Python) -> PyResult<Py<AuthorizeResult>> {
        self.ok
            .as_ref()
            .map(|o| o.clone_ref(py))
            .ok_or_else(|| PyValueError::new_err("BatchItemUnsignedResult is Err"))
    }

    /// The per-item `BatchItemError` if `!is_ok()`; `None` otherwise.
    #[getter]
    fn error(&self, py: Python) -> Option<Py<BatchItemError>> {
        self.err.as_ref().map(|e| e.clone_ref(py))
    }
}

impl BatchItemUnsignedResult {
    pub(crate) fn from_cedarling(
        py: Python,
        value: Result<cedarling::AuthorizeResult, cedarling::BatchItemError>,
    ) -> PyResult<Self> {
        match value {
            Ok(r) => Ok(Self {
                ok: Some(Py::new(py, AuthorizeResult::from(r))?),
                err: None,
            }),
            Err(e) => Ok(Self {
                ok: None,
                err: Some(Py::new(py, BatchItemError::from(e))?),
            }),
        }
    }
}

/// BatchItemMultiIssuerResult
/// ==========================
///
/// Multi-issuer analog of `BatchItemUnsignedResult`.
#[pyclass]
pub(crate) struct BatchItemMultiIssuerResult {
    ok: Option<Py<MultiIssuerAuthorizeResult>>,
    err: Option<Py<BatchItemError>>,
}

#[pymethods]
impl BatchItemMultiIssuerResult {
    fn is_ok(&self) -> bool {
        self.ok.is_some()
    }

    fn unwrap(&self, py: Python) -> PyResult<Py<MultiIssuerAuthorizeResult>> {
        self.ok
            .as_ref()
            .map(|o| o.clone_ref(py))
            .ok_or_else(|| PyValueError::new_err("BatchItemMultiIssuerResult is Err"))
    }

    #[getter]
    fn error(&self, py: Python) -> Option<Py<BatchItemError>> {
        self.err.as_ref().map(|e| e.clone_ref(py))
    }
}

impl BatchItemMultiIssuerResult {
    pub(crate) fn from_cedarling(
        py: Python,
        value: Result<cedarling::MultiIssuerAuthorizeResult, cedarling::BatchItemError>,
    ) -> PyResult<Self> {
        match value {
            Ok(r) => Ok(Self {
                ok: Some(Py::new(py, MultiIssuerAuthorizeResult::from(r))?),
                err: None,
            }),
            Err(e) => Ok(Self {
                ok: None,
                err: Some(Py::new(py, BatchItemError::from(e))?),
            }),
        }
    }
}
