/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::*;
use pyo3::prelude::*;
use pyo3::types::PyDict;

/// BootstrapConfig
/// =========
///
/// A Python wrapper for the Rust `cedarling::BootstrapConfig` struct.
/// Configures the `Cedarling` application, including authorization, logging, and policy store settings.
///
/// Methods
/// -------
/// .. method:: __init__(self, options)
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
}

#[pymethods]
impl BootstrapConfig {
    #[new]
    pub fn new(options: &Bound<'_, PyDict>) -> PyResult<Self> {
        let source: cedarling::BootstrapConfigRaw = serde_pyobject::from_pyobject(options.clone())
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        let inner = cedarling::BootstrapConfig::from_raw_config(&source)
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        Ok(Self { inner })
    }

    #[staticmethod]
    pub fn load_from_file(path: &str) -> PyResult<Self> {
        let inner = cedarling::BootstrapConfig::load_from_file(path).map_err(|e| match e {
            cedarling::BootstrapConfigLoadingError::ReadFile(file_name, e) => {
                PyOSError::new_err(format!("Error reading file `{}`: {}", file_name, e))
            },
            err => PyValueError::new_err(err.to_string()),
        })?;
        Ok(Self { inner })
    }

    #[staticmethod]
    pub fn load_from_json(config_json: &str) -> PyResult<Self> {
        let inner = cedarling::BootstrapConfig::load_from_json(config_json)
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        Ok(Self { inner })
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
