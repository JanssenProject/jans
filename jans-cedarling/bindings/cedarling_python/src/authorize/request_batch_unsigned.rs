/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

use super::authorize_options::{self, AuthorizeOptions};
use super::batch_item::BatchItem;
use super::entity_data::EntityData;

/// BatchAuthorizeUnsignedRequest
/// =============================
///
/// A Python wrapper for the Rust `cedarling::BatchAuthorizeUnsignedRequest`.
/// Bundles one optional principal with N `BatchItem`s; every item is evaluated
/// against the same principal snapshot and pushed-data snapshot.
///
/// Attributes
/// ----------
/// :param principal: Optional `EntityData` shared across every item.
/// :param items: List of `BatchItem` objects, evaluated in input order.
/// :param options: Optional `AuthorizeOptions`, e.g. a TRACE context.
///
/// Example
/// -------
/// ```python
/// req = BatchAuthorizeUnsignedRequest(
///     principal=principal,
///     items=[BatchItem(resource=res1, action="Read", context={}), ...],
/// )
/// ```
#[pyclass]
pub(crate) struct BatchAuthorizeUnsignedRequest {
    #[pyo3(get, set)]
    pub principal: Option<EntityData>,
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
impl BatchAuthorizeUnsignedRequest {
    #[new]
    #[pyo3(signature = (items, principal=None, options=None))]
    fn new(
        items: Vec<BatchItem>,
        principal: Option<EntityData>,
        options: Option<AuthorizeOptions>,
    ) -> Self {
        Self {
            principal,
            items,
            options,
        }
    }

    /// Setter for `items`; assignment fully replaces the list.
    #[setter]
    fn set_items(&mut self, value: Vec<BatchItem>) {
        self.items = value;
    }
}

impl BatchAuthorizeUnsignedRequest {
    pub(crate) fn to_cedarling(&self) -> Result<cedarling::BatchAuthorizeUnsignedRequest, PyErr> {
        let items = self
            .items
            .iter()
            .map(BatchItem::to_cedarling)
            .collect::<Result<Vec<_>, _>>()?;
        Ok(cedarling::BatchAuthorizeUnsignedRequest {
            principal: self.principal.clone().map(Into::into),
            items,
            options: authorize_options::to_cedarling(self.options.as_ref()),
        })
    }
}
