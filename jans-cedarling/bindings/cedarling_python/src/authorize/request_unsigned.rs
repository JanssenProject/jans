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
use std::sync::OnceLock;

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
#[pyclass]
pub struct RequestUnsigned {
    #[pyo3(get, set)]
    pub principal: Option<EntityData>,
    /// cedar_policy action
    #[pyo3(get, set)]
    pub action: String,
    /// cedar_policy resource data
    #[pyo3(get, set)]
    pub resource: EntityData,
    /// Context dict. Getter returns a MappingProxyType view — in-place edits
    /// raise TypeError; reassign (`req.context = {...}`) to replace and refresh the cache.
    pub context: Py<PyDict>,
    /// Sticky cache of `context` deserialized; populated on first `to_cedarling()` call.
    cached_context: OnceLock<serde_json::Value>,
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
            cached_context: OnceLock::new(),
        }
    }

    /// Read-only view over the context dict; mutation raises TypeError.
    #[getter]
    fn context(&self, py: Python) -> PyResult<Py<PyAny>> {
        let proxy_cls = py.import("types")?.getattr("MappingProxyType")?;
        let proxy = proxy_cls.call1((self.context.clone_ref(py),))?;
        Ok(proxy.unbind())
    }

    /// Setter clears the deserialized cache.
    #[setter]
    fn set_context(&mut self, value: Py<PyDict>) {
        self.context = value;
        self.cached_context = OnceLock::new();
    }
}

impl RequestUnsigned {
    pub fn to_cedarling(&self) -> Result<cedarling::RequestUnsigned, PyErr> {
        let principal = self.principal.clone().map(|p| p.into());

        let context = if let Some(cached) = self.cached_context.get() {
            cached.clone()
        } else {
            let value = Python::attach(|py| -> Result<serde_json::Value, PyErr> {
                let ctx = self.context.clone_ref(py).into_bound(py);
                from_pyobject(ctx).map_err(|err| {
                    PyRuntimeError::new_err(format!("Failed to convert context to json: {}", err))
                })
            })?;
            let _ = self.cached_context.set(value.clone());
            value
        };

        Ok(cedarling::RequestUnsigned {
            principal,
            action: self.action.clone(),
            resource: self.resource.clone().into(),
            context,
        })
    }
}
