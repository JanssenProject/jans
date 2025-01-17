/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

// use cedarling::ResourceData;
use super::resource_data::ResourceData;
use pyo3::exceptions::PyRuntimeError;
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
    pub tokens: Tokens,
    pub new_tokens: HashMap<String, String>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: ResourceData,
    /// context to be used in cedar_policy
    pub context: Py<PyDict>,
}

/// Tokens
/// =======
///
/// A Python wrapper for the Rust `cedarling::Token` struct. Contains the JWTs
/// that will be used for the AuthZ request.
///
/// Attributes
/// ----------
/// :param access_token: (Optional) The access token string.
/// :param id_token: (Optional) The id token string.
/// :param userinfo_token: (Optional) The userinfo token string.
///
/// Example
/// -------
/// ```python
/// tokens = Request("your_access_tkn", "your_id_tkn", "your_userinfo_tkn")
/// ```
#[derive(Clone)]
#[pyclass(get_all, set_all)]
pub struct Tokens {
    /// Access token raw value
    pub access_token: Option<String>,
    /// Id token raw value
    pub id_token: Option<String>,
    /// Userinfo token raw value
    pub userinfo_token: Option<String>,
}

#[pymethods]
impl Tokens {
    #[new]
    #[pyo3(signature = (access_token, id_token, userinfo_token))]
    fn new(
        access_token: Option<String>,
        id_token: Option<String>,
        userinfo_token: Option<String>,
    ) -> Self {
        Self {
            access_token,
            id_token,
            userinfo_token,
        }
    }
}

#[pymethods]
impl Request {
    #[new]
    #[pyo3(signature = (tokens, action, resource, context))]
    fn new(tokens: Tokens, action: String, resource: ResourceData, context: Py<PyDict>) -> Self {
        Self {
            tokens,
            new_tokens: HashMap::new(), // TODO: replace tokens
            action,
            resource,
            context,
        }
    }
}

impl From<Tokens> for cedarling::Tokens {
    fn from(tokens: Tokens) -> Self {
        Self {
            access_token: tokens.access_token,
            id_token: tokens.id_token,
            userinfo_token: tokens.userinfo_token,
        }
    }
}

impl Request {
    pub fn to_cedarling(&self) -> Result<cedarling::Request, PyErr> {
        let context = Python::with_gil(|py| -> Result<serde_json::Value, PyErr> {
            let context = self.context.clone_ref(py).into_bound(py);
            from_pyobject(context).map_err(|err| {
                PyRuntimeError::new_err(format!("Failed to convert context: {}", err))
            })
        })?;

        Ok(cedarling::Request {
            tokens: self.tokens.clone().into(),
            new_tokens: self.new_tokens.clone().into(),
            action: self.action.clone(),
            resource: self.resource.clone().into(),
            context,
        })
    }
}
