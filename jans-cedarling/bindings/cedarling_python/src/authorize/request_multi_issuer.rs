/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::entity_data::EntityData;
use super::token_input::TokenInput;
use pyo3::exceptions::PyRuntimeError;
use pyo3::prelude::*;
use pyo3::types::PyDict;
use serde_pyobject::from_pyobject;

/// AuthorizeMultiIssuerRequest
/// ===========================
///
/// A Python wrapper for the Rust `cedarling::AuthorizeMultiIssuerRequest` struct.
/// Represents a multi-issuer authorization request with multiple JWT tokens from different issuers.
///
/// Attributes
/// ----------
/// :param tokens: List of TokenInput objects containing JWT tokens with explicit type mappings
/// :param resource: Resource data (wrapped `EntityData` object)
/// :param action: The action to be authorized
/// :param context: Optional Python dictionary with additional context
///
/// Example
/// -------
/// ```python
/// # Create a multi-issuer authorization request
/// tokens = [
///     TokenInput(mapping="Jans::Access_Token", payload="eyJhbGc..."),
///     TokenInput(mapping="Acme::DolphinToken", payload="eyJhbGc...")
/// ]
/// request = AuthorizeMultiIssuerRequest(
///     tokens=tokens,
///     resource=resource,
///     action="Read",
///     context={"location": "miami"}
/// )
/// ```
#[pyclass(get_all, set_all)]
pub struct AuthorizeMultiIssuerRequest {
    /// List of TokenInput objects
    pub tokens: Vec<TokenInput>,
    /// cedar_policy resource data
    pub resource: EntityData,
    /// cedar_policy action
    pub action: String,
    /// Optional context to be used in cedar_policy
    pub context: Option<Py<PyDict>>,
}

#[pymethods]
impl AuthorizeMultiIssuerRequest {
    #[new]
    #[pyo3(signature = (tokens, resource, action, context=None))]
    fn new(
        tokens: Vec<TokenInput>,
        resource: EntityData,
        action: String,
        context: Option<Py<PyDict>>,
    ) -> Self {
        Self {
            tokens,
            resource,
            action,
            context,
        }
    }
}

impl AuthorizeMultiIssuerRequest {
    pub fn to_cedarling(&self) -> Result<cedarling::AuthorizeMultiIssuerRequest, PyErr> {
        let tokens = self.tokens.clone().into_iter().map(Into::into).collect();

        let context = if let Some(ref ctx) = self.context {
            Some(Python::attach(|py| -> Result<serde_json::Value, PyErr> {
                let context = ctx.clone_ref(py).into_bound(py);
                from_pyobject(context).map_err(|err| {
                    PyRuntimeError::new_err(format!("Failed to convert context to json: {}", err))
                })
            })?)
        } else {
            None
        };

        Ok(cedarling::AuthorizeMultiIssuerRequest {
            tokens,
            resource: self.resource.clone().into(),
            action: self.action.clone(),
            context,
        })
    }
}
