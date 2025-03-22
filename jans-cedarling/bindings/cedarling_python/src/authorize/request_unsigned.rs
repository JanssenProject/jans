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

/// Request
/// =======
///
/// A Python wrapper for the Rust `cedarling::RequestUnsigned` struct. Represents
/// authorization data for unsigned authorization requests for many principals.
///
/// Attributes
/// ----------
/// :param principals: A list of `EntityData` objects representing the principals.
/// :param action: The action to be authorized.
/// :param resource: Resource data (wrapped `ResourceData` object).
/// :param context: Python dictionary with additional context.
///
/// Example
/// -------
/// ```python
/// # Create a request for authorization
/// request = Request(principals=[principal], action="read", resource=resource, context={})
/// ```
#[pyclass(get_all, set_all)]
pub struct RequestUnsigned {
    pub principals: Vec<EntityData>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: EntityData,
    /// context to be used in cedar_policy
    pub context: Py<PyDict>,
}

#[pymethods]
impl RequestUnsigned {
    #[new]
    #[pyo3(signature = (principals, action, resource, context))]
    fn new(
        principals: Vec<EntityData>,
        action: String,
        resource: EntityData,
        context: Py<PyDict>,
    ) -> Self {
        Self {
            principals,
            action,
            resource,
            context,
        }
    }
}

impl RequestUnsigned {
    pub fn to_cedarling(&self) -> Result<cedarling::RequestUnsigned, PyErr> {
        let principals = self
            .principals
            .iter()
            .map(|p| p.to_owned().into())
            .collect();

        let context = Python::with_gil(|py| -> Result<serde_json::Value, PyErr> {
            let context = self.context.clone_ref(py).into_bound(py);
            from_pyobject(context).map_err(|err| {
                PyRuntimeError::new_err(format!("Failed to convert context to json: {}", err))
            })
        })?;

        Ok(cedarling::RequestUnsigned {
            principals,
            action: self.action.clone(),
            resource: self.resource.clone().into(),
            context,
        })
    }
}
