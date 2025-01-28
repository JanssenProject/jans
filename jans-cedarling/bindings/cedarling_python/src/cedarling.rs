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

/// Cedarling
/// =========
///
/// A Python wrapper for the Rust `cedarling::Cedarling` struct.
/// Represents an instance of the Cedarling application, a local authorization service
/// that answers authorization questions based on JWT tokens.
///
/// Attributes
/// ----------
/// :param config: A `BootstrapConfig` object for initializing the Cedarling instance.
///
/// Methods
/// -------
/// .. method:: __init__(self, config)
///
///     Initializes the Cedarling instance with the provided configuration.
///
///     :param config: A `BootstrapConfig` object with startup settings.
///
/// .. method:: authorize(self, request: Request) -> AuthorizeResult
///
///     Execute authorize request
///     :param request: Request struct for authorize.
///
/// .. method:: pop_logs(self) -> List[dict]
///
///     Retrieves and removes all logs from storage.
///
///     :returns: A list of log entries as Python objects.
///
/// .. method:: get_log_by_id(self, id: str) -> dict|None
///
///     Gets a log entry by its ID.
///
///     :param id: The log entry ID.
///
/// .. method:: get_log_ids(self) -> List[str]
///
///     Retrieves all stored log IDs.
///
/// .. method:: get_logs_by_tag(self, tag: str) -> List[dict]
///
///     Retrieves all logs matching a specific tag. Tags can be 'log_kind', 'log_level' params from log entries.
///
///     :param tag: A string specifying the tag type.
///
///     :returns: A list of log entries filtered by the tag, each converted to a Python dictionary.
///
/// .. method:: get_log_by_request_id(self, id: str) -> List[dict]
///
///     Retrieves log entries associated with a specific request ID. Each log entry is converted to a Python dictionary containing fields like 'id', 'timestamp', and 'message'.
///
///     :param id: The unique identifier for the request.
///
///     :returns: A list of dictionaries, each representing a log entry related to the specified request ID.
///
/// .. method:: get_logs_by_request_id_and_tag(self, id: str, tag: str) -> List[dict]
///
///     Retrieves all logs associated with a specific request ID and tag. The tag can be 'log_kind', 'log_level' params from log entries.
///
///     :param id: The request ID as a string.
///
///     :param tag: The tag type as a string.
///
///     :returns: A list of log entries matching both the request ID and tag, each converted to a Python dictionary.
///
#[derive(Clone)]
#[pyclass]
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
    fn get_log_by_request_id(&self, request_id: &str) -> PyResult<Vec<PyObject>> {
        let logs = self.inner.get_log_by_request_id(request_id);
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
