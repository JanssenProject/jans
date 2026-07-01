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
use std::sync::OnceLock;

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
#[pyclass]
pub struct AuthorizeMultiIssuerRequest {
    /// List of TokenInput objects. Getter materializes a fresh Python list, so
    /// `req.tokens.append(t)` is silently discarded — reassign to mutate.
    #[pyo3(get)]
    pub tokens: Vec<TokenInput>,
    /// cedar_policy resource data
    #[pyo3(get, set)]
    pub resource: EntityData,
    /// cedar_policy action
    #[pyo3(get, set)]
    pub action: String,
    /// Optional context dict. Getter returns a MappingProxyType view — in-place edits
    /// raise TypeError; reassign to replace and refresh the cache.
    pub context: Option<Py<PyDict>>,
    /// Sticky cache of `tokens` converted to `cedarling::TokenInput`.
    cached_tokens: OnceLock<Vec<cedarling::TokenInput>>,
    /// Sticky cache of `context` deserialized.
    cached_context: OnceLock<Option<serde_json::Value>>,
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
            cached_tokens: OnceLock::new(),
            cached_context: OnceLock::new(),
        }
    }

    /// Setter clears the cached converted Vec.
    #[setter]
    fn set_tokens(&mut self, value: Vec<TokenInput>) {
        self.tokens = value;
        self.cached_tokens = OnceLock::new();
    }

    /// Read-only view over the context dict; mutation raises TypeError.
    #[getter]
    fn context(&self, py: Python) -> PyResult<Option<Py<PyAny>>> {
        match &self.context {
            Some(ctx) => {
                let proxy_cls = py.import("types")?.getattr("MappingProxyType")?;
                let proxy = proxy_cls.call1((ctx.clone_ref(py),))?;
                Ok(Some(proxy.unbind()))
            }
            None => Ok(None),
        }
    }

    /// Setter clears the deserialized cache.
    #[setter]
    fn set_context(&mut self, value: Option<Py<PyDict>>) {
        self.context = value;
        self.cached_context = OnceLock::new();
    }
}

impl AuthorizeMultiIssuerRequest {
    pub fn to_cedarling(&self) -> Result<cedarling::AuthorizeMultiIssuerRequest, PyErr> {
        // First call builds and stores; later calls clone the cached Vec.
        let tokens = self
            .cached_tokens
            .get_or_init(|| self.tokens.iter().cloned().map(Into::into).collect())
            .clone();

        let context = if let Some(cached) = self.cached_context.get() {
            cached.clone()
        } else {
            let value = match &self.context {
                Some(ctx) => Python::attach(|py| -> Result<Option<serde_json::Value>, PyErr> {
                    let bound = ctx.clone_ref(py).into_bound(py);
                    Ok(Some(from_pyobject(bound).map_err(|err| {
                        PyRuntimeError::new_err(format!(
                            "Failed to convert context to json: {}",
                            err
                        ))
                    })?))
                })?,
                None => None,
            };
            let _ = self.cached_context.set(value.clone());
            value
        };

        Ok(cedarling::AuthorizeMultiIssuerRequest {
            tokens,
            resource: self.resource.clone().into(),
            action: self.action.clone(),
            context,
        })
    }
}
