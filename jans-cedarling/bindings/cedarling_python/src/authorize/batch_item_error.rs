/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// BatchItemError
/// ==============
///
/// Per-item build failure surfaced inside a batch response at
/// `results[i].error()` when Cedar couldn't be reached for that item.
///
/// Attributes
/// ----------
/// :param category: Stable variant slug (`action_parse`, `resource_build`,
///     `context_build`, `principal_build`, `schema_validation`,
///     `multi_issuer_entity`, `request_validation`).
/// :param item_index: Position of the failing item in the original `items` list.
/// :param message: Human-readable diagnostic. Safe to log.
#[pyclass(skip_from_py_object)]
#[derive(Clone, Debug)]
pub(crate) struct BatchItemError {
    inner: cedarling::BatchItemError,
}

#[pymethods]
impl BatchItemError {
    #[getter]
    fn category(&self) -> &'static str {
        self.inner.category()
    }

    #[getter]
    fn item_index(&self) -> usize {
        self.inner.item_index()
    }

    #[getter]
    fn message(&self) -> String {
        self.inner.to_string()
    }

    fn __repr__(&self) -> String {
        format!(
            "BatchItemError(category={:?}, item_index={}, message={:?})",
            self.category(),
            self.item_index(),
            self.message(),
        )
    }
}

impl From<cedarling::BatchItemError> for BatchItemError {
    fn from(inner: cedarling::BatchItemError) -> Self {
        Self { inner }
    }
}
