/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::Bound;
use pyo3::prelude::*;

pub(crate) mod authorize_result;
mod authorize_result_response;
mod decision;
mod diagnostics;
pub(crate) mod errors;
mod policy_evaluation_error;
pub(crate) mod request;
mod resource_data;

pub fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<policy_evaluation_error::PolicyEvaluationError>()?;
    m.add_class::<diagnostics::Diagnostics>()?;
    m.add_class::<decision::Decision>()?;
    m.add_class::<resource_data::ResourceData>()?;
    m.add_class::<request::Request>()?;
    m.add_class::<authorize_result_response::AuthorizeResultResponse>()?;
    m.add_class::<authorize_result::AuthorizeResult>()?;

    let submodule = PyModule::new_bound(m.py(), "authorize_errors")?;
    errors::authorize_errors_module(&submodule)?;
    m.add_submodule(&submodule)?;

    Ok(())
}
