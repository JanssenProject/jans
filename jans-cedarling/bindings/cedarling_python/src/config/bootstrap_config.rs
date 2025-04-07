/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::*;
use pyo3::prelude::*;
use pyo3::types::PyDict;

/// A Python wrapper for the Rust `cedarling::BootstrapConfig` struct.
/// Configures the `Cedarling` application, including authorization, logging, and policy store settings.
#[pyclass(module = "cedarling_python._cedarling_python")]
pub struct BootstrapConfig {
    inner: cedarling::BootstrapConfig,
}

#[pymethods]
impl BootstrapConfig {
    #[new]
    pub fn new(options: Bound<'_, PyDict>) -> PyResult<Self> {
        let source: cedarling::BootstrapConfigRaw = serde_pyobject::from_pyobject(options)
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        let inner = cedarling::BootstrapConfig::from_raw_config(&source)
            .map_err(|e| PyValueError::new_err(e.to_string()))?;
        Ok(Self { inner })
    }

    #[staticmethod]
    #[pyo3(signature = (config=None))]
    fn from_env(config: Option<Bound<'_, PyDict>>) -> PyResult<Self> {
        let inner = if let Some(c) = config {
            let source: cedarling::BootstrapConfigRaw = serde_pyobject::from_pyobject(c)
                .map_err(|e| PyValueError::new_err(e.to_string()))?;

            cedarling::BootstrapConfig::from_raw_config_and_env(Some(source))
                .map_err(|e| PyValueError::new_err(e.to_string()))?
        } else {
            cedarling::BootstrapConfig::from_raw_config_and_env(None)
                .map_err(|e| PyValueError::new_err(e.to_string()))?
        };

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
