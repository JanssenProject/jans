/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::entity_data::EntityData;
use pyo3::exceptions::PyRuntimeError;
use pyo3::prelude::*;
use pyo3::types::PyDict;
use serde_pyobject::from_pyobject;

/// BatchItem
/// =========
///
/// A Python wrapper for the Rust `cedarling::BatchItem` struct. Represents one
/// `{resource, action, context}` triple inside a batch authorization request.
///
/// Attributes
/// ----------
/// :param resource: `EntityData` describing the resource for this item.
/// :param action: The action string for this item (e.g., 'Jans::Action::"Read"').
/// :param context: Optional Python dict of per-item context. `None` defaults to `{}`.
///
/// Example
/// -------
/// ```python
/// item = BatchItem(resource=resource, action="Jans::Action::\"Read\"", context={})
/// ```
#[pyclass(from_py_object)]
pub struct BatchItem {
    #[pyo3(get, set)]
    pub resource: EntityData,
    #[pyo3(get, set)]
    pub action: String,
    /// Per-item context dict; deserialized on every `to_cedarling()` call so
    /// mutations to the source dict (or nested values) are always picked up.
    #[pyo3(get, set)]
    pub context: Option<Py<PyDict>>,
}

// `Py<PyDict>` is only `Clone` under the GIL; a manual impl takes the GIL and
// clones the reference via `clone_ref`, so `BatchItem` can be extracted from
// Python lists (`Vec<BatchItem>`) and cloned on the Rust side.
impl Clone for BatchItem {
    fn clone(&self) -> Self {
        Python::attach(|py| Self {
            resource: self.resource.clone(),
            action: self.action.clone(),
            context: self.context.as_ref().map(|c| c.clone_ref(py)),
        })
    }
}

#[pymethods]
impl BatchItem {
    #[new]
    #[pyo3(signature = (resource, action, context=None))]
    fn new(resource: EntityData, action: String, context: Option<Py<PyDict>>) -> Self {
        Self {
            resource,
            action,
            context,
        }
    }
}

impl BatchItem {
    pub fn to_cedarling(&self) -> Result<cedarling::BatchItem, PyErr> {
        let context = Python::attach(|py| -> Result<serde_json::Value, PyErr> {
            match &self.context {
                Some(ctx) => {
                    let bound = ctx.clone_ref(py).into_bound(py);
                    from_pyobject(bound).map_err(|err| {
                        PyRuntimeError::new_err(format!(
                            "Failed to convert batch item context to json: {err}"
                        ))
                    })
                },
                None => Ok(serde_json::Value::Object(serde_json::Map::new())),
            }
        })?;

        Ok(cedarling::BatchItem {
            resource: self.resource.clone().into(),
            action: self.action.clone(),
            context,
        })
    }
}
