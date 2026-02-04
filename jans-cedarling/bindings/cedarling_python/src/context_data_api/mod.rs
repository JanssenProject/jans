/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;
use pyo3::Bound;

pub(crate) mod cedar_type;
pub(crate) mod data_entry;
pub(crate) mod data_store_stats;
pub(crate) mod errors;

pub fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<cedar_type::CedarType>()?;
    m.add_class::<data_entry::DataEntry>()?;
    m.add_class::<data_store_stats::DataStoreStats>()?;

    let submodule = PyModule::new(m.py(), "data_errors")?;
    errors::data_errors_module(&submodule)?;
    m.add_submodule(&submodule)?;

    Ok(())
}
