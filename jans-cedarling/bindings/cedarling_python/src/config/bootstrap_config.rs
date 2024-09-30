/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::{PyTypeError, PyValueError};
use pyo3::prelude::*;

use crate::config::authz_config::AuthzConfig;
use crate::config::policy_store_config::PolicyStoreConfig;

use super::memory_log_config::MemoryLogConfig;
use super::off_log_config::DisabledLoggingConfig;
use super::stdout_log_config::StdOutLogConfig;

/// BootstrapConfig
/// ===============
///
/// A Python wrapper for the Rust `cedarling::BootstrapConfig` struct.
/// Configures the `Cedarling` application, including authorization, logging, and policy store settings.
///
/// Attributes
/// ----------
/// :param authz_config: An `AuthzConfig` object for authorization settings.
/// :param log_config: A logging configuration (can be `DisabledLoggingConfig`, `MemoryLogConfig`, or `StdOutLogConfig`).
/// :param policy_store_config: A `PolicyStoreConfig` object for the policy store configuration.
///
/// Example
/// -------
/// ```python
/// from cedarling import BootstrapConfig, AuthzConfig, MemoryLogConfig, PolicyStoreConfig
///
/// # Create a BootstrapConfig with memory logging
/// authz = AuthzConfig(application_name="MyApp")
/// log_config = MemoryLogConfig(log_ttl=300)
/// policy_store = PolicyStoreConfig(source=PolicyStoreSource(json='{...}'))
///
/// bootstrap_config = BootstrapConfig(authz_config=authz, log_config=log_config, policy_store_config=policy_store)
/// ```
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
    // - DisabledLoggingConfig
    // - MemoryLogConfig
    // - StdOutLogConfig
    pub log_config: Option<cedarling::LogConfig>,
    /// A set of properties used to load `PolicyStore` in the `Cedarling` application.
    #[pyo3(set)]
    pub policy_store_config: Option<PolicyStoreConfig>,
}

fn extract_log_config(log_config: &PyObject) -> PyResult<cedarling::LogConfig> {
    let log_type = Python::with_gil(|py| -> PyResult<cedarling::LogTypeConfig> {
        if let Ok(log_config) = log_config.extract::<DisabledLoggingConfig>(py) {
            Ok(log_config.into())
        } else if let Ok(log_config) = log_config.extract::<MemoryLogConfig>(py) {
            Ok(log_config.into())
        } else if let Ok(log_config) = log_config.extract::<StdOutLogConfig>(py) {
            Ok(log_config.into())
        } else {
            Err(PyTypeError::new_err("Invalid log_config type. Expected one of: DisabledLoggingConfig, MemoryLogConfig, StdOutLogConfig."))
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
