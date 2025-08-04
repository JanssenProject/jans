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
/// a resource entity with Cedar mapping information and attributes. Attributes are stored as a payload
/// in a dictionary format.
///
/// Attributes
/// ----------
/// :param cedar_entity_mapping: Cedar entity mapping information containing entity_type and id.
/// :param payload: Optional dictionary of attributes.
///
/// Methods
/// -------
/// .. method:: __init__(self, entity_type: str, id: str, **kwargs: dict)
///     Initialize a new EntityData. In kwargs the payload is a dictionary of entity attributes.
///
/// .. method:: from_dict(cls, value: dict) -> EntityData
///     Initialize a new EntityData from a dictionary.
///     The dictionary should contain a `cedar_entity_mapping` field with `entity_type` and `id` subfields.
#[derive(Clone, serde::Deserialize)]
#[pyclass]
pub struct EntityData {
    /// Cedar entity mapping info
    #[pyo3(set)]
    #[serde(rename = "cedar_entity_mapping")]
    pub cedar_mapping: CedarEntityMapping,
    /// entity attributes
    #[serde(flatten)]
    pub attributes: EntityDataAttrs,
}

/// Cedar entity mapping information
#[derive(Clone, serde::Deserialize)]
#[pyclass]
pub struct CedarEntityMapping {
    /// entity type name
    #[pyo3(set)]
    #[serde(rename = "entity_type")]
    pub entity_type: String,
    /// entity id
    #[pyo3(set)]
    pub id: String,
}

#[pymethods]
impl CedarEntityMapping {
    #[new]
    fn new(entity_type: String, id: String) -> Self {
        Self {
            entity_type,
            id,
        }
    }
}

// type alias for the entity data attributes
type EntityDataAttrs = HashMap<String, serde_json::Value>;

fn get_payload(object: Bound<'_, PyDict>) -> PyResult<EntityDataAttrs> {
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
    #[pyo3(signature = (cedar_entity_mapping, **kwargs))]
    fn new(cedar_entity_mapping: CedarEntityMapping, kwargs: Option<Bound<'_, PyDict>>) -> PyResult<Self> {
        let attributes = kwargs
            .map(|dict| get_payload(dict))
            .unwrap_or(Ok(HashMap::new()))?;

        Ok(Self {
            cedar_mapping: cedar_entity_mapping,
            attributes,
        })
    }

    /// setter for payload attribute
    #[setter]
    fn payload(&mut self, value: Bound<'_, PyDict>) -> PyResult<()> {
        self.attributes = get_payload(value)?;
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
            cedar_mapping: cedarling::CedarEntityMapping {
                entity_type: value.cedar_mapping.entity_type,
                id: value.cedar_mapping.id,
            },
            attributes: value.attributes,
        }
    }
}
