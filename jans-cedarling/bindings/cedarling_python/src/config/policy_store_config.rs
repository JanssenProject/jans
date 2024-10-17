/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

use crate::config::policy_store_source::PolicyStoreSource;

/// PolicyStoreConfig
/// =================
///
/// A Python wrapper for the Rust `cedarling::PolicyStoreConfig` struct.
/// Configures how and where policies are loaded, specifying the source and optional store ID.
///
/// Attributes
/// ----------
/// :param source: Optional `PolicyStoreSource` for the policy location.
/// :param store_id: Optional store ID; assumes one store if not provided.
///
/// Example
/// -------
/// ```
/// # Create a PolicyStoreConfig with a source and store_id
/// source = PolicyStoreSource(json='{...')
/// config = PolicyStoreConfig(source=source, store_id="store1")
///
/// # Create without store_id
/// config_without_store_id = PolicyStoreConfig(source=source)
///
/// # Access attributes
/// print(config.source)
/// print(config.store_id)
/// ```
#[derive(Debug, Clone)]
#[pyclass(get_all, set_all)]
pub struct PolicyStoreConfig {
    /// Source - represent the place where we going to read the policy.
    /// The value is required.
    pub source: Option<PolicyStoreSource>,
    /// `CEDARLING_POLICY_STORE_ID` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// If None then we should have only one policy store in the `source`.
    pub store_id: Option<String>,
}

#[pymethods]
impl PolicyStoreConfig {
    #[new]
    #[pyo3(signature = (source=None, store_id=None))]
    fn new(source: Option<PolicyStoreSource>, store_id: Option<String>) -> PyResult<Self> {
        Ok(PolicyStoreConfig { source, store_id })
    }
}

impl TryInto<cedarling::PolicyStoreConfig> for PolicyStoreConfig {
    type Error = PyErr;

    fn try_into(self) -> Result<cedarling::PolicyStoreConfig, Self::Error> {
        let source = self
            .source
            .ok_or(PyValueError::new_err(
                "Expected source for PolicyStoreConfig, but got: None",
            ))?
            .into();

        Ok(cedarling::PolicyStoreConfig {
            source,
            store_id: self.store_id,
        })
    }
}
