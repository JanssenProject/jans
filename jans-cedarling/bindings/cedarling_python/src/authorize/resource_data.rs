/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use pyo3::exceptions::{PyRuntimeError, PyValueError};
use pyo3::prelude::*;
use pyo3::types::{PyDict, PyType};
use serde_pyobject::from_pyobject;

/// ResourceData
/// ============
///
/// A Python wrapper for the Rust `cedarling::ResourceData` struct. This class represents
/// a resource entity with a type, ID, and attributes. Attributes are stored as a payload
/// in a dictionary format.
///
/// Attributes
/// ----------
/// :param resource_type: Type of the resource entity.
/// :param id: ID of the resource entity.
/// :param payload: Optional dictionary of attributes.
///
/// Methods
/// -------
/// .. method:: __init__(self, resource_type: str, id: str, **kwargs: dict)
///     Initialize a new ResourceData. In kwargs the payload is a dictionary of entity attributes.
///
/// .. method:: from_dict(cls, value: dict) -> ResourceData
///     Initialize a new ResourceData from a dictionary.
///     To pass `resource_type` you need to use `type` key.
#[derive(Clone, serde::Deserialize)]
#[pyclass]
pub struct ResourceData {
    /// entity type name
    #[pyo3(set)]
    #[serde(rename = "type")]
    pub resource_type: String,
    /// entity id
    #[pyo3(set)]
    pub id: String,
    /// entity attributes
    #[serde(flatten)]
    pub payload: PayloadType,
}

// type alias for the payload hash map
type PayloadType = HashMap<String, serde_json::Value>;

fn get_payload(object: Bound<'_, PyDict>) -> PyResult<PayloadType> {
    from_pyobject(object).map_err(|err| {
        PyRuntimeError::new_err(format!(
            "Failed to convert to rust HashMap<String, serde_json::Value>: {}",
            err
        ))
    })
}

#[pymethods]
impl ResourceData {
    #[new]
    #[pyo3(signature = (resource_type, id, **kwargs))]
    fn new(resource_type: String, id: String, kwargs: Option<Bound<'_, PyDict>>) -> PyResult<Self> {
        let payload = kwargs
            .map(|dict| get_payload(dict))
            .unwrap_or(Ok(HashMap::new()))?;

        Ok(Self {
            resource_type,
            id,
            payload,
        })
    }

    /// setter for payload attribute
    #[setter]
    fn payload(&mut self, value: Bound<'_, PyDict>) -> PyResult<()> {
        self.payload = get_payload(value)?;
        Ok(())
    }

    /// method to build a resource data object from a python dictionary
    #[classmethod]
    fn from_dict(_cls: Bound<'_, PyType>, value: Bound<'_, PyDict>) -> PyResult<Self> {
        from_pyobject(value).map_err(|err| PyValueError::new_err(err.0))
    }
}

impl From<ResourceData> for cedarling::EntityData {
    fn from(value: ResourceData) -> Self {
        Self {
            resource_type: value.resource_type,
            id: value.id,
            payload: value.payload,
        }
    }
}
