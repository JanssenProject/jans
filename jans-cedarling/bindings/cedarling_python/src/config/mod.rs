/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;
use pyo3::Bound;

mod authz_config;

pub fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<authz_config::AuthzConfig>()?;

    Ok(())
}
