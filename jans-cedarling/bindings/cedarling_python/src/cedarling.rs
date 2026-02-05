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
use crate::authorize::multi_issuer_authorize_result::MultiIssuerAuthorizeResult;
use crate::authorize::request::Request;
use crate::authorize::request_multi_issuer::AuthorizeMultiIssuerRequest;
use crate::authorize::request_unsigned::RequestUnsigned;
use crate::config::bootstrap_config::BootstrapConfig;
use crate::context_data_api::data_entry::DataEntry;
use crate::context_data_api::data_store_stats::DataStoreStats;
use crate::context_data_api::errors::data_error_to_py;
use cedarling::DataApi;
use serde_pyobject::{from_pyobject, to_pyobject};
use std::time::Duration;

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
/// .. method:: authorize_unsigned(self, request: RequestUnsigned) -> AuthorizeResult
///
///     Authorize request with unsigned data.
///     :param request: RequestUnsigned struct for authorize.
///
/// .. method:: authorize_multi_issuer(self, request: AuthorizeMultiIssuerRequest) -> MultiIssuerAuthorizeResult
///
///     Authorize multi-issuer request.
///     Makes authorization decision based on multiple JWT tokens from different issuers.
///     :param request: AuthorizeMultiIssuerRequest struct for authorize.
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
/// .. method:: get_logs_by_request_id(self, id: str) -> List[dict]
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
/// .. method:: shut_down(self)
///
///     Closes the connections to the Lock Server and pushes all available logs.
///
/// .. method:: push_data_ctx(self, key: str, value: Any, ttl_secs: int | None = None)
///
///     Push a value into the data store with an optional TTL.
///     If the key already exists, the value will be replaced.
///     If TTL is not provided, the default TTL from configuration is used.
///
///     :param key: The key for the data entry
///     :param value: The value to store (dict, list, str, int, float, bool)
///     :param ttl_secs: Optional TTL in seconds (None uses default from config)
///     :raises DataError: If the operation fails
///
/// .. method:: get_data_ctx(self, key: str) -> Any | None
///
///     Get a value from the data store by key.
///     Returns None if the key doesn't exist or the entry has expired.
///
///     :param key: The key to retrieve
///     :returns: The value as a Python object, or None if not found
///     :raises DataError: If the operation fails
///
/// .. method:: get_data_entry_ctx(self, key: str) -> DataEntry | None
///
///     Get a data entry with full metadata by key.
///     Returns None if the key doesn't exist or the entry has expired.
///
///     :param key: The key to retrieve
///     :returns: A DataEntry object with metadata, or None if not found
///     :raises DataError: If the operation fails
///
/// .. method:: remove_data_ctx(self, key: str) -> bool
///
///     Remove a value from the data store by key.
///
///     :param key: The key to remove
///     :returns: True if the key existed and was removed, False otherwise
///     :raises DataError: If the operation fails
///
/// .. method:: clear_data_ctx(self)
///
///     Clear all entries from the data store.
///
///     :raises DataError: If the operation fails
///
/// .. method:: list_data_ctx(self) -> List[DataEntry]
///
///     List all entries with their metadata.
///     Returns a list of DataEntry objects containing key, value, type, and timing metadata.
///
///     :returns: A list of DataEntry objects
///     :raises DataError: If the operation fails
///
/// .. method:: get_stats_ctx(self) -> DataStoreStats
///
///     Get statistics about the data store.
///     Returns current entry count, capacity limits, and configuration state.
///
///     :returns: A DataStoreStats object
///     :raises DataError: If the operation fails
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

    /// Authorize request with unsigned data.
    fn authorize_unsigned(
        &self,
        request: Bound<'_, RequestUnsigned>,
    ) -> Result<AuthorizeResult, PyErr> {
        let cedarling_instance = self
            .inner
            .authorize_unsigned(request.borrow().to_cedarling()?)
            .map_err(authorize_error_to_py)?;
        Ok(cedarling_instance.into())
    }

    /// Authorize multi-issuer request.
    /// Makes authorization decision based on multiple JWT tokens from different issuers.
    fn authorize_multi_issuer(
        &self,
        request: Bound<'_, AuthorizeMultiIssuerRequest>,
    ) -> Result<MultiIssuerAuthorizeResult, PyErr> {
        let cedarling_instance = self
            .inner
            .authorize_multi_issuer(request.borrow().to_cedarling()?)
            .map_err(authorize_error_to_py)?;
        Ok(cedarling_instance.into())
    }

    /// Return logs and remove them from the storage
    fn pop_logs(&self) -> PyResult<Vec<Py<PyAny>>> {
        let logs = self.inner.pop_logs();
        Python::attach(|py| -> PyResult<Vec<Py<PyAny>>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<Py<PyAny>>>>()
        })
    }

    /// Get specific log entry
    fn get_log_by_id(&self, id: &str) -> PyResult<Option<Py<PyAny>>> {
        // It doesn't follow a functional approach because handling all types properly challenging
        // and this code easy to read
        if let Some(entry) = self.inner.get_log_by_id(id) {
            let py_obj =
                Python::attach(|py| -> PyResult<Py<PyAny>> { log_entry_to_py(py, &entry) })?;
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
    fn get_logs_by_tag(&self, tag: &str) -> PyResult<Vec<Py<PyAny>>> {
        let logs = self.inner.get_logs_by_tag(tag);

        Python::attach(|py| -> PyResult<Vec<Py<PyAny>>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<Py<PyAny>>>>()
        })
    }

    /// Returns a list of log entries by request id.
    fn get_logs_by_request_id(&self, request_id: &str) -> PyResult<Vec<Py<PyAny>>> {
        let logs = self.inner.get_logs_by_request_id(request_id);
        Python::attach(|py| -> PyResult<Vec<Py<PyAny>>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<Py<PyAny>>>>()
        })
    }

    /// Returns a list of all log entries by request id and tag.
    /// Tag can be `log_kind`, `log_level`.
    fn get_logs_by_request_id_and_tag(&self, id: &str, tag: &str) -> PyResult<Vec<Py<PyAny>>> {
        let logs = self.inner.get_logs_by_request_id_and_tag(id, tag);

        Python::attach(|py| -> PyResult<Vec<Py<PyAny>>> {
            logs.iter()
                .map(|entry| log_entry_to_py(py, entry))
                .collect::<PyResult<Vec<Py<PyAny>>>>()
        })
    }

    /// Closes the connections to the Lock Server and pushes all available logs.
    fn shut_down(&self) {
        self.inner.shut_down();
    }

    /// Push a value into the data store with an optional TTL.
    ///
    /// If the key already exists, the value will be replaced.
    /// If TTL is not provided, the default TTL from configuration is used.
    ///
    /// :param key: The key for the data entry
    /// :param value: The value to store (dict, list, str, int, float, bool)
    /// :param ttl_secs: Optional TTL in seconds (None uses default from config)
    /// :raises DataError: If the operation fails
    #[pyo3(signature = (key, value, *, ttl_secs = None))]
    fn push_data_ctx(
        &self,
        key: &str,
        value: Bound<'_, PyAny>,
        ttl_secs: Option<u64>,
    ) -> PyResult<()> {
        let json_value: serde_json::Value = from_pyobject(value).map_err(|err| err.0)?;

        let ttl = ttl_secs.map(Duration::from_secs);
        self.inner
            .push_data_ctx(key, json_value, ttl)
            .map_err(|err| data_error_to_py(Box::new(err)))?;
        Ok(())
    }

    /// Get a value from the data store by key.
    ///
    /// Returns None if the key doesn't exist or the entry has expired.
    /// If metrics are enabled, increments the access count for the entry.
    ///
    /// :param key: The key to retrieve
    /// :returns: The value as a Python object, or None if not found
    /// :raises DataError: If the operation fails
    fn get_data_ctx(&self, key: &str, py: Python) -> PyResult<Option<Py<PyAny>>> {
        match self
            .inner
            .get_data_ctx(key)
            .map_err(|err| data_error_to_py(Box::new(err)))?
        {
            Some(value) => {
                let py_obj = to_pyobject(py, &value)
                    .map(|v| v.unbind())
                    .map_err(|err| err.0)?;
                Ok(Some(py_obj))
            },
            None => Ok(None),
        }
    }

    /// Get a data entry with full metadata by key.
    ///
    /// Returns None if the key doesn't exist or the entry has expired.
    /// Includes metadata like creation time, expiration, access count, and type.
    ///
    /// :param key: The key to retrieve
    /// :returns: A DataEntry object with metadata, or None if not found
    /// :raises DataError: If the operation fails
    fn get_data_entry_ctx(&self, key: &str) -> PyResult<Option<DataEntry>> {
        match self
            .inner
            .get_data_entry_ctx(key)
            .map_err(|err| data_error_to_py(Box::new(err)))?
        {
            Some(entry) => Ok(Some(entry.into())),
            None => Ok(None),
        }
    }

    /// Remove a value from the data store by key.
    ///
    /// :param key: The key to remove
    /// :returns: True if the key existed and was removed, False otherwise
    /// :raises DataError: If the operation fails
    fn remove_data_ctx(&self, key: &str) -> PyResult<bool> {
        self.inner
            .remove_data_ctx(key)
            .map_err(|err| data_error_to_py(Box::new(err)))
    }

    /// Clear all entries from the data store.
    ///
    /// :raises DataError: If the operation fails
    fn clear_data_ctx(&self) -> PyResult<()> {
        self.inner
            .clear_data_ctx()
            .map_err(|err| data_error_to_py(Box::new(err)))
    }

    /// List all entries with their metadata.
    ///
    /// Returns a list of DataEntry objects containing key, value, type, and timing metadata.
    ///
    /// :returns: A list of DataEntry objects
    /// :raises DataError: If the operation fails
    fn list_data_ctx(&self) -> PyResult<Vec<DataEntry>> {
        self.inner
            .list_data_ctx()
            .map(|entries| entries.into_iter().map(|e| e.into()).collect())
            .map_err(|err| data_error_to_py(Box::new(err)))
    }

    /// Get statistics about the data store.
    ///
    /// Returns current entry count, capacity limits, and configuration state.
    ///
    /// :returns: A DataStoreStats object
    /// :raises DataError: If the operation fails
    fn get_stats_ctx(&self) -> PyResult<DataStoreStats> {
        self.inner
            .get_stats_ctx()
            .map(|stats| stats.into())
            .map_err(|err| data_error_to_py(Box::new(err)))
    }
}

fn log_entry_to_py(gil: Python, entry: &serde_json::Value) -> PyResult<Py<PyAny>> {
    to_pyobject(gil, entry)
        .map(|v| v.unbind())
        .map_err(|err| err.0)
}
