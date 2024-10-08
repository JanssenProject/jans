/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;
use pyo3::Bound;

mod authorize;
mod cedarling;
mod config;

#[pymodule]
fn cedarling_python(m: &Bound<'_, PyModule>) -> PyResult<()> {
    config::register_entities(m)?;
    authorize::register_entities(m)?;

    m.add_class::<cedarling::Cedarling>()?;

    Ok(())
}
