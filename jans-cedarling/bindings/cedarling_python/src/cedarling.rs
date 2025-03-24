/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::LogStorage;
use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

use crate::authorize::authorize_result::AuthorizeResult;
use crate::authorize::errors::authorize_error_to_py;
use crate::authorize::request::Request;
use crate::config::bootstrap_config::BootstrapConfig;
use serde_pyobject::to_pyobject;

/// A Python wrapper for the Rust `cedarling::Cedarling` struct.
/// Represents an instance of the Cedarling application, a local authorization service
/// that answers authorization questions based on JWT tokens.
#[derive(Clone)]
#[pyclass(module = "cedarling_python._cedarling_python")]
pub struct Cedarling {
    inner: cedarling::blocking::Cedarling,
}

#[pymethods]
impl Cedarling {
    #[new]
    fn new(config: &BootstrapConfig) -> PyResult<Self> {
        let inner = cedarling::blocking::Cedarling::new(config.inner())
            .map_err(|err| PyValueError::new_err(err.to_string()))?;
        Ok(Self { inner })
    }

    /// Authorize request
    fn authorize(&self, request: Bound<'_, Request>) -> Result<AuthorizeResult, PyErr> {
        let cedarling_instance = self
            .inner
            .authorize(request.borrow().to_cedarling()?)
            .map_err(authorize_error_to_py)?;
        Ok(cedarling_instance.into())
    }

    /// Return logs and remove them from the storage
    fn pop_logs(&self) -> PyResult<Vec<PyObject>> {
        let logs = self.inner.pop_logs();
        Python::with_gil(|py| -> PyResult<Vec<PyObject>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<PyObject>>>()
        })
    }

    /// Get specific log entry
    fn get_log_by_id(&self, id: &str) -> PyResult<Option<PyObject>> {
        // It doesn't follow a functional approach because handling all types properly challenging
        // and this code easy to read
        if let Some(entry) = self.inner.get_log_by_id(id) {
            let py_obj =
                Python::with_gil(|py| -> PyResult<PyObject> { log_entry_to_py(py, &entry) })?;
            Ok(Some(py_obj))
        } else {
            Ok(None)
        }
    }

    /// Returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String> {
        self.inner.get_log_ids()
    }

    /// Returns a list of log entries by tag.
    /// Tag can be `log_kind`, `log_level`.
    fn get_logs_by_tag(&self, tag: &str) -> PyResult<Vec<PyObject>> {
        let logs = self.inner.get_logs_by_tag(tag);

        Python::with_gil(|py| -> PyResult<Vec<PyObject>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<PyObject>>>()
        })
    }

    /// Returns a list of log entries by request id.
    fn get_logs_by_request_id(&self, request_id: &str) -> PyResult<Vec<PyObject>> {
        let logs = self.inner.get_logs_by_request_id(request_id);
        Python::with_gil(|py| -> PyResult<Vec<PyObject>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<PyObject>>>()
        })
    }

    /// Returns a list of all log entries by request id and tag.
    /// Tag can be `log_kind`, `log_level`.
    fn get_logs_by_request_id_and_tag(&self, id: &str, tag: &str) -> PyResult<Vec<PyObject>> {
        let logs = self.inner.get_logs_by_request_id_and_tag(id, tag);

        Python::with_gil(|py| -> PyResult<Vec<PyObject>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<PyObject>>>()
        })
    }
}

fn log_entry_to_py(gil: Python, entry: &serde_json::Value) -> PyResult<PyObject> {
    to_pyobject(gil, entry)
        .map(|v| v.unbind())
        .map_err(|err| err.0)
}
