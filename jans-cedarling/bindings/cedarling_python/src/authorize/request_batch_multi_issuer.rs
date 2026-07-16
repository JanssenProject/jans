/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::batch_item::BatchItem;
use super::token_input::TokenInput;
use pyo3::prelude::*;

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
}

#[pymethods]
impl BatchAuthorizeMultiIssuerRequest {
    #[new]
    fn new(tokens: Vec<TokenInput>, items: Vec<BatchItem>) -> Self {
        Self { tokens, items }
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
        Ok(cedarling::BatchAuthorizeMultiIssuerRequest { tokens, items })
    }
}
