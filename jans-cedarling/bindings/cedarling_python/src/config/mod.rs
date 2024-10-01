/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use pyo3::prelude::*;
use pyo3::Bound;

mod authz_config;
pub(crate) mod bootstrap_config;
mod memory_log_config;
mod off_log_config;
mod policy_store_config;
mod policy_store_source;
mod stdout_log_config;

pub fn register_entities(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add_class::<authz_config::AuthzConfig>()?;
    m.add_class::<memory_log_config::MemoryLogConfig>()?;
    m.add_class::<off_log_config::DisabledLoggingConfig>()?;
    m.add_class::<stdout_log_config::StdOutLogConfig>()?;
    m.add_class::<policy_store_source::PolicyStoreSource>()?;
    m.add_class::<policy_store_config::PolicyStoreConfig>()?;
    m.add_class::<bootstrap_config::BootstrapConfig>()?;

    Ok(())
}
