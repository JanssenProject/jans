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

/// RequestUnsigned
/// =======
///
/// A Python wrapper for the Rust `cedarling::RequestUnsigned` struct. Represents
/// authorization data for an unsigned authorization request for an optional single
/// principal.
///
/// Attributes
/// ----------
/// :param principal: Optional `EntityData` representing the principal. When `None`,
///     the core performs partial evaluation of the policies.
/// :param action: The action to be authorized.
/// :param resource: Resource data (wrapped `ResourceData` object).
/// :param context: Python dictionary with additional context.
///
/// Example
/// -------
/// ```python
/// # Create a request for authorization
/// request = RequestUnsigned(principal=principal, action="read", resource=resource, context={})
/// ```
#[pyclass(get_all, set_all)]
pub struct RequestUnsigned {
    pub principal: Option<EntityData>,
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
    #[pyo3(signature = (action, resource, context, principal=None))]
    fn new(
        action: String,
        resource: EntityData,
        context: Py<PyDict>,
        principal: Option<EntityData>,
    ) -> Self {
        Self {
            principal,
            action,
            resource,
            context,
        }
    }
}

impl RequestUnsigned {
    pub fn to_cedarling(&self) -> Result<cedarling::RequestUnsigned, PyErr> {
        let principal = self.principal.clone().map(|p| p.into());

        let context = Python::attach(|py| -> Result<serde_json::Value, PyErr> {
            let context = self.context.clone_ref(py).into_bound(py);
            from_pyobject(context).map_err(|err| {
                PyRuntimeError::new_err(format!("Failed to convert context to json: {}", err))
            })
        })?;

        Ok(cedarling::RequestUnsigned {
            principal,
            action: self.action.clone(),
            resource: self.resource.clone().into(),
            context,
        })
    }
}
