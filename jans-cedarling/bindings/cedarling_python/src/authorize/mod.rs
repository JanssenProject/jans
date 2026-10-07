/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;
use pyo3::Bound;

pub(crate) mod authorize_result;
mod authorize_result_response;
pub(crate) mod batch_authorize_response;
pub(crate) mod batch_item;
pub(crate) mod batch_item_error;
pub(crate) mod batch_item_result;
mod decision;
mod diagnostics;
pub(crate) mod entity_data;
pub(crate) mod errors;
pub(crate) mod multi_issuer_authorize_result;
mod policy_effect;
mod policy_evaluation_error;
pub(crate) mod policy_metadata;
pub(crate) mod request_batch_multi_issuer;
pub(crate) mod request_batch_unsigned;
pub(crate) mod request_multi_issuer;
pub(crate) mod request_unsigned;
pub(crate) mod token_input;

pub fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<policy_evaluation_error::PolicyEvaluationError>()?;
    m.add_class::<diagnostics::Diagnostics>()?;
    m.add_class::<decision::Decision>()?;
    m.add_class::<entity_data::EntityData>()?;
    m.add_class::<entity_data::CedarEntityMapping>()?;
    m.add_class::<request_unsigned::RequestUnsigned>()?;
    m.add_class::<token_input::TokenInput>()?;
    m.add_class::<request_multi_issuer::AuthorizeMultiIssuerRequest>()?;
    m.add_class::<batch_item::BatchItem>()?;
    m.add_class::<batch_item_error::BatchItemError>()?;
    m.add_class::<batch_item_result::BatchItemUnsignedResult>()?;
    m.add_class::<batch_item_result::BatchItemMultiIssuerResult>()?;
    m.add_class::<request_batch_unsigned::BatchAuthorizeUnsignedRequest>()?;
    m.add_class::<request_batch_multi_issuer::BatchAuthorizeMultiIssuerRequest>()?;
    m.add_class::<batch_authorize_response::BatchAuthorizeUnsignedResponse>()?;
    m.add_class::<batch_authorize_response::BatchAuthorizeMultiIssuerResponse>()?;
    m.add_class::<authorize_result_response::AuthorizeResultResponse>()?;
    m.add_class::<authorize_result::AuthorizeResult>()?;
    m.add_class::<multi_issuer_authorize_result::MultiIssuerAuthorizeResult>()?;
    m.add_class::<policy_effect::PolicyEffect>()?;
    m.add_class::<policy_metadata::PolicyMetadata>()?;

    let submodule = PyModule::new(m.py(), "authorize_errors")?;
    errors::authorize_errors_module(&submodule)?;
    m.add_submodule(&submodule)?;

    Ok(())
}
