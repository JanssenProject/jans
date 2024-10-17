/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;
use pyo3::Bound;

mod decision;
mod diagnostics;
mod policy_evaluation_error;
mod request;
mod resource_data;

pub fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<policy_evaluation_error::PolicyEvaluationError>()?;
    m.add_class::<diagnostics::Diagnostics>()?;
    m.add_class::<decision::Decision>()?;
    m.add_class::<resource_data::ResourceData>()?;
    m.add_class::<request::Request>()?;

    Ok(())
}
