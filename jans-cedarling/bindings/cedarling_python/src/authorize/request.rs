/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::resource_data::ResourceData;
use pyo3::exceptions::PyRuntimeError;
use pyo3::prelude::*;
use pyo3::types::PyDict;
use serde_pyobject::from_pyobject;
use std::collections::HashMap;

/// Request
/// =======
///
/// A Python wrapper for the Rust `cedarling::Request` struct. Represents
/// authorization data with access token, action, resource, and context.
///
/// Attributes
/// ----------
/// :param tokens: A class containing the JWTs what will be used for the request.
/// :param action: The action to be authorized.
/// :param resource: Resource data (wrapped `ResourceData` object).
/// :param context: Python dictionary with additional context.
///
/// Example
/// -------
/// ```python
/// # Create a request for authorization
/// request = Request(access_token="token123", action="read", resource=resource, context={})
/// ```
#[pyclass(get_all, set_all)]
pub struct Request {
    pub tokens: Py<PyDict>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: ResourceData,
    /// context to be used in cedar_policy
    pub context: Py<PyDict>,
}

#[pymethods]
impl Request {
    #[new]
    #[pyo3(signature = (tokens, action, resource, context))]
    fn new(
        tokens: Py<PyDict>,
        action: String,
        resource: ResourceData,
        context: Py<PyDict>,
    ) -> Self {
        Self {
            tokens,
            action,
            resource,
            context,
        }
    }
}

impl Request {
    pub fn to_cedarling(&self) -> Result<cedarling::Request, PyErr> {
        let tokens = Python::with_gil(|py| -> Result<HashMap<String, String>, PyErr> {
            let tokens = self.tokens.clone_ref(py).into_bound(py);
            from_pyobject(tokens).map_err(|err| {
                PyRuntimeError::new_err(format!("Failed to convert tokens to json: {}", err))
            })
        })?;

        let context = Python::with_gil(|py| -> Result<serde_json::Value, PyErr> {
            let context = self.context.clone_ref(py).into_bound(py);
            from_pyobject(context).map_err(|err| {
                PyRuntimeError::new_err(format!("Failed to convert context to json: {}", err))
            })
        })?;

        Ok(cedarling::Request {
            tokens,
            action: self.action.clone(),
            resource: self.resource.clone().into(),
            context,
        })
    }
}
