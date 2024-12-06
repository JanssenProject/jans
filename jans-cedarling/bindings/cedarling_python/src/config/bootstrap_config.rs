/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::str::FromStr;

use cedarling::LoggerType;
use pyo3::exceptions::*;
use pyo3::prelude::*;
use pyo3::types::PyDict;

/// BootstrapConfig
/// =========
///
/// A Python wrapper for the Rust `cedarling::BootstrapConfig` struct.
/// Configures the `Cedarling` application, including authorization, logging, and policy store settings.
///
/// Attributes
/// ----------
/// :param log_type: Log type, e.g., 'none', 'memory', 'std_out', or 'lock'.
/// :param log_ttl: Time to live (TTL) of `memory` logs in seconds.
/// :param policy_store_local_fn: A path to the local policy store function
///
/// Methods
/// -------
/// .. method:: __init__(self, config)
///
///     Initializes the Cedarling instance with the provided configuration.
///
///     :param options: A `dict` with startup settings.
///
/// .. method:: load_from_file(str) -> BootstrapConfig
///
///     Loads the bootstrap config from a file.
///
///     :returns: A BootstrapConfig instance
///
///     :raises ValueError: If a provided value is invalid or decoding fails.
///     :raises OSError: If there is an error reading while the file.
///
/// .. method:: load_from_json(str) -> BootstrapConfig
///
///     Loads the bootstrap config from a JSON string.
///
///     :returns: A BootstrapConfig instance
///
///     :raises ValueError: If a provided value is invalid or decoding fails.
#[pyclass]
pub struct BootstrapConfig {
    inner: cedarling::BootstrapConfig,
    source: Option<cedarling::BootstrapConfigRaw>,
}

#[pymethods]
impl BootstrapConfig {
    #[new]
    pub fn new(options: &Bound<'_, PyDict>) -> PyResult<Self> {
        let source: cedarling::BootstrapConfigRaw = serde_pyobject::from_pyobject(options.clone())
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        let inner = cedarling::BootstrapConfig::from_raw_config(&source)
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        Ok(Self {
            inner,
            source: Some(source),
        })
    }

    #[staticmethod]
    pub fn load_from_file(path: &str) -> PyResult<Self> {
        let inner = cedarling::BootstrapConfig::load_from_file(path).map_err(|e| match e {
            cedarling::BootstrapConfigLoadingError::ReadFile(file_name, e) => {
                PyOSError::new_err(format!("Error reading file `{}`: {}", file_name, e))
            },
            err => PyValueError::new_err(err.to_string()),
        })?;
        Ok(Self {
            inner,
            source: None,
        })
    }

    #[staticmethod]
    pub fn load_from_json(config_json: &str) -> PyResult<Self> {
        let inner = cedarling::BootstrapConfig::load_from_json(config_json)
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        Ok(Self {
            inner,
            source: None,
        })
    }

    #[getter]
    pub fn get_policy_store_local_fn(&mut self) -> Option<String> {
        self.source
            .as_ref()
            .map(|s| s.policy_store_local_fn.clone())?
    }

    #[setter]
    pub fn set_policy_store_local_fn(&mut self, path: &str) -> PyResult<()> {
        match self.source.as_mut() {
            Some(source) => {
                source.policy_store_local_fn = Some(path.to_string());
                self.inner = cedarling::BootstrapConfig::from_raw_config(source).map_err(|e| PyValueError::new_err(e.to_string()))?;
                Ok(())
            }
            None => Err(PyValueError::new_err("Failed to set `CEDARLING_POLICY_STORE_LOCAL_FN` since a policy store wasn't loaded yet.")),
        }
    }

    #[getter]
    pub fn get_log_type(&mut self) -> Option<String> {
        self.source.as_ref().map(|s| s.log_type.to_string())
    }

    #[setter]
    pub fn set_log_type(&mut self, log_type: &str) -> PyResult<()> {
        match self.source.as_mut() {
            Some(source) => {
                source.log_type = LoggerType::from_str(log_type)
                    .map_err(|e| PyValueError::new_err(e.to_string()))?;
                self.inner = cedarling::BootstrapConfig::from_raw_config(source)
                    .map_err(|e| PyValueError::new_err(e.to_string()))?;
                Ok(())
            },
            None => Err(PyValueError::new_err(
                "Failed to set `CEDARLING_LOG_TYPE` since a policy store wasn't loaded yet.",
            )),
        }
    }

    #[getter]
    pub fn get_log_ttl(&mut self) -> Option<u64> {
        self.source.as_ref()?.log_ttl
    }

    #[setter]
    pub fn set_log_ttl(&mut self, ttl: u64) -> PyResult<()> {
        match self.source.as_mut() {
            Some(source) => {
                source.log_ttl = Some(ttl);
                Ok(())
            },
            None => Err(PyValueError::new_err(
                "Failed to set `CEDARLING_LOG_TLL` since a policy store wasn't loaded yet.",
            )),
        }
    }
}

impl BootstrapConfig {
    pub fn inner(&self) -> &cedarling::BootstrapConfig {
        &self.inner
    }
}

impl From<BootstrapConfig> for cedarling::BootstrapConfig {
    fn from(value: BootstrapConfig) -> Self {
        value.inner
    }
}
