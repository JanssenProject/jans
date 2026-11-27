/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
#![cfg(not(target_arch = "wasm32"))]
// looks like the problem in usage PyResult from pyo3 and operator `?`
#![allow(clippy::useless_conversion)]
#![allow(
    clippy::doc_markdown,
    reason = "doc comments become Python docstrings written in reStructuredText"
)]
#![allow(
    clippy::needless_pass_by_value,
    reason = "pyo3 extracts Python arguments as owned values"
)]

use pyo3::prelude::*;
use pyo3::Bound;

mod authorize;
mod cedarling;
mod config;
mod context_data_api;

#[pymodule]
fn cedarling_python(m: &Bound<'_, PyModule>) -> PyResult<()> {
    config::register_entities(m)?;
    authorize::register_entities(m)?;
    context_data_api::register_entities(m)?;

    m.add_class::<cedarling::Cedarling>()?;
    m.add_class::<cedarling::MetricsSnapshot>()?;

    Ok(())
}
