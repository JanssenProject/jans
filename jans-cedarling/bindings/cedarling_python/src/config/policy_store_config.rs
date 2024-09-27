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
/// `PolicyStoreConfig` is a Python wrapper around the Rust `cedarling::PolicyStoreConfig` struct.
///  It represents the configuration for the policy store, including the source from which policies are read and the optional policy store ID.
///
/// Class Definition
/// ----------------
///
/// .. class:: PolicyStoreConfig(source=None, store_id=None)
///
///     The `PolicyStoreConfig` class is used to configure how and where policies are loaded. The `source` specifies the location (e.g., JSON) of the policy, and `store_id` represents the ID of the policy store, which is optional.
///
///     :param source: Optional. A `PolicyStoreSource` object representing the policy source.
///     :param store_id: Optional. A string representing the policy store ID. If not specified, only one policy store is assumed in the `source`.
///
/// Attributes
/// ----------
///
/// .. attribute:: source
///
///     The source from which the policy is read. This attribute is required for policy configuration.
///
///     :type: PolicyStoreSource or None
///
/// .. attribute:: store_id
///
///     The ID of the policy store. If this is not provided, the assumption is that there is only one policy store in the `source`.
///
///     :type: str or None
///
/// Methods
/// -------
///
/// .. method:: __init__(self, source=None, store_id=None)
///
///     Initializes a new instance of the `PolicyStoreConfig` class. Both `source` and `store_id` are optional, but the `source` must be provided for the configuration to be valid.
///
///     :param source: Optional. A `PolicyStoreSource` object.
///     :param store_id: Optional. A string representing the ID of the policy store.
///
/// Example
/// -------
///
/// ```python
///
///     # Creating a new PolicyStoreConfig instance with a source and store_id
///     source = PolicyStoreSource(json='{"policy": {"id": "policy1", "rules": []}}')
///     config = PolicyStoreConfig(source=source, store_id="store1")
///
///     # Creating a PolicyStoreConfig instance without a store_id
///     config_without_store_id = PolicyStoreConfig(source=source)
///
///     # Accessing attributes
///     print(config.source)
///     print(config.store_id)
///     
///     # Attempting to create PolicyStoreConfig without a source will raise an error during conversion
///     try:
///         invalid_config = PolicyStoreConfig(store_id="store1")
///         # This will raise an error when converted to cedarling::PolicyStoreConfig
///     except ValueError as e:
///         print(f"Error: {e}")
/// ```
///
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
