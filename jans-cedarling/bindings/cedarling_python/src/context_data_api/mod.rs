// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
use pyo3::prelude::*;
use pyo3::Bound;

pub(crate) mod cedar_type;
pub(crate) mod data_entry;
pub(crate) mod data_store_stats;
pub(crate) mod errors;

pub(crate) fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<cedar_type::CedarType>()?;
    m.add_class::<data_entry::DataEntry>()?;
    m.add_class::<data_store_stats::DataStoreStats>()?;

    let submodule = PyModule::new(m.py(), "data_errors_ctx")?;
    errors::data_errors_module(&submodule)?;
    m.add_submodule(&submodule)?;
    
    // Insert into sys.modules so it can be imported as cedarling_python.data_errors_ctx
    // Note: add_submodule only attaches the child as an attribute and does not insert it into sys.modules,
    // so the explicit set_item call is required to enable package-qualified imports
    let py = m.py();
    let sys = py.import("sys")?;
    let modules = sys.getattr("modules")?;
    modules.set_item("cedarling_python.data_errors_ctx", submodule.as_borrowed())?;

    Ok(())
}
