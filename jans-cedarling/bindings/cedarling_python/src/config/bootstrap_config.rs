/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

use crate::config::authz_config::AuthzConfig;
use crate::config::policy_store_config::PolicyStoreConfig;

use super::memory_log_config::MemoryLogConfig;
use super::off_log_config::OffLogConfig;
use super::stdout_log_config::StdOutLogConfig;

/// BootstrapConfig
/// ===============
///
/// `BootstrapConfig` is a Python wrapper around the Rust `cedarling::BootstrapConfig` struct.
///  It represents the main configuration for bootstrapping the `Cedarling` application.
///  This configuration includes settings for authorization (`AuthzConfig`), logging (`LogConfig`), and policy store (`PolicyStoreConfig`).
///
/// Class Definition
/// ----------------
///
/// .. class:: BootstrapConfig(authz_config=None, log_config=None, policy_store_config=None)
///
///     The `BootstrapConfig` class is used to configure the initial properties for the `Cedarling` application. This includes setting up authorization, logging, and the policy store.
///
///     :param authz_config: An `AuthzConfig` object representing the authorization configuration.
///     :param log_config: A logging configuration, which can be one of the following:
///                        - `OffLogConfig`: Disable logging.
///                        - `MemoryLogConfig`: Configure memory-based logging.
///                        - `StdOutLogConfig`: Configure logging to standard output.
///     :param policy_store_config: A `PolicyStoreConfig` object representing the policy store configuration.
///
/// Attributes
/// ----------
///
/// .. attribute:: authz_config
///
///     A set of properties used to configure the `Authz` (authorization) in the `Cedarling` application.
///
///     :type: AuthzConfig
///
/// .. attribute:: log_config
///
///     A set of properties used to configure logging in the `Cedarling` application. The log configuration must be one of the following:
///     - `OffLogConfig`: Disables logging.
///     - `MemoryLogConfig`: Configures memory-based logging.
///     - `StdOutLogConfig`: Logs to standard output.
///
///     :type: LogConfig
///
/// .. attribute:: policy_store_config
///
///     A set of properties used to load and configure the policy store in the `Cedarling` application.
///
///     :type: PolicyStoreConfig
///
/// Methods
/// -------
///
/// .. method:: __init__(self, authz_config=None, log_config=None, policy_store_config=None)
///
///     Initializes a new instance of the `BootstrapConfig` class.
///
///     :param authz_config: Optional. An `AuthzConfig` object representing the authorization configuration.
///     :param log_config: Optional. A logging configuration (`OffLogConfig`, `MemoryLogConfig`, `StdOutLogConfig`).
///     :param policy_store_config: Optional. A `PolicyStoreConfig` object for configuring the policy store.
///
/// .. method:: set_log_config(self, value)
///
///     Sets the log configuration. The value must be one of the following types: `OffLogConfig`, `MemoryLogConfig`, or `StdOutLogConfig`.
///
///     :param value: The log configuration object.
///     :raises ValueError: If the provided log configuration is not a valid type.
///
/// Example
/// -------
///
/// ```python
///
///     from cedarling import BootstrapConfig, AuthzConfig, MemoryLogConfig, PolicyStoreConfig
///
///     # Creating a new BootstrapConfig with memory log configuration
///     authz = AuthzConfig(application_name="MyApp")
///     log_config = MemoryLogConfig(log_ttl=300)
///     policy_store = PolicyStoreConfig(source=PolicyStoreSource(json='{...}'))
///
///     bootstrap_config = BootstrapConfig(authz_config=authz, log_config=log_config, policy_store_config=policy_store)
///
///     # Setting log config to OffLogConfig
///     bootstrap_config.log_config = OffLogConfig()
///
///     # Attempting to set an invalid log configuration will raise a ValueError
///     try:
///         bootstrap_config.log_config = "InvalidConfig"
///     except ValueError as e:
///         print(f"Error: {e}")
/// ```
///
#[derive(Debug, Clone)]
#[pyclass]
pub struct BootstrapConfig {
    /// A set of properties used to configure `Authz` in the `Cedarling` application.
    #[pyo3(set)]
    pub authz_config: Option<AuthzConfig>,
    /// A set of properties used to configure logging in the `Cedarling` application.
    //
    // we implement setter for this field manually
    // because we can use for this option next classes:
    // - OffLogConfig
    // - MemoryLogConfig
    // - StdOutLogConfig
    pub log_config: Option<cedarling::LogConfig>,
    /// A set of properties used to load `PolicyStore` in the `Cedarling` application.
    #[pyo3(set)]
    pub policy_store_config: Option<PolicyStoreConfig>,
}

fn extract_log_config(log_config: &PyObject) -> PyResult<cedarling::LogConfig> {
    let log_type = Python::with_gil(|py| -> PyResult<cedarling::LogTypeConfig> {
        if let Ok(log_config) = log_config.extract::<OffLogConfig>(py) {
            Ok(log_config.into())
        } else if let Ok(log_config) = log_config.extract::<MemoryLogConfig>(py) {
            Ok(log_config.into())
        } else if let Ok(log_config) = log_config.extract::<StdOutLogConfig>(py) {
            Ok(log_config.into())
        } else {
            Err(PyValueError::new_err("Invalid log_config type. Expected one of: OffLogConfig, MemoryLogConfig, StdOutLogConfig."))
        }
    })?;

    // if `cedarling::LogConfig` will be expanded, we will update python API
    Ok(cedarling::LogConfig { log_type })
}

#[pymethods]
impl BootstrapConfig {
    #[new]
    #[pyo3(signature = (authz_config=None, log_config=None, policy_store_config=None))]
    fn new(
        authz_config: Option<AuthzConfig>,
        log_config: Option<PyObject>, // Use Py<PyAny> instead of &PyAny
        policy_store_config: Option<PolicyStoreConfig>,
    ) -> PyResult<Self> {
        let log_config = match log_config {
            Some(python_value) => Some(extract_log_config(&python_value)?),
            None => None,
        };

        Ok(Self {
            authz_config,
            policy_store_config,
            log_config,
        })
    }

    #[setter]
    fn log_config(&mut self, value: PyObject) -> PyResult<()> {
        self.log_config = Some(extract_log_config(&value)?);
        Ok(())
    }
}

impl TryFrom<BootstrapConfig> for cedarling::BootstrapConfig {
    type Error = PyErr;

    fn try_from(value: BootstrapConfig) -> Result<Self, Self::Error> {
        Ok(Self {
            authz_config: value
                .authz_config
                .ok_or(PyValueError::new_err("value authz_config is None"))?
                .try_into()?,
            log_config: value
                .log_config
                .ok_or(PyValueError::new_err("value log_config is None"))?,
            policy_store_config: value
                .policy_store_config
                .ok_or(PyValueError::new_err("value policy_store_config is None"))?
                .try_into()?,
        })
    }
}
