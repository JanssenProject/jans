/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

use super::authorize_options::{self, AuthorizeOptions};
use super::batch_item::BatchItem;
use super::token_input::TokenInput;

/// BatchAuthorizeMultiIssuerRequest
/// ================================
///
/// A Python wrapper for the Rust `cedarling::BatchAuthorizeMultiIssuerRequest`.
/// Bundles one token set with N `BatchItem`s; tokens are validated and token
/// entities built once for the whole batch.
///
/// Attributes
/// ----------
/// :param tokens: List of `TokenInput` shared across every item.
/// :param items: List of `BatchItem` objects, evaluated in input order.
/// :param options: Optional `AuthorizeOptions`, e.g. a TRACE context.
///
/// Example
/// -------
/// ```python
/// req = BatchAuthorizeMultiIssuerRequest(
///     tokens=[TokenInput(mapping="Jans::Access_Token", payload="eyJ...")],
///     items=[BatchItem(resource=res1, action="Read", context={}), ...],
/// )
/// ```
#[pyclass]
pub(crate) struct BatchAuthorizeMultiIssuerRequest {
    /// Getter materializes a fresh Python list, so `req.tokens.append(t)` is
    /// silently discarded — reassign to mutate.
    #[pyo3(get)]
    pub tokens: Vec<TokenInput>,
    /// Getter materializes a fresh Python list, so `req.items.append(i)` is
    /// silently discarded — reassign to mutate.
    #[pyo3(get)]
    pub items: Vec<BatchItem>,
    /// Per-call options; never change the decision. The getter returns a copy,
    /// so mutating `req.options.trace` is discarded; assign new options instead.
    #[pyo3(get, set)]
    pub options: Option<AuthorizeOptions>,
}

#[pymethods]
impl BatchAuthorizeMultiIssuerRequest {
    #[new]
    #[pyo3(signature = (tokens, items, options=None))]
    fn new(
        tokens: Vec<TokenInput>,
        items: Vec<BatchItem>,
        options: Option<AuthorizeOptions>,
    ) -> Self {
        Self {
            tokens,
            items,
            options,
        }
    }

    /// Setter for `tokens`; assignment fully replaces the list.
    #[setter]
    fn set_tokens(&mut self, value: Vec<TokenInput>) {
        self.tokens = value;
    }

    /// Setter for `items`; assignment fully replaces the list.
    #[setter]
    fn set_items(&mut self, value: Vec<BatchItem>) {
        self.items = value;
    }
}

impl BatchAuthorizeMultiIssuerRequest {
    pub(crate) fn to_cedarling(
        &self,
    ) -> Result<cedarling::BatchAuthorizeMultiIssuerRequest, PyErr> {
        let tokens = self
            .tokens
            .iter()
            .cloned()
            .map(Into::into)
            .collect::<Vec<cedarling::TokenInput>>();
        let items = self
            .items
            .iter()
            .map(BatchItem::to_cedarling)
            .collect::<Result<Vec<_>, _>>()?;
        Ok(cedarling::BatchAuthorizeMultiIssuerRequest {
            tokens,
            items,
            options: authorize_options::to_cedarling(self.options.as_ref()),
        })
    }
}
