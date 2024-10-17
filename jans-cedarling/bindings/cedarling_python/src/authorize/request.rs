/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

// use cedarling::ResourceData;
use super::resource_data::ResourceData;
use pyo3::prelude::*;
use pyo3::types::PyDict;
use serde_pyobject::from_pyobject;

/// Request
/// =======
///
/// A Python wrapper for the Rust `cedarling::Request` struct. Represents
/// authorization data with access token, action, resource, and context.
///
/// Attributes
/// ----------
/// :param access_token: The access token string.
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
    /// Access token raw value
    pub access_token: String,
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
    fn new(
        access_token: String,
        action: String,
        resource: ResourceData,
        context: Py<PyDict>,
    ) -> Self {
        Self {
            access_token,
            action,
            resource,
            context,
        }
    }
}

impl TryFrom<Request> for cedarling::Request {
    type Error = PyErr;

    fn try_from(value: Request) -> Result<Self, Self::Error> {
        let context = Python::with_gil(|py| -> Result<serde_json::Value, PyErr> {
            let context = value.context.into_bound(py);
            from_pyobject(context).map_err(|err| err.0)
        })?;

        Ok(Self {
            access_token: value.access_token,
            action: value.action,
            resource: value.resource.into(),
            context,
        })
    }
}
