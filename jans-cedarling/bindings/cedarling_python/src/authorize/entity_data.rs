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

/// EntityData
/// ============
///
/// A Python wrapper for the Rust `cedarling::EntityData` struct. This class represents
/// a resource entity with a type, ID, and attributes. Attributes are stored as a payload
/// in a dictionary format.
///
/// Attributes
/// ----------
/// :param entity_type: Type of the entity.
/// :param id: ID of the entity.
/// :param payload: Optional dictionary of attributes.
///
/// Methods
/// -------
/// .. method:: __init__(self, resource_type: str, id: str, **kwargs: dict)
///     Initialize a new EntityData. In kwargs the payload is a dictionary of entity attributes.
///
/// .. method:: from_dict(cls, value: dict) -> EntityData
///     Initialize a new EntityData from a dictionary.
///     To pass `resource_type` you need to use `type` key.
#[derive(Clone, serde::Deserialize)]
#[pyclass]
pub struct EntityData {
    /// entity type name
    #[pyo3(set)]
    #[serde(rename = "type")]
    pub entity_type: String,
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
impl EntityData {
    #[new]
    #[pyo3(signature = (entity_type, id, **kwargs))]
    fn new(entity_type: String, id: String, kwargs: Option<Bound<'_, PyDict>>) -> PyResult<Self> {
        let payload = kwargs
            .map(|dict| get_payload(dict))
            .unwrap_or(Ok(HashMap::new()))?;

        Ok(Self {
            entity_type,
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

impl From<EntityData> for cedarling::EntityData {
    fn from(value: EntityData) -> Self {
        Self {
            entity_type: value.entity_type,
            id: value.id,
            payload: value.payload,
        }
    }
}
