/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;
use serde_pyobject::to_pyobject;

use super::cedar_type::CedarType;

/// DataEntry
/// =========
///
/// A data entry in the DataStore with value and metadata.
///
/// Attributes
/// ----------
/// key : str
///     The key for this entry
/// value : dict
///     The actual value stored (as a Python dict)
/// data_type : CedarType
///     The inferred Cedar type of the value
/// created_at : str
///     Timestamp when this entry was created (RFC 3339 format)
/// expires_at : str | None
///     Timestamp when this entry expires (RFC 3339 format), or None if no TTL
/// access_count : int
///     Number of times this entry has been accessed
#[pyclass]
pub struct DataEntry {
    inner: cedarling::DataEntry,
}

#[pymethods]
impl DataEntry {
    /// Get the key for this entry
    #[getter]
    fn key(&self) -> String {
        self.inner.key.clone()
    }

    /// Get the value stored in this entry
    fn value(&self, py: Python) -> PyResult<Py<PyAny>> {
        to_pyobject(py, &self.inner.value)
            .map(|v| v.unbind())
            .map_err(|err| err.0)
    }

    /// Get the Cedar type of this entry
    #[getter]
    fn data_type(&self) -> CedarType {
        self.inner.data_type.into()
    }

    /// Get the creation timestamp (RFC 3339 format)
    #[getter]
    fn created_at(&self) -> String {
        self.inner.created_at.to_rfc3339()
    }

    /// Get the expiration timestamp (RFC 3339 format), or None if no TTL
    #[getter]
    fn expires_at(&self) -> Option<String> {
        self.inner.expires_at.map(|dt| dt.to_rfc3339())
    }

    /// Get the access count
    #[getter]
    fn access_count(&self) -> u64 {
        self.inner.access_count
    }

    /// String representation
    fn __str__(&self) -> String {
        format!(
            "DataEntry(key={}, type={:?})",
            self.inner.key, self.inner.data_type
        )
    }

    /// Detailed representation
    fn __repr__(&self) -> String {
        format!(
            "DataEntry(key={}, type={:?}, created_at={}, access_count={})",
            self.inner.key,
            self.inner.data_type,
            self.inner.created_at.to_rfc3339(),
            self.inner.access_count
        )
    }
}

impl From<cedarling::DataEntry> for DataEntry {
    fn from(value: cedarling::DataEntry) -> Self {
        Self { inner: value }
    }
}
