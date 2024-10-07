/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::LogStorage;
use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

use crate::authorize::Request;
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
/// .. method:: pop_logs(self)
///
///     Retrieves and removes all logs from storage.
///
///     :returns: A list of log entries as Python objects.
///     :rtype: List[PyObject]
///
///     :raises ValueError: If an error occurs while fetching logs.
///
/// .. method:: get_log_by_id(self, id)
///
///     Gets a log entry by its ID.
///
///     :param id: The log entry ID.
///     :type id: str
///
///     :returns: The log entry as a Python object or None if not found.
///     :rtype: Optional[PyObject]
///
///     :raises ValueError: If an error occurs while fetching the log.
///
/// .. method:: get_log_ids(self)
///
///     Retrieves all stored log IDs.
///
///     :returns: A list of log entry IDs.
///     :rtype: List[str]
///
/// .. method:: authorize(self, Request)
///
///    Evaluate Authorization Request.
///
#[derive(Clone)]
#[pyclass]
pub struct Cedarling {
    inner: cedarling::Cedarling,
}

#[pymethods]
impl Cedarling {
    #[new]
    fn new(config: BootstrapConfig) -> PyResult<Self> {
        let inner = cedarling::Cedarling::new(config.try_into()?)
            .map_err(|err| PyValueError::new_err(err.to_string()))?;
        Ok(Self { inner })
    }

    /// return logs and remove them from the storage
    fn pop_logs(&self) -> PyResult<Vec<PyObject>> {
        let logs = self.inner.pop_logs();
        Python::with_gil(|py| -> PyResult<Vec<PyObject>> {
            logs
                .iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<PyObject>>>()
        })
    }

    /// get specific log entry
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

    /// returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String> {
        self.inner.get_log_ids()
    }

    /// Evaluate Authorization Request
    /// - evaluate if authorization is granted for *client*
    //
    // this function will be finished in next issue
    pub fn authorize(&self, request: &Request) -> PyResult<()> {
        let cedarling_request: cedarling::Request = request.into();
        self.inner
            .authorize(&cedarling_request)
            .map_err(|err| PyValueError::new_err(err.to_string()))
    }
}

fn log_entry_to_py(gil: Python, entry: &cedarling::LogEntry) -> PyResult<PyObject> {
    to_pyobject(gil, entry)
        .map(|v| v.unbind())
        .map_err(|err| err.0)
}
