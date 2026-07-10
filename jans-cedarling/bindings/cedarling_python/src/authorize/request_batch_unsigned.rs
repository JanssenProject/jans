/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::batch_item::BatchItem;
use super::entity_data::EntityData;
use pyo3::prelude::*;

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
pub struct BatchAuthorizeUnsignedRequest {
    #[pyo3(get, set)]
    pub principal: Option<EntityData>,
    /// Getter materializes a fresh Python list, so `req.items.append(i)` is
    /// silently discarded — reassign to mutate.
    #[pyo3(get)]
    pub items: Vec<BatchItem>,
}

#[pymethods]
impl BatchAuthorizeUnsignedRequest {
    #[new]
    #[pyo3(signature = (items, principal=None))]
    fn new(items: Vec<BatchItem>, principal: Option<EntityData>) -> Self {
        Self { principal, items }
    }

    /// Setter for `items`; assignment fully replaces the list.
    #[setter]
    fn set_items(&mut self, value: Vec<BatchItem>) {
        self.items = value;
    }
}

impl BatchAuthorizeUnsignedRequest {
    pub fn to_cedarling(&self) -> Result<cedarling::BatchAuthorizeUnsignedRequest, PyErr> {
        let items = self
            .items
            .iter()
            .map(BatchItem::to_cedarling)
            .collect::<Result<Vec<_>, _>>()?;
        Ok(cedarling::BatchAuthorizeUnsignedRequest {
            principal: self.principal.clone().map(Into::into),
            items,
        })
    }
}
